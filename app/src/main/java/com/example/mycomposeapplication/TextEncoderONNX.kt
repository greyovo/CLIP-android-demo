package com.example.mycomposeapplication

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.widget.Toast
import org.pytorch.*
import java.nio.IntBuffer
import java.util.*

class TextEncoderONNX(private val context: Context, useQuantizedModel: Boolean = true)  {
    private val modelPath = if (useQuantizedModel) {
        "clip-text-encoder-quant-int8.onnx"
    } else {
        "clip-text-encoder.onnx"
    }
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var tokenizer: BPETokenizer? = null

    init {
        ortEnv = OrtEnvironment.getEnvironment()
        ortSession = createOrtSession()
        tokenizer = BPETokenizer(context)
    }

    private fun createOrtSession(): OrtSession? {
        val p = assetFilePath(context, modelPath) ?: return null
        return ortEnv?.createSession(p)
    }

    fun encode(input: String): Array<FloatArray>? {
        if (tokenizer == null || ortSession == null) {
            Toast.makeText(context, "TextEncoderONNX init failed!", Toast.LENGTH_SHORT).show()
            return null
        }

        val token = tokenizer?.tokenize(input)
        val buffer = IntBuffer.wrap(token?.first)
        val shape = token?.second
        val inputName = ortSession?.inputNames?.iterator()?.next()

        val env = OrtEnvironment.getEnvironment()
        env.use {
            val tensor = OnnxTensor.createTensor(env, buffer, shape)
            tensor.use {
                val output = ortSession?.run(Collections.singletonMap(inputName, tensor))
                output.use {
                    @Suppress("UNCHECKED_CAST")
                    return (output?.get(0)?.value) as Array<FloatArray>
                }
            }
        }
    }

}