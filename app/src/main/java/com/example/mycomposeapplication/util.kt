package com.example.mycomposeapplication

import android.R.attr
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.media.Image.Plane
import android.media.ImageReader
import androidx.compose.ui.graphics.ImageBitmap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer


@Throws(IOException::class)
fun assetFilePath(context: Context, assetName: String): String? {
    val file = File(context.filesDir, assetName)
    if (file.exists() && file.length() > 0) {
        return file.absolutePath
    }
    context.assets.open(assetName).use { `is` ->
        FileOutputStream(file).use { os ->
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while (`is`.read(buffer).also { read = it } != -1) {
                os.write(buffer, 0, read)
            }
            os.flush()
        }
        return file.absolutePath
    }
}


fun loadJsonFromAssets(context: Context, fileName: String): Map<Char, Int> {
    val jsonString = try {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.readText()
        } else {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        }
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        return emptyMap()
    }
    val mapType = object : TypeToken<Map<Char, Int>>() {}.type
    return Gson().fromJson(jsonString, mapType)
}


@Throws(IOException::class)
fun convertBitmapAsImage(bitmap: Bitmap): Image {
    // 将 Bitmap 转换为 Image
    val width = bitmap.width
    val height = bitmap.height
    val image = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1).acquireLatestImage()
    val planes: Array<Plane> = image.planes
    val buffer: ByteBuffer = planes[0].buffer
    val bitmapBuffer = ByteBuffer.allocate(bitmap.byteCount)
    bitmap.copyPixelsToBuffer(bitmapBuffer)
    buffer.put(bitmapBuffer.array())
    image.close()

    return image
}