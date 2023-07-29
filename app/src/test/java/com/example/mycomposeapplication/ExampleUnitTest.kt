package com.example.mycomposeapplication

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun tokenizer_isOk() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
//        val t = tokenize("a diagram")
        val text = "a diagram"
        val tokenizer = BPETokenizer(context)

        println(tokenizer.tokenize(text))
    }
}