package com.example.mycomposeapplication

import android.util.Log
import java.io.File
import java.io.FileOutputStream

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

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