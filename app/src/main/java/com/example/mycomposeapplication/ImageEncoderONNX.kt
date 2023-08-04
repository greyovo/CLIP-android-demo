package com.example.mycomposeapplication

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.providers.NNAPIFlags
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import org.pytorch.*
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.util.*


class ImageEncoderONNX(private val context: MainActivity) {
    //    private val modelPath = "tiny-clip-image-encoder.ptl"
    private val modelPath = "clip-image-encoder-fp16.onnx"

    //    private val modelPath = "clip-image-quant-cpu.pt"
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

//    private var module: Module? = null

    init {
        ortEnv = OrtEnvironment.getEnvironment()
        ortSession = createOrtSession()
    }

    private fun readModel(): ByteArray {
        return context.assets.open(modelPath).readBytes()
    }

    // Create a new ORT session in background
    private fun createOrtSession(): OrtSession? {
        val options = OrtSession.SessionOptions()
        options.addNnapi(EnumSet.of(NNAPIFlags.USE_FP16))
        return ortEnv?.createSession(assetFilePath(context, modelPath), options)
    }

    /**
     * 缩放为短边为224像素
     */
    private fun resize(bitmap: Bitmap): Bitmap {
        return if (bitmap.width < bitmap.height) {
            val longHeight = bitmap.height * 224 / bitmap.width
            Bitmap.createScaledBitmap(bitmap, 224, longHeight, false)
        } else {
            val longWidth = bitmap.width * 224 / bitmap.height
            Bitmap.createScaledBitmap(bitmap, longWidth, 224, false)
        }
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

    private fun preprocess(bitmap: Bitmap): Bitmap {
        val start = System.currentTimeMillis()
        val res = centerCrop(resize(bitmap))
        Log.d("preprocess", "${System.currentTimeMillis() - start} ms")
        return res
    }

    fun encode(bitmap: Bitmap): Array<FloatArray> {
        val start = System.currentTimeMillis()
        val imgData = bitmapToFloatBuffer(preprocess(bitmap))
        Log.d("bitmapToBuffer", "${System.currentTimeMillis() - start} ms")
//        val imgDataShort = floatBufferToFloat16Buffer(imgData)
//        Log.d("ONNX imgData size", imgData.limit().toString())
//        Log.d("ONNX imgDataShort size", imgDataShort.limit().toString())
        val inputName = ortSession?.inputNames?.iterator()?.next()
        val shape: LongArray = longArrayOf(1, 3, 224, 224)
        val env = OrtEnvironment.getEnvironment()
        env.use {
            val tensor = OnnxTensor.createTensor(env, imgData, shape)
            tensor.use {
                val start = System.currentTimeMillis()
                val output: OrtSession.Result? =
                    ortSession?.run(Collections.singletonMap(inputName, tensor))
                Log.d("ONNX cost", "${System.currentTimeMillis() - start} ms")
                output.use {
                    @Suppress("UNCHECKED_CAST")
                    val rawOutput = (output?.get(0)?.value) as Array<FloatArray>
                    return rawOutput
                }
            }
        }
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