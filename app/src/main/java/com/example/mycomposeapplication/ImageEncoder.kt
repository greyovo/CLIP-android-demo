package com.example.mycomposeapplication

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import org.pytorch.*
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream


class ImageEncoder(private val context: MainActivity) {
    private val modelPath = "clip-image-encoder.ptl"

    private var module: Module? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        val time = System.currentTimeMillis()
        module = LiteModuleLoader.load(assetFilePath(context, modelPath))
        val dur = (System.currentTimeMillis() - time) * 1000
        Log.i("ImageEncoder", "load cost: $dur s")
    }

    private fun resize(bitmap: Bitmap): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, 224, 224, false)
    }

    private fun centerCrop(bitmap: Bitmap): Bitmap {
        // 计算裁切位置
        val x: Int = (bitmap.width - 224) / 2
        val y: Int = (bitmap.height - 224) / 2
        return Bitmap.createBitmap(bitmap, x, y, 224, 224)
    }


    private fun toRGB(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height) // 获取每个像素的颜色值

        for (i in pixels.indices) { // 将每个像素的颜色值转换为RGB格式
            val color = pixels[i]
            val red: Int = Color.red(color)
            val green: Int = Color.green(color)
            val blue: Int = Color.blue(color)
            val rgb = red shl 16 or (green shl 8) or blue
            pixels[i] = rgb
        }
        return bitmap
    }

    private fun toTensor(bitmap: Bitmap): Tensor {
        val mInputTensorBuffer = Tensor.allocateFloatBuffer(3 * 224 * 224)
        TensorImageUtils.bitmapToFloatBuffer(
            bitmap,
            0, 0,
            224, 224,
            floatArrayOf(0.48145467f, 0.4578275f, 0.40821072f),
            floatArrayOf(0.26862955f, 0.2613026f, 0.2757771f),
            mInputTensorBuffer,
            0,
            MemoryFormat.CHANNELS_LAST,
        )
        return Tensor.fromBlob(
            mInputTensorBuffer, longArrayOf(1, 3, 224, 224),
            MemoryFormat.CHANNELS_LAST,
        )
    }

    fun encode(input: Bitmap): Tensor? {
        if (module == null) {
            loadModel()
        }
        var start = System.currentTimeMillis()
        val resized = resize(input)
        Log.d("resize cost:", "${System.currentTimeMillis() - start} ms")
//        saveBitMap(resized, "resized")
        start = System.currentTimeMillis()
        val cropped = centerCrop(resized)
        saveBitMap(cropped, "cropped")
        Log.d("crop cost:", "${System.currentTimeMillis() - start} ms")
//        saveBitMap(cropped, "cropped")
        start = System.currentTimeMillis()
        val tensor = toTensor(toRGB(cropped))
        Log.d("toTensor cost:", "${System.currentTimeMillis() - start} ms")

//        Log.i(
//            "ImageEncoder.tensor",
//            tensor.dataAsFloatArray[tensor.dataAsFloatArray.size - 1].toString()
//        )
        start = System.currentTimeMillis()
        val res = module?.forward(IValue.from(tensor))?.toTensor()
        Log.d("forward cost:", "${System.currentTimeMillis() - start} ms")
        return res
    }

    private fun saveBitMap(bitmap: Bitmap, name: String) {
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
}