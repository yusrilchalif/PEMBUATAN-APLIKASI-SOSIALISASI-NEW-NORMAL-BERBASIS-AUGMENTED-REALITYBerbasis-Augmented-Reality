package com.YusrilChalif.ta_mask_detection

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main_menu.*
import kotlin.system.exitProcess

class MainMenu : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        btn_to_detect.setOnClickListener(this)
        btn_credit.setOnClickListener(this)
        btn_quit.setOnClickListener(this)

    }

    override fun onClick(view: View?) {
        when(view!!.id)
        {
            R.id.btn_to_detect ->
            {
                startActivity(Intent(this@MainMenu, FaceDetection::class.java))
                finish()
            }
            R.id.btn_credit ->
            {
                startActivity(Intent(this@MainMenu, Credit::class.java))
                finish()
            }
            R.id.btn_quit ->
            {
                moveTaskToBack(true)
                exitProcess(-1)
            }
        }
    }
}