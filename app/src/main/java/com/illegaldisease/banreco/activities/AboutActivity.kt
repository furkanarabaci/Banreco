package com.illegaldisease.banreco.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.illegaldisease.banreco.R
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createAboutPage())
    }
    private fun createAboutPage() : View {
        val versionElement = Element()
        versionElement.title = "Version 1.0.0" //TODO: Take this from build.gradle somehow.
        return AboutPage(this)
                .isRTL(false)
                .setDescription("Text recognition supported by Google Vision API")
                .setImage(R.mipmap.logo_foreground)
                .addItem(versionElement)
//                .addGroup("Connect with us")
//                .addEmail("elmehdi.sakout@gmail.com")
//                .addWebsite("http://medyo.github.io/")
//                .addFacebook("the.medy")
//                .addTwitter("medyo80")
//                .addYoutube("UCdPQtdWIsg7_pi4mrRu46vA")
//                .addPlayStore("com.ideashower.readitlater.pro")
//                .addGitHub("medyo")
//                .addInstagram("medyo80")
                .create()
    }
}
