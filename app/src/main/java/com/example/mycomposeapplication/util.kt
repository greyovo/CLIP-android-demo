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

import android.graphics.*
import android.util.Log
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.Arrays
import kotlin.math.roundToInt

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

/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


const val DIM_BATCH_SIZE = 1;
const val DIM_PIXEL_SIZE = 3;
const val IMAGE_SIZE_X = 224;
const val IMAGE_SIZE_Y = 224;

fun bitmapToFloatBuffer(bitmap: Bitmap): FloatBuffer {
    val imgData = FloatBuffer.allocate(
        DIM_BATCH_SIZE
                * DIM_PIXEL_SIZE
                * IMAGE_SIZE_X
                * IMAGE_SIZE_Y
    )
    imgData.rewind()
    val stride = IMAGE_SIZE_X * IMAGE_SIZE_Y
    val bmpData = IntArray(stride)
    bitmap.getPixels(bmpData, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    for (i in 0 until IMAGE_SIZE_X) {
        for (j in 0 until IMAGE_SIZE_Y) {
            val idx = IMAGE_SIZE_Y * i + j
            val pixelValue = bmpData[idx]
            imgData.put(idx, (((pixelValue shr 16 and 0xFF) / 255f - 0.485f) / 0.229f))
            imgData.put(idx + stride, (((pixelValue shr 8 and 0xFF) / 255f - 0.456f) / 0.224f))
            imgData.put(idx + stride * 2, (((pixelValue and 0xFF) / 255f - 0.406f) / 0.225f))
        }
    }

    imgData.rewind()
    return imgData
}

fun floatBufferToFloat16Buffer(floatBuffer: FloatBuffer): ShortBuffer {
//    val byteBuffer = ByteBuffer.allocate(floatArray.size * 2).order(ByteOrder.nativeOrder())
//    val floatBuffer = byteBuffer.asShortBuffer()
//    for (f in floatArray) {
//        floatBuffer.put(floatToHalf(f))
//    }
//    floatBuffer.rewind()
//    return floatBuffer
    val shortBuffer: ShortBuffer = ShortBuffer.allocate(floatBuffer.limit())
    while (floatBuffer.hasRemaining()) {
        val floatValue = floatBuffer.get()
        val shortValue = floatToHalf(floatValue)
        shortBuffer.put(shortValue)
    }
    shortBuffer.rewind()
    return shortBuffer
}

private fun floatToHalf(f: Float): Short {
    val bits = java.lang.Float.floatToIntBits(f)
    val s = (bits shr 16) and 0x8000 // sign
    var e = (bits shr 23) and 0xff // exponent
    var m = bits and 0x7fffff // mantissa
    if (e == 0) {
        if (m == 0) {
            return (s or 0x0000).toShort() // +/-0
        } else {
            while (m and 0x800000 == 0) {
                m = m shl 1
                e -= 1
            }
            e += 1
            m = m and 0x7fffff
        }
    } else if (e == 0xff) {
        if (m == 0) {
            return (s or 0x7c00).toShort() // +/-inf
        } else {
            return (s or 0x7c00 or ((m shr 13) and 0x03ff)).toShort() // NaN
        }
    } else {
        e += (15 - 127)
        m = m shr 13
    }
    return (s or (e shl 10) or m).toShort()
}