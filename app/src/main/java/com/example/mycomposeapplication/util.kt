package com.example.mycomposeapplication

import android.content.Context
import android.graphics.*
import android.media.Image
import android.media.Image.Plane
import android.media.ImageReader
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.FloatBuffer

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

/**
 * https://developer.android.google.cn/topic/performance/graphics/load-bitmap?hl=zh-cn
 * */
fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

/**
 * https://developer.android.google.cn/topic/performance/graphics/load-bitmap?hl=zh-cn
 * */
fun decodeSampledBitmapFromFile(
    pathName: String,
    reqWidth: Int,
    reqHeight: Int
): Bitmap {
    // First decode with inJustDecodeBounds=true to check dimensions
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(pathName, this)

        // Calculate inSampleSize
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        inJustDecodeBounds = false

        BitmapFactory.decodeFile(pathName, this)
    }

}

fun saveBitMap(context: Context, bitmap: Bitmap, name: String) {
    try {
        val file = File(context.filesDir.path + "/$name.png")
        if (!file.exists()) {
            file.createNewFile()
        }
        val out = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)

        // 刷新输出流并关闭
        out.flush()
        out.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadThumbnail(context: Context, imagePath: String): Bitmap {
    return Glide.with(context)
        .asBitmap()
        .load(imagePath)
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .downsample(DownsampleStrategy.FIT_CENTER)
        .apply(RequestOptions().override(224, 224))
        .skipMemoryCache(true)
        .submit().get()
}