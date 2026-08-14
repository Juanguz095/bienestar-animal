package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.practicafinal.controlador.ControladorUsuarios
import com.example.practicafinal.session.SesionManager
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya hay sesión, ir directo al mapa
        if (SesionManager.tieneSesion(this)) {
            irAlMapa()
            return
        }

        setContentView(R.layout.activity_login)

        etCorreo = findViewById(R.id.et_correo)
        etContrasena = findViewById(R.id.et_contrasena)
        tvError = findViewById(R.id.tv_error)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_login)
            .setOnClickListener { iniciarSesion() }

        findViewById<TextView>(R.id.tv_ir_registro)
            .setOnClickListener {
                startActivity(Intent(this, RegistroActivity::class.java))
            }
    }

    private fun iniciarSesion() {
        val correo = etCorreo.text.toString().trim()
        val contrasena = etContrasena.text.toString()

        if (correo.isEmpty() || contrasena.isEmpty()) {
            mostrarError("Completa todos los campos")
            return
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_login).isEnabled = false
        executor.execute {
            val usuario = ControladorUsuarios.iniciarSesion(this, correo, contrasena)
            runOnUiThread {
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_login).isEnabled = true
                if (usuario != null) {
                    SesionManager.guardarSesion(this, usuario.id)
                    Toast.makeText(this, "¡Hola, ${usuario.nombre}!", Toast.LENGTH_SHORT).show()
                    irAlMapa()
                } else {
                    mostrarError("Correo o contraseña incorrectos")
                }
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        tvError.visibility = View.VISIBLE
    }

    private fun irAlMapa() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
