package com.YusrilChalif.ta_mask_detection

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import com.YusrilChalif.ta_mask_detection.common.AugmentedFaceFragment
import com.YusrilChalif.ta_mask_detection.common.AugmentedFaceListener
import com.YusrilChalif.ta_mask_detection.common.AugmentedFaceNode
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), AugmentedFaceListener {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (face_view as AugmentedFaceFragment).setAugmentedFaceListener(this)

        val button: ImageButton = findViewById(R.id.btnBack)
        button.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main){
                delay(500L)
                startActivity(Intent(this@MainActivity, MainMenu::class.java))
                mediaPlayer!!.stop()
                finish()
            }
        }
    }

    companion object{
        enum class FaceLandmark{
            FOREHEAD_LEFT,
            FOREHEAD_RIGHT,
            NOSE_TIP
        }
    }

    override fun onFaceAdded(face: AugmentedFaceNode) {
        face.setRegionModel(
            AugmentedFaceNode.Companion.FaceLandmark.NOSE_TIP,
            "models/covid.obj",
            "models/CovidTexture.png")
        face.setRegionModel(
            AugmentedFaceNode.Companion.FaceLandmark.FOREHEAD_LEFT,
            "models/covid.obj",
            "models/CovidTexture.png")
        face.setRegionModel(
            AugmentedFaceNode.Companion.FaceLandmark.FOREHEAD_RIGHT,
            "models/covid.obj",
            "models/CovidTexture.png")

        mediaPlayer = MediaPlayer.create(this, R.raw.audio)
        mediaPlayer!!.isLooping = false
        mediaPlayer!!.start()
    }

    override fun onFaceUpdate(face: AugmentedFaceNode) {}

}