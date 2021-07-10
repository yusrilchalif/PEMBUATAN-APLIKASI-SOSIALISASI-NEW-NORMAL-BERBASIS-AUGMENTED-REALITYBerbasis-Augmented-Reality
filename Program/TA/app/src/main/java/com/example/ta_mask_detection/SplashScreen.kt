package com.example.ta_mask_detection

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import java.util.*

class SplashScreen : AppCompatActivity() {
    private var timer: Timer? = null
    private var progressBar: ProgressBar? = null
    private var i = 0
    var textView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        getActionBar()?.hide();

        progressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        progressBar!!.progress = 0
        textView = findViewById<View?>(R.id.textView) as TextView
        textView!!.text = ""
        val period: Long = 100
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                if(i < 100)
                {
                    runOnUiThread { textView!!.text = "Loading $i%" }
                    progressBar!!.progress = i
                    i++
                }
                else
                {
                    timer!!.cancel()
                    val intent = Intent(this@SplashScreen, FaceDetection::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }, 0, period)
    }
}