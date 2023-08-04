package com.example.mycomposeapplication

import ai.onnxruntime.OrtSession
import android.app.Dialog
import android.app.FragmentManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.example.mycomposeapplication.ui.theme.MyComposeApplicationTheme
import kotlinx.coroutines.*
import org.pytorch.Tensor
import java.io.File
import java.util.*
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyComposeApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column {
                        Greeting("Android")
                        Button(onClick = { testTokenizer() }) {
                            Text(text = "testTokenizer")
                        }
                        Text(text = "tokenizerCost: ${tokenizerCost.value} ms")

                        Button(onClick = { testTextEncoder() }) {
                            Text(text = "testTextEncoder")
                        }
                        Text(text = "testTextEncoder: ${encodeTextCost.value} ms")

                        Button(onClick = { testImageEncoder() }) {
                            Text(text = "testImageEncoder")
                        }
                        Text(text = "testImageEncoder: ${encodeImageCost.value} ms")

                        Button(onClick = { testBatchEncodeImage() }) {
                            Text(text = "encode batch images")
                        }

                        Button(onClick = { testMultiThread() }) {
                            Text(text = "encode batch images in MultiThread")
                        }


                        Button(onClick = { testBatchONNX() }) {
                            Text(text = "testBatchONNX")
                        }


                        Text(text = encodeImageState1.value)
                        Text(text = encodeImageState2.value)
                    }
                }
            }
        }
    }

    private var tokenizerCost: MutableState<Long> = mutableStateOf(0L)
    private var encodeTextCost: MutableState<Long> = mutableStateOf(0L)
    private var encodeImageCost: MutableState<Long> = mutableStateOf(0L)
    private var encodeImageState1: MutableState<String> = mutableStateOf("None")
    private var encodeImageState2: MutableState<String> = mutableStateOf("None")


    private var tokenizer: BPETokenizer? = null
    private var textEncoder: TextEncoder? = null
    private var imageEncoder: ImageEncoder? = null
    private var textEncoderONNX: TextEncoderONNX? = null
    private var imageEncoderONNX: ImageEncoderONNX? = null

    private fun testTextEncoder() {
        if (textEncoder == null) {
            textEncoder = TextEncoder(this)
        }
        if (tokenizer == null) {
            tokenizer = BPETokenizer(this)
        }
        val text = "A bird flying in the sky, cloudy"
        Log.i("testTextEncoder", "start...")
        val time = System.currentTimeMillis()
        val input = tokenizer!!.tokenize(text)
        val output = textEncoder?.encode(Tensor.fromBlob(input.first, input.second))
        encodeTextCost.value = System.currentTimeMillis() - time
//        Log.i("testTextEncoder", "encode cost: ${(System.currentTimeMillis() - time)}")
//        Log.d("testTextEncoder", Arrays.toString(output?.dataAsFloatArray))
    }

    private val imageName = "image-1000px.jpg"

    private fun testImageEncoder() {
//        if (imageEncoder == null) {
//            imageEncoder = ImageEncoder(this)
//        }

        if (imageEncoderONNX == null) {
            imageEncoderONNX = ImageEncoderONNX(this)
        }

        // creating bitmap from packaged into app android asset 'image.jpg',
        // app/src/main/assets/image.jpg
        val time = System.currentTimeMillis()
        val bitmap: Bitmap? = BitmapFactory.decodeStream(assets.open(imageName))
        Log.d("decodeStream", "${System.currentTimeMillis() - time} ms")
        if (bitmap == null) {
            Toast.makeText(this, "无法解码图片", Toast.LENGTH_LONG).show()
            encodeImageCost.value = 0
            return
        }
//        val output = imageEncoder?.encode(bitmap)
        val output = imageEncoderONNX?.encode(bitmap)
        encodeImageCost.value = System.currentTimeMillis() - time
        Log.d("testImageEncoder", Arrays.toString(output))
    }

    private fun loadImageEncoder() {
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                if (imageEncoder == null) {
                    encodeImageState1.value = "Loading ImageEncoder ..."
                    encodeImageState2.value = "Loading ImageEncoder ..."
                    imageEncoder = ImageEncoder(context = this@MainActivity)
                    encodeImageState1.value = "Loading ImageEncoder done"
                    encodeImageState2.value = "Loading ImageEncoder done"
                }
            }
        }
    }

    private fun loadImageEncoderONNX() {
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                if (imageEncoder == null) {
                    encodeImageState1.value = "Loading ImageEncoder ONNX ..."
                    encodeImageState2.value = "Loading ImageEncoder ONNX ..."
                    imageEncoderONNX = ImageEncoderONNX(context = this@MainActivity)
                    encodeImageState1.value = "Loading ImageEncoder ONNX done"
                    encodeImageState2.value = "Loading ImageEncoder ONNX done"
                }
            }
        }
    }

    private fun loadTextEncoderONNX() {
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                if (imageEncoder == null) {
                    textEncoderONNX = TextEncoderONNX(context = this@MainActivity)
                }
            }
        }
    }

    private var batchTestLock = false

    /**
     * With SnapDragon 8+ gen 1 SoC:
     * - Encoding 100 images for ~11s
     * - Encoding 2000 images for ~25s
     */
    private fun testBatchEncodeImage() {
        assetFilePath(this, imageName)
        if (imageEncoder == null) {
            loadImageEncoder()
            return
        }
        if (batchTestLock) {
            Toast.makeText(this, "Already Running batch test!", Toast.LENGTH_SHORT).show()
            return
        }
        batchTestLock = true
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                val total = 500
                val start = System.currentTimeMillis()
                for (i in 0..total) {
//                    val bitmap = BitmapFactory.decodeStream(assets.open(imageName))
                    val bitmap =
                        BitmapFactory.decodeStream(File(filesDir.path + "/" + (imageName)).inputStream())
                    imageEncoder?.encode(bitmap)
                    if (i % 10 == 0) {
                        encodeImageState1.value = "Processing `$imageName`: $i / $total"
                    }
                }
                encodeImageState1.value =
                    "Processed: $total `$imageName` images in ${System.currentTimeMillis() - start} ms"
                batchTestLock = false
            }
        }
    }

    private fun testBatchONNX() {
        assetFilePath(this, imageName)
        if (imageEncoderONNX == null) {
            loadImageEncoderONNX()
            return
        }
        if (batchTestLock) {
            Toast.makeText(this, "Already Running batch test!", Toast.LENGTH_SHORT).show()
            return
        }
        batchTestLock = true
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                val total = 500
                val start = System.currentTimeMillis()
                for (i in 0..total) {
//                    val bitmap = BitmapFactory.decodeStream(assets.open(imageName))
                    val _start = System.currentTimeMillis()
                    val bitmap =
                        BitmapFactory.decodeStream(File(filesDir.path + "/" + (imageName)).inputStream())
                    Log.d("decodeStream", "${System.currentTimeMillis() - _start} ms")
                    imageEncoderONNX?.encode(bitmap)
                    if (i % 10 == 0) {
                        encodeImageState1.value =
                            "Processing `$imageName`: $i / $total using ONNX..."
                    }
                }
                encodeImageState1.value =
                    "Processed: $total `$imageName` images in ${System.currentTimeMillis() - start} ms using ONNX."
                batchTestLock = false
            }
        }
    }

    var encodeLock1 = false
    var encodeLock2 = false
    private fun testMultiThread() {
        assetFilePath(this, imageName)
        if (imageEncoder == null) {
            loadImageEncoder()
            return
        }
        if (encodeLock1) {
            Toast.makeText(this, "Already Running batch test!", Toast.LENGTH_SHORT).show()
            return
        } else {
            encodeLock1 = true
            thread(start = true, isDaemon = false, name = "DThread1", priority = 1) {
                val total = 500
                val start = System.currentTimeMillis()
                for (i in 0..total) {
                    val bitmap =
                        BitmapFactory.decodeStream(File(filesDir.path + "/" + (imageName)).inputStream())
                    imageEncoder?.encode(bitmap)
                    if (i % 10 == 0) {
                        encodeImageState1.value = "Processing `$imageName`: $i / $total"
                    }
                }
                encodeImageState1.value =
                    "Processed: $total `$imageName` images in ${System.currentTimeMillis() - start} ms"
                encodeLock1 = false
            }
        }
        if (encodeLock2) {
            Toast.makeText(this, "Already Running batch test!", Toast.LENGTH_SHORT).show()
            return
        } else {
            encodeLock2 = true
            thread(start = true, isDaemon = false, name = "DThread2", priority = 1) {
                val total = 500
                val start = System.currentTimeMillis()
                for (i in 0..total) {
                    val bitmap =
                        BitmapFactory.decodeStream(File(filesDir.path + "/" + (imageName)).inputStream())
                    imageEncoder?.encode(bitmap)
                    if (i % 10 == 0) {
                        encodeImageState2.value = "Processing `$imageName`: $i / $total"
                    }
                }
                encodeImageState2.value =
                    "Processed: $total `$imageName` images in ${System.currentTimeMillis() - start} ms"
            }
            encodeLock2 = false
        }
    }

    private fun testTokenizer() {
        tokenizer = tokenizer ?: BPETokenizer(this)

        val text = "A bird flying in the sky, cloudy"
        Log.i("testTokenizer", "start...")
        val time = System.currentTimeMillis()
        val tensor = tokenizer?.tokenize(text)
        tokenizerCost.value = System.currentTimeMillis() - time
//        Log.i("testTokenizer", Arrays.toString(tensor?.dataAsIntArray))
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyComposeApplicationTheme {
        Column {
            Greeting("Android")
        }
    }
}