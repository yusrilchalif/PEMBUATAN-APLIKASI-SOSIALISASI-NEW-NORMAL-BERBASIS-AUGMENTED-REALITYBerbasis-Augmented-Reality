package com.example.ta_mask_detection

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_splash_screen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SplashScreen : AppCompatActivity() {

    private val btnSkip: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val btnSkip: Button = findViewById(R.id.btn_skip);

        viewPager.adapter = ViewPageAdapter(supportFragmentManager)

        btnSkip.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                delay(2000L)
                startActivity(Intent(this@SplashScreen, MainMenu::class.java))
                finish()
            }
        }

    }
}