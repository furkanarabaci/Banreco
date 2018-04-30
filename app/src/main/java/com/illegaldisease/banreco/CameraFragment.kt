package com.illegaldisease.banreco

import android.Manifest
import android.app.Fragment
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.Toast
import java.io.*
import java.util.*

class CameraFragment : Fragment() {
    private var takePictureButton: FloatingActionButton? = null
    private var textureView: TextureView? = null
    private var cameraId: String? = null
    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraCaptureSessions: CameraCaptureSession
    //    private var captureRequest: CaptureRequest? = null
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private var imageDimension: Size? = null
    private var imageReader: ImageReader? = null
    //    private val mFlashSupported: Boolean = false
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null
    private var textureListener: TextureView.SurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            //open your camera here
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // Transform you image captured size according to the surface width and height
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            //This is called when the camera is open
            Log.e(CameraFragment.TAG, "onOpened")
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice!!.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice!!.close()
            cameraDevice = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreate(savedInstanceState)
        return inflater!!.inflate(R.layout.fragment_camera ,container,false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textureView = view!!.findViewById<View>(R.id.texture) as TextureView
        textureView!!.surfaceTextureListener = textureListener
        takePictureButton = view.findViewById<View>(R.id.fab_take_photo) as FloatingActionButton
        takePictureButton!!.setOnClickListener { takePicture() }
    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    private fun takePicture() {
        if (null == cameraDevice) {
            Log.e(CameraFragment.TAG, "cameraDevice is null")
            return
        }
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(cameraDevice!!.id)
            val jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(ImageFormat.JPEG)
            var width = 640
            var height = 480 //Default sizes.
            if (jpegSizes != null && jpegSizes.isNotEmpty()) {
                width = jpegSizes[0].width
                height = jpegSizes[0].height
            }
            val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
            val outputSurfaces = ArrayList<Surface>(2)
            outputSurfaces.add(reader.surface)
            outputSurfaces.add(Surface(textureView!!.surfaceTexture))
            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            // Orientation
            val rotation = activity.windowManager.defaultDisplay.rotation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, CameraFragment.ORIENTATIONS.get(rotation))
            saveFileToStorage(reader)
            val captureListener = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    super.onCaptureCompleted(session, request, result)

                    createCameraPreview()
                }
            }
            cameraDevice!!.createCaptureSession(outputSurfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }

                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }
    private fun saveFileToStorage(reader: ImageReader){
        (activity as CameraActivity).pickDate()
        val file = File(activity.filesDir.toString() + "/" + System.currentTimeMillis().toString()) //Now we guaranteed it to be unique.
        val readerListener = object : ImageReader.OnImageAvailableListener {
            override fun onImageAvailable(reader: ImageReader) {
                var image: Image? = null
                try {
                    image = reader.acquireLatestImage()
                    val buffer = image!!.planes[0].buffer
                    val bytes = ByteArray(buffer.capacity())
                    buffer.get(bytes)
                    save(bytes)
                    Toast.makeText(activity, "Saved to :$file", Toast.LENGTH_LONG).show()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                    Toast.makeText(activity, "Some error has occurred.", Toast.LENGTH_LONG).show()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(activity, "Could not save to :$file", Toast.LENGTH_LONG).show()
                } finally {
                    if (image != null) {
                    }
                }
            }

            @Throws(IOException::class)
            private fun save(bytes: ByteArray) {
                var output: OutputStream? = null
                try {
                    output = FileOutputStream(file)
                    output.write(bytes)
                } finally {
                    if (null != output) {
                        output.close()
                    }
                }
            }
        }
        reader.setOnImageAvailableListener(readerListener, mBackgroundHandler)
    }
    private fun createCameraPreview() {
        try {
            val texture = textureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            val surface = Surface(texture)
            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)
            cameraDevice!!.createCaptureSession(Arrays.asList(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession
                    updatePreview()
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    Toast.makeText(activity, "Configuration change", Toast.LENGTH_SHORT).show()
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun openCamera() {
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        Log.e(CameraFragment.TAG, "is camera open")
        try {
            cameraId = manager.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId!!)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), CameraFragment.REQUEST_CAMERA_PERMISSION)
                return
            }
            manager.openCamera(cameraId!!, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        Log.e(CameraFragment.TAG, "openCamera X")
    }

    private fun updatePreview() {
        if (null == cameraDevice) {
            Log.e(CameraFragment.TAG, "updatePreview error, return")
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == CameraFragment.REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(activity, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show()
                activity.finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e(CameraFragment.TAG, "onResume")
        startBackgroundThread()
        if (textureView!!.isAvailable) {
            openCamera()
        } else {
            textureView!!.surfaceTextureListener = textureListener
        }
    }

    override fun onPause() {
        Log.e(CameraFragment.TAG, "onPause")
        //closeCamera();
        stopBackgroundThread()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        closeCamera()
        closeBackGroundThread()
    }

    private fun closeCamera() {
        if (null != cameraDevice) {
            cameraDevice!!.close()
            cameraDevice = null
        }
        if (null != imageReader) {
            imageReader!!.close()
            imageReader = null
        }
    }
    private fun closeBackGroundThread(){
        if(mBackgroundThread != null){
            mBackgroundThread!!.quitSafely() //I think we are double-checking null thingy, so very well.
            mBackgroundThread = null
            mBackgroundHandler = null
        }
    }
    companion object {
        private const val TAG = "CameraFragment"
        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        private const val REQUEST_CAMERA_PERMISSION = 200
    }
}