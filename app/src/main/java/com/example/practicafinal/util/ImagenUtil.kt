package com.example.practicafinal.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/** Decodifica una imagen desde una URI (galería) reduciendo su tamaño. */
fun decodificarImagen(context: Context, uriStr: String, sample: Int = 4): Bitmap? =
    try {
        val uri = Uri.parse(uriStr)
        context.contentResolver.openInputStream(uri)?.use { input ->
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeStream(input, null, opts)
        }
    } catch (_: Exception) {
        null
    }
