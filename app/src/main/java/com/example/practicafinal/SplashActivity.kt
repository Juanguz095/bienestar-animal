package com.example.practicafinal

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.img_logo)
        val progress = findViewById<ProgressBar>(R.id.progress)

        logo.scaleX = 0.3f
        logo.scaleY = 0.3f
        logo.alpha = 0f
        progress.max = 100

        // Animación del logo
        logo.animate()
            .scaleX(1f).scaleY(1f)
            .alpha(1f)
            .setDuration(1500)
            .start()

        // Barra de carga que se llena en paralelo
        val anim = ObjectAnimator.ofInt(progress, "progress", 100)
        anim.duration = 1500
        anim.start()

        // Al terminar la animación, abrir la app
        logo.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1600)
    }
}
