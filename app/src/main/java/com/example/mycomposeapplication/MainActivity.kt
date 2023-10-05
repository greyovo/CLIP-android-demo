package com.example.mycomposeapplication

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.mycomposeapplication.ui.theme.MyComposeApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.concurrent.thread
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


private val imageList =
    listOf(
        "image@400px.jpg",
        "image@1000px.jpg",
        "image@4000px.jpg",
        "image@4000px-large.jpg",
        "image-large-17.2MB.jpg"
    )
private val textList =
    listOf(
        "A bird flying in the sky, cloudy",
        "A helicopter in water",
        "cat",
        "cat on the pavement",
        "pink rose in the pond",
        "red cloth inside blue bag",
    "white brown cat on pavement with shadow",
        "keyboard",
        "white computer keyboard keys",
        "dog face in cold weather"
    )

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyComposeApplicationTheme() {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    // Create a scroll state
                    val scrollState = rememberScrollState()

                    // Wrap the Column with verticalScroll
                    Column(
                        modifier = Modifier.verticalScroll(scrollState)
                    ) {
                        Greeting(selectedImage.value)
                        OutlinedButton(onClick = { imageListExpanded.value = true }) {
                            Text(text = "selectImage")
                            DropdownMenu(
                                expanded = imageListExpanded.value,
                                onDismissRequest = { imageListExpanded.value = false }
                            ) {
                                for (im in imageList) {
                                    DropdownMenuItem(onClick = {
                                        setImage(im)
                                    }) {
                                        Text(im)
                                    }
                                }
                            }
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

                        Button(onClick = { testBatchONNX() }) {
                            Text(text = "testBatchONNX")
                        }
                        Button(onClick = { testMultiThreadONNX() }) {
                            Text(text = "testMultiThreadONNX")
                        }

                        Text(text = encodeImageState1.value)
                        Text(text = encodeImageState2.value)
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use Quantized Model")
                            Switch(
                                checked = useQuantizedModel.value,
                                onCheckedChange = { newState ->
                                    useQuantizedModel.value = newState
                                    textEncoderONNX?.close()
                                    imageEncoderONNX?.close()
                                    textEncoderONNX = null
                                    imageEncoderONNX = null
                                    // You can also call any other logic that needs to run when the state changes
                                }
                            )
                        }
                        Button(onClick = { testScoring() }) {
                            Text(text = "testScore")
                        }
                        DisplayImage(imagePath = imagePathState.value)
                        Text(text = scoreState.value)
                    }
                }
            }
        }
        imagePath = assetFilePath(this, selectedImage.value).toString()
        imagePathState.value = imagePath
    }

    var imageListExpanded = mutableStateOf(false)
    private var selectedImage = mutableStateOf(imageList[0])
    private var tokenizerCost = mutableStateOf(0L)
    private var encodeTextCost: MutableState<Long> = mutableStateOf(0L)
    private var encodeImageCost: MutableState<Long> = mutableStateOf(0L)
    private var encodeImageState1: MutableState<String> = mutableStateOf("None")
    private var encodeImageState2: MutableState<String> = mutableStateOf("None")
    private var scoreState: MutableState<String> = mutableStateOf("")
    private var imagePathState: MutableState<String> = mutableStateOf("")
    private var useQuantizedModel: MutableState<Boolean> = mutableStateOf(true)

    private var textEncoderONNX: TextEncoderONNX? = null
    private var imageEncoderONNX: ImageEncoderONNX? = null

    private var imagePath: String = ""

    private  fun testTextEncoder() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (textEncoderONNX == null) {
                loadTextEncoderONNX()
            }
            val text = "A bird flying in the sky, cloudy"
            Log.i("testTextEncoder", "start...")
            val time = System.currentTimeMillis()
            textEncoderONNX?.encode(text)

            encodeTextCost.value = System.currentTimeMillis() - time
        }
    }


    private fun testImageEncoder() {
        lifecycleScope.launch {
            if (imageEncoderONNX == null) {
                loadImageEncoderONNX()
            }
            val time = System.currentTimeMillis()
            // 120ms+ for loading 4096px, 4.7MB JPEG
//            val bitmap = BitmapFactory.decodeFile(filesDir.path + "/" + selectedImage.value)
            // 70ms for loading 4096px, 4.7MB JPEG
//            val bitmap = decodeSampledBitmapFromFile(imagePath, 224, 224)
            // 64ms for loading 4096px, 4.7MB JPEG
            val bitmap = loadThumbnail(this@MainActivity, imagePath)
            Log.d("loadImage", "${System.currentTimeMillis() - time} ms")
            saveBitMap(this@MainActivity, bitmap, "decodeSampledBitmapFromFile")
            val output = imageEncoderONNX?.encode(bitmap)
            encodeImageCost.value = System.currentTimeMillis() - time
            Log.d("testImageEncoder", Arrays.toString(output))
        }
    }

    private fun testScoring() {
        lifecycleScope.launch {
            scoreState.value = ""
            if (imageEncoderONNX == null) {
                loadImageEncoderONNX()
            }
            if (textEncoderONNX == null) {
                loadTextEncoderONNX()
            }
            val time = System.currentTimeMillis()
            val bitmap = loadThumbnail(this@MainActivity, imagePath)
            Log.d("loadImage", "${System.currentTimeMillis() - time} ms")
            saveBitMap(this@MainActivity, bitmap, "decodeSampledBitmapFromFile")
            val imageEmbedding = imageEncoderONNX?.encode(bitmap)
            encodeImageCost.value = System.currentTimeMillis() - time

            // Create a mutable list to hold text and its corresponding score
            val scoreList = mutableListOf<Pair<String, Double>>()
            for (text in textList) {
                val textEmbedding = textEncoderONNX?.encode(text)
                if (imageEmbedding != null && textEmbedding != null) {
                    val score = computeScore(imageEmbedding[0], textEmbedding[0])
                    scoreList.add(Pair(text, score))
                }
            }
            scoreList.sortBy { -1.0 * it.second }
            val displayString = buildString {
                for ((text, score) in scoreList) {
                    append("\n%.4f".format(score))
                    append(" : $text")
                     // Limit to 4 digits
                }
            }
            scoreState.value = displayString

        }
    }

    private fun loadImageEncoderONNX() {
        if (imageEncoderONNX == null) {
            encodeImageState1.value = "Loading ImageEncoder ONNX ..."
            encodeImageState2.value = "Loading ImageEncoder ONNX ..."
            imageEncoderONNX = ImageEncoderONNX(context = this@MainActivity, useQuantizedModel.value)
            encodeImageState1.value = "Loading ImageEncoder ONNX done"
            encodeImageState2.value = "Loading ImageEncoder ONNX done"
        }
    }

    private suspend fun loadTextEncoderONNX() {
        Log.i(this.javaClass.canonicalName, "init loading textEncoderONNX")
        withContext(Dispatchers.Default) {
            if (textEncoderONNX == null) {
                Log.i(this.javaClass.canonicalName, "Starting loading textEncoder")
                textEncoderONNX = TextEncoderONNX(context = this@MainActivity, useQuantizedModel.value)
                Log.i(this.javaClass.canonicalName, "Done loading textEncoder")
            }
        }
    }


    private var batchTestLock = false

    /**
     * With SnapDragon 8+ gen 1 SoC, ONNX model:
     * - 500pics @ ~54s, 400px, 21KB, fp32
     * - 500pics @ ~20s, 400px, 21KB, int8
     * - 500pics @ ~27s, 1000px, 779KB, int8
     * - 500pics @ ~60s, 4096px, 1.7MB, int8
     * - 500pics @ ~87s, 4096px, 4MB, int8
     *
     * Decoding stream costs more time than model inference time
     * if the image is large.
     */
    private fun testBatchONNX() {
        if (batchTestLock) {
            Toast.makeText(this, "Already Running batch test!", Toast.LENGTH_SHORT).show()
            return
        }
        batchTestLock = true
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                if (imageEncoderONNX == null) {
                    loadImageEncoderONNX()
                }
                val total = 500
                val start = System.currentTimeMillis()
                for (i in 0..total) {
                    val _start = System.currentTimeMillis()
                    val bitmap = loadThumbnail(this@MainActivity, imagePath)
                    Log.d("decodeStream", "${System.currentTimeMillis() - _start} ms")
                    saveBitMap(this@MainActivity, bitmap, "temp-224.jpg")
                    imageEncoderONNX?.encode(bitmap)
                    if (i % 10 == 0) {
                        encodeImageState1.value =
                            "Processing `${selectedImage.value}`: $i / $total using ONNX..."
                    }
                }
                encodeImageState1.value =
                    "Processed: $total `${selectedImage.value}` images in ${System.currentTimeMillis() - start} ms using ONNX."
                batchTestLock = false
            }
        }
    }


    var encodeLock1 = false
    var encodeLock2 = false

    private fun testMultiThreadONNX() {
        assetFilePath(this, selectedImage.value)
        if (imageEncoderONNX == null) {
            loadImageEncoderONNX()
            return
        }
        if (encodeLock1) {
            Toast.makeText(this, "Already Running batch test!", Toast.LENGTH_SHORT).show()
            return
        } else {
            encodeLock1 = true
            thread(start = true, isDaemon = false, name = "DThread1", priority = 1) {
                suspend {
                    val total = 500
                    val start = System.currentTimeMillis()
                    for (i in 0..total) {
                        val bitmap =
                            BitmapFactory.decodeStream(File(filesDir.path + "/" + (selectedImage.value)).inputStream())
                        imageEncoderONNX?.encode(bitmap)
                        if (i % 10 == 0) {
                            encodeImageState1.value =
                                "Processing `${selectedImage.value}`: $i / $total"
                        }
                    }
                    encodeImageState1.value =
                        "Processed: $total `${selectedImage.value}` images in ${System.currentTimeMillis() - start} ms"
                    encodeLock1 = false
                }

            }
        }
        if (encodeLock2) {
            Toast.makeText(this, "Already Running batch test!", Toast.LENGTH_SHORT).show()
            return
        } else {
            encodeLock2 = true
            thread(start = true, isDaemon = false, name = "DThread2", priority = 1) {
                suspend {
                    val total = 500
                    val start = System.currentTimeMillis()
                    for (i in 0..total) {
                        val bitmap = loadThumbnail(this, imagePath)
                        imageEncoderONNX?.encode(bitmap)
                        if (i % 10 == 0) {
                            encodeImageState2.value =
                                "Processing `${selectedImage.value}`: $i / $total"
                        }
                    }
                    encodeImageState2.value =
                        "Processed: $total `${selectedImage.value}` images in ${System.currentTimeMillis() - start} ms"
                    encodeLock2 = false
                }
            }

        }
    }

    private fun setImage(im: String) {
        imageListExpanded.value = false
        selectedImage.value = im
        imagePath = assetFilePath(this@MainActivity, selectedImage.value).toString()
        imagePathState.value = imagePath
        if (imagePath.isEmpty()) {
            Toast.makeText(this, "图片加载失败！", Toast.LENGTH_SHORT).show()
        } else {
            scoreState.value = ""
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Selected image: $name")
}
@Composable
fun DisplayImage(imagePath: String) {
    val bitmap = BitmapFactory.decodeFile(imagePath)
    val imageBitmap = bitmap?.asImageBitmap()

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null, // decorative
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )
    } else {
        // Handle the case where the image could not be loaded
    }
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