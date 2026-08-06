package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.session.SesionManager
import java.util.concurrent.Executors

class RegistroActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var etNombre: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etConfirmar: EditText
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        etNombre = findViewById(R.id.et_nombre)
        etCorreo = findViewById(R.id.et_correo)
        etContrasena = findViewById(R.id.et_contrasena)
        etConfirmar = findViewById(R.id.et_confirmar)
        tvError = findViewById(R.id.tv_error)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_registrar)
            .setOnClickListener { registrar() }
    }

    private fun registrar() {
        val nombre = etNombre.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val contrasena = etContrasena.text.toString()
        val confirmar = etConfirmar.text.toString()

        when {
            nombre.isEmpty() || correo.isEmpty() || contrasena.isEmpty() || confirmar.isEmpty() ->
                mostrarError("Completa todos los campos")

            contrasena.length < 6 ->
                mostrarError("La contraseña debe tener al menos 6 caracteres")

            contrasena != confirmar ->
                mostrarError("Las contraseñas no coinciden")

            else -> {
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_registrar).isEnabled = false
                executor.execute {
                    val id = DatabaseHelper(this).registrar(nombre, correo, contrasena)
                    runOnUiThread {
                        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_registrar).isEnabled =
                            true
                        if (id != null) {
                            SesionManager.guardarSesion(this, id)
                            Toast.makeText(this, "¡Cuenta creada!", Toast.LENGTH_SHORT).show()
                            irAlMapa()
                        } else {
                            mostrarError("Ese correo ya está registrado")
                        }
                    }
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
