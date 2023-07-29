package com.example.mycomposeapplication

import android.content.Context
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mycomposeapplication.ui.theme.MyComposeApplicationTheme
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        testTokenizer()
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
                    }
                }
            }
        }
    }

    var tokenizer: BPETokenizer? = null

    private fun testTokenizer() {
        if (tokenizer == null) {
            tokenizer = BPETokenizer(applicationContext)
        }
        val text = "a diagram"
        Log.i("testTokenizer", "start testTokenizer")
        val tensor = tokenizer!!.tokenize(text)
        Log.i("testTokenizer", Arrays.toString(tensor.dataAsIntArray))
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