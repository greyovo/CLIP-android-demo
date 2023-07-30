package com.example.mycomposeapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import android.util.Log
import org.pytorch.*
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.util.*


class ImageEncoder(private val context: Context) {
    private val modelPath = "clip-image-encoder.ptl"

    //    private val modelPath = "clip-image-encoder-quantized.ptl"
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
        val mInputTensor = Tensor.fromBlob(
            mInputTensorBuffer, longArrayOf(1, 3, 224, 224),
            MemoryFormat.CHANNELS_LAST,
        )
        return mInputTensor
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
        saveBitMap(resized, "resized")
        val cropped = centerCrop(resized)
        saveBitMap(cropped, "cropped")
        val tensor = toTensor(toRGB(cropped))
        tensor.dtype()

        Log.i(
            "ImageEncoder.tensor",
            tensor.dataAsFloatArray[tensor.dataAsFloatArray.size - 1].toString()
        )
        return module?.forward(IValue.from(tensor))?.toTensor()
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