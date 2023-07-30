package com.example.mycomposeapplication

import android.content.Context
import org.pytorch.Tensor
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream


//fun bytesToUnicode(): Map<Byte, String> {
//    val charset = Charset.forName("UTF-8")
//    val bytes = ByteArray(256) { it.toByte() }
//    val buffer = ByteBuffer.wrap(bytes)
//    val unicodeChars = charset.decode(buffer).toString().toCharArray().map { it.toString() }
//
//    return bytes.mapIndexed { index, byte ->
//        byte to unicodeChars[index]
//    }.toMap()
//}

fun createCharDict(): Map<Int, Char> {
    val bytesList = mutableListOf<Int>()
    bytesList.addAll(33..126)
    bytesList.addAll(161..172)
    bytesList.addAll(174..255)
    val charList = bytesList.toMutableList()
    var n = 0
    for (b in 0..255) {
        if (b !in bytesList) {
            bytesList.add(b)
            charList.add(256 + n)
            n++
        }
    }
    return bytesList.zip(charList.map { it.toChar() }).toMap()
}

fun readGzipFile(context: Context, assetName: String): List<String> {
    val filePath = assetFilePath(context, assetName)
    val result = mutableListOf<String>()
    val inputStream = GZIPInputStream(FileInputStream(File(filePath!!)))
    val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        result.add(line!!)
    }
    reader.close()
    inputStream.close()
    return result
}

class BPETokenizer(context: Context, bpePath: String = "bpe_vocab") {

    private val byteEncoder = createCharDict()
    private val byteDecoder = byteEncoder.map { it.value to it.key }.toMap()

    private val merges: List<Pair<String, String>>
    val encoder: Map<String, Int>
    private val decoder: Map<Int, String>
    private val bpeRanks: Map<Pair<String, String>, Int>
    private val cache: MutableMap<String, String>

    private val pat = Pattern.compile(
        "<\\|startoftext\\|>|<\\|endoftext\\|>|'s|'t|'re|'ve|'m|'ll|'d|[\\p{L}]+|[\\p{N}]|[^\\s\\p{L}\\p{N}]+",
    )

    init {
        val vocab: MutableList<String> = byteEncoder.values.map { it.toString() }.toMutableList()
        vocab.addAll(vocab.map { "$it</w>" }.toList())

        val mergesFile: List<String> = readGzipFile(context, bpePath)
        merges =
            mergesFile.subList(1, 49152 - 256 - 2 + 1).map {
                val sp = it.split(" ")
                Pair(sp[0], sp[1])
            }

        vocab.addAll(merges.map { it.first + it.second })
        vocab.addAll(listOf("<|startoftext|>", "<|endoftext|>"))

        encoder = vocab.withIndex()
            .associateBy({ it.value }, { it.index })
        decoder = encoder
            .map { it.value to it.key }.toMap()
        bpeRanks = merges.mapIndexed { index, pair -> pair to index }.toMap()
        cache = mutableMapOf(
            "<|startoftext|>" to "<|startoftext|>", "<|endoftext|>" to "<|endoftext|>"
        )
    }

    private fun bpe(token: String): String {
        if (cache.containsKey(token)) {
            return cache[token]!!
        }

//        val word = (token.dropLast(1) + "${token.last()}</w>")
        var word: MutableList<String> = token.dropLast(1).map { it.toString() }.toMutableList()
        word.add(token.last().toString() + "</w>")
        var pairs: List<Pair<String, String>> = getPairs(word)

        if (pairs.isEmpty()) {
            return "$token</w>"
        }
//        var result = mutableListOf<String>()
        while (true) {
            val bigram: Pair<String, String> =
                pairs.minByOrNull { bpeRanks[it] ?: Int.MAX_VALUE } ?: break
            if (!bpeRanks.containsKey(bigram)) break

            val (first, second) = bigram

            val newWord = mutableListOf<String>()
            var i = 0
            while (i < word.size) {
                val j = word.subList(i, word.size).indexOf(first)
                if (j != -1) {
                    newWord.addAll(word.subList(i, j + i))
                    i = j
                } else {
                    newWord.addAll(word.subList(i, word.size))
                    break
                }

                if (i < word.size - 1 && word[i] == first && word[i + 1] == second) {
                    newWord.add(first + second)
                    i += 2
                } else {
                    newWord.add(word[i])
                    i++
                }
            }
            word = newWord

            if (word.size == 1) break
            else pairs = getPairs(word.toList())
        }
        val result = word.joinToString(" ")
        cache[token] = result
        return result
    }

