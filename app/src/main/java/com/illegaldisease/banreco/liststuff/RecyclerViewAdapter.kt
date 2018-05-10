package com.illegaldisease.banreco.liststuff

import android.content.Context
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.RecyclerView
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.illegaldisease.banreco.R
import com.illegaldisease.banreco.databaserelated.EventHandler
import com.illegaldisease.banreco.databaserelated.EventsRemastered

import com.illegaldisease.banreco.liststuff.ItemFragment.OnListFragmentInteractionListener
import java.util.*
import android.content.Intent
import android.util.Log
import com.illegaldisease.banreco.activities.ImageActivity


class RecyclerViewAdapter(private val context : Context, private val mValues: MutableList<EventsRemastered>, private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {
    private var mOnClickListener: View.OnClickListener ?= null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_list, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        with(holder.mView) {
            holder.eventImage.setImageBitmap(item.photo)
            holder.eventDate.text = convertTimestampToString(item.date)
            setOnClickListener(mOnClickListener)
        }
        holder.fab.setOnClickListener {
            try {
                EventHandler.deleteEvent(context, item) //Deletes from database and original static list.
            }
            catch (e : Exception){
                mValues.remove(item) //Deletes from current adapter.
            }
            notifyDataSetChanged()
            notifyItemInserted(position)
            notifyItemRangeInserted(position,itemCount)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount)
        }
        holder.eventImage.setOnClickListener {
            try{
                val intent = Intent(context, ImageActivity:: class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("Bitmap",item.date)
                    putExtra("willshow",false)//We will not show buttons when just previewing them.
                }
                context.startActivity(intent)
            }
            catch (er : NullPointerException){
                Log.e("onclicklistener",Log.getStackTraceString(er))
            }
        }
    }
    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        var eventDate : TextView = mView.findViewById(R.id.listDate)
        var eventImage : ImageView = mView.findViewById(R.id.listImage)
        var fab : FloatingActionButton = mView.findViewById(R.id.fragmentfloatingActionButton)
        init {
            mOnClickListener = View.OnClickListener { v ->
                val item = v.tag as? EventsRemastered
                // Notify the active callbacks interface (the activity, if the fragment is attached to
                // one) that an item has been selected.
                mListener?.onListFragmentInteraction(item)
            }
        }
    }
    private fun convertTimestampToString(timestamp : Int) : String {
        val calendar = Calendar.getInstance(Locale.ENGLISH)
        calendar.timeInMillis = timestamp.toLong() * 1000
        return DateFormat.format("dd-MM-yyyy HH:mm",calendar).toString()
    }
}
