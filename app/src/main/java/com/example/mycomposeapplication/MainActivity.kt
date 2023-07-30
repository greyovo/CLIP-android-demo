package com.example.mycomposeapplication

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.example.mycomposeapplication.ui.theme.MyComposeApplicationTheme
import kotlinx.coroutines.*
import org.pytorch.MemoryFormat
import org.pytorch.torchvision.TensorImageUtils
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyComposeApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column {
                        Greeting("Android")
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
                        Text(text = encodeImageState.value)
                    }
                }
            }
        }
    }

    private var tokenizerCost: MutableState<Long> = mutableStateOf(0L)
    private var encodeTextCost: MutableState<Long> = mutableStateOf(0L)
    private var encodeImageCost: MutableState<Long> = mutableStateOf(0L)
    private var encodeImageState: MutableState<String> = mutableStateOf("None")


    private var tokenizer: BPETokenizer? = null
    private var textEncoder: TextEncoder? = null
    private var imageEncoder: ImageEncoder? = null

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
        val output = textEncoder?.encode(input)
        encodeTextCost.value = System.currentTimeMillis() - time
//        Log.i("testTextEncoder", "encode cost: ${(System.currentTimeMillis() - time)}")
//        Log.d("testTextEncoder", Arrays.toString(output?.dataAsFloatArray))
    }

    private fun testImageEncoder() {
        if (imageEncoder == null) {
            imageEncoder = ImageEncoder(this)
        }

        // creating bitmap from packaged into app android asset 'image.jpg',
        // app/src/main/assets/image.jpg
        val bitmap = BitmapFactory.decodeStream(assets.open("image.jpg"))
        Log.i("testImageEncoder", "start...")
        val time = System.currentTimeMillis()
        val output = imageEncoder?.encode(bitmap)
        encodeImageCost.value = System.currentTimeMillis() - time
        Log.d("testImageEncoder", Arrays.toString(output?.dataAsFloatArray))
    }

    private fun testBatchEncodeImage() {
        if (imageEncoder == null) {
            imageEncoder = ImageEncoder(this)
        }
        // 在UI线程中启动一个协程
        lifecycleScope.launch(Dispatchers.Main) {
            // 在后台线程中执行任务
            val result = withContext(Dispatchers.Default) {
                val total = 100
                val start = System.currentTimeMillis()
                for (i in 0..total) {
                    val bitmap = BitmapFactory.decodeStream(assets.open("image.jpg"))
                    imageEncoder?.encode(bitmap)
                    if (i % 10 == 0) {
                        encodeImageState.value = "Processing: $i / $total"
                    }
                }
                encodeImageState.value =
                    "Processed: $total / $total images in ${System.currentTimeMillis() - start} ms"
            }
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
            Greeting("Android")
        }
    }
}