    fun encode(text: String): List<Int> {
        val cleanedText = whitespaceClean(text).lowercase()
        val matcher = pat.matcher(cleanedText)
        val matches = mutableListOf<String>()
        while (matcher.find()) {
            val match = matcher.group()
            matches.add(match)
        }
        val bpeTokens = mutableListOf<Int>()
//        for token in re.findall(self.pat, text):
//        token = ''.join(self.byte_encoder[b] for b in token.encode('utf-8'))
//        bpe_tokens.extend(self.encoder[bpe_token] for bpe_token in self.bpe(token).split(' '))

//        return bpe_tokens
        for (token in matches) {
            val encodedToken = token.toByteArray().map { byteEncoder[it.toInt()] }.joinToString("")
            for (bpe_token in bpe(encodedToken).split(" ")) {
                bpeTokens.add(encoder[bpe_token]!!)
            }
        }
        return bpeTokens
//        return matches.map { match ->
//            val token = match.toByteArray().toString(Charsets.UTF_8)
//            bpe(token).split(' ').map { encoder[it]!! }
//        }.flatten()
    }
//    fun encode(text: String): List<String> {
//        val bpeTokens = mutableListOf<String>()
//        val cleanedText = whitespaceClean(text).lowercase()
//        val utf8Encoder = Charset.forName("UTF-8").newEncoder()
//        val matcher = pat.matcher(cleanedText)
//        val matches = mutableListOf<String>()
//        while (matcher.find()) {
//            val match = matcher.group()
//            matches.add(match)
//        }
//        for (token in matches) {
//            val encodedBytes = byteEncoder[utf8Encoder.encode(token)]
//            val encodedToken = encodedBytes.array().joinToString("") { byte ->
//                byteEncoder.encode(byte.toString()).array().joinToString("") { it.toString() }
//            }
//            val bpeTokenList = bpe(encodedToken).split(' ')
//            bpeTokens.addAll(bpeTokenList.map { encoder[it] })
//        }
//        return bpeTokens
//    }

    fun decode(tokens: List<Int>): String {
//        val text = tokens.map { decoder[it]!! }.joinToString("")
        val text = tokens.joinToString("") { decoder[it]!! }
        return text.toByteArray().toString(Charsets.UTF_8).replace("</w>", " ")
    }


    fun tokenize(text: String, contextLength: Int = 77, truncate: Boolean = false): Tensor {
        val sotToken: Int = encoder["<|startoftext|>"]!!
        val eotToken: Int = encoder["<|endoftext|>"]!!
//    val allTokens: MutableList<MutableList<Int>> = ArrayList()
        val tokens: MutableList<Int> = ArrayList()
        tokens.add(sotToken)
        tokens.addAll(encode(text))
        tokens.add(eotToken)

//    if (DefaultTensorFactory.VERSION < 1.8) {
//        result = Tensor.zeros(tokens.size, contextLength, java.lang.Long.TYPE)
//    } else {
//        result = Tensor.zeros(tokens.size, contextLength, Integer.TYPE)
//    }

        if (tokens.size > contextLength) {
            if (truncate) {
                val truncatedTokens = tokens.subList(0, contextLength)
                truncatedTokens[contextLength - 1] = eotToken
            } else {
                throw java.lang.RuntimeException("Input $text is too long for context length $contextLength")
            }
        }
        val a = IntArray(contextLength) {
            if (it < tokens.size)
                tokens[it]
            else 0
        }
        val shape = longArrayOf(1, contextLength.toLong())
        val result: Tensor = Tensor.fromBlob(a, shape)

        return result
    }

}

fun getPairs(word: List<String>): List<Pair<String, String>> {
    return word.zipWithNext().map { it.first to it.second }
}

fun whitespaceClean(text: String): String {
    var cleanedText = text.replace(Regex("\\s+"), " ")
    cleanedText = cleanedText.trim()
    return cleanedText
}


//fun tokenize(texts: List<String>, contextLength: Int, truncate: Boolean = true): Tensor {
//
//    val tokenizer = BPETokenizer() // get tokenizer
//    val sotToken = tokenizer.encoder["<|startoftext|>"]!!
//    val eotToken = tokenizer.encoder["<|endoftext|>"]!!
//
//    val allTokens = texts.map { text ->
//        listOf(sotToken) + tokenizer.encode(text) + eotToken
//    }
//
//    val result = PyTorchAndroid.zeros(allTokens.size, contextLength, torch.int)
//
//    allTokens.forEachIndexed { index, tokens ->
//        if (tokens.size > contextLength) {
//            if (truncate) {
//                tokens = tokens.take(contextLength)
//                tokens[contextLength - 1] = eotToken
//            } else {
//                throw RuntimeException("Input $texts[index] is too long for context length $contextLength")
//            }
//        }
//        result[index, 0 until tokens.size] = tokens.toTensor()
//    }
//
//    return result
//}

