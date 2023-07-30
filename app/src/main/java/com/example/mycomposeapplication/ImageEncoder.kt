package com.example.mycomposeapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.util.Log
import org.pytorch.*
import org.pytorch.torchvision.TensorImageUtils


class ImageEncoder(private val context: Context) {
    private val modelPath = "clip-image-encoder.ptl"
    private var module: Module? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        // loading serialized torchscript module from packaged into app android asset model.pt,
        // app/src/model/assets/model.pt
        val time = System.currentTimeMillis()
        module = LiteModuleLoader.load(assetFilePath(context, modelPath))
        val dur = (System.currentTimeMillis() - time) * 1000
        Log.i("ImageEncoder", "load cost: $dur s")
    }

    private fun resize(bitmap: Bitmap): Bitmap {
        // 获取图片的宽度和高度
        val width = bitmap.width
        val height = bitmap.height

        // 计算缩放比例
        val scale = (224f / width).coerceAtMost(224f / height)

        // 创建缩放后的Bitmap
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
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
        return TensorImageUtils.bitmapToFloat32Tensor(
            bitmap,
            floatArrayOf(0.485f, 0.456f, 0.406f),
            floatArrayOf(0.229f, 0.224f, 0.225f),
            MemoryFormat.CHANNELS_LAST
        )
//        floatArrayOf(0.485f, 0.456f, 0.406f)
//        floatArrayOf(0.229f, 0.224f, 0.225f)
//        (0.48145466, 0.4578275, 0.40821073), (0.26862954, 0.26130258, 0.27577711)
    }


    // Compose([
    //    Resize(n_px, interpolation=InterpolationMode.BICUBIC),
    //    CenterCrop(n_px),
    //    _convert_image_to_rgb,
    //    ToTensor(),
    //    Normalize((0.48145466, 0.4578275, 0.40821073), (0.26862954, 0.26130258, 0.27577711)),
    // ])
    private fun preprocessImage() {

    }

    fun encode(input: Bitmap): Tensor? {
        if (module == null) {
            loadModel()
        }
        val resized = resize(input)
        val cropped = centerCrop(resized)
        val tensor = toTensor(toRGB(cropped))

        return module?.forward(IValue.from(tensor))?.toTensor()
    }
}