package com.example.ta_mask_detection

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton

class Credit : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credit)

        val buttonToMain: ImageButton = findViewById(R.id.btnBackmain)
        buttonToMain.setOnClickListener {
            startActivity(Intent(this@Credit, MainMenu::class.java))
        }
    }
}