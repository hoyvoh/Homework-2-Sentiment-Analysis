package com.example.homework2

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var editTextInput: EditText
    private lateinit var buttonSubmit: Button
    private lateinit var textViewResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        editTextInput = findViewById(R.id.editTextInput)
        buttonSubmit = findViewById(R.id.buttonSubmit)
        textViewResult = findViewById(R.id.textViewResult)

        buttonSubmit.setOnClickListener {
            val userInput = editTextInput.text.toString().trim()
            if (userInput.isNotEmpty()) {
                analyzeSentiment(userInput)
            } else {
                textViewResult.text = "Please enter text to analyze."
            }
        }
    }

    private fun analyzeSentiment(text: String) {
        val url = "https://language.googleapis.com/v2/documents:analyzeSentiment"
        val apiKey = BuildConfig.GOOGLE_API_KEY  // ✅ Get API key from BuildConfig

        val json = """
        {
            "encodingType": "UTF8",
            "document": {
                "type": "PLAIN_TEXT",
                "content": "$text"
            }
        }
    """.trimIndent()

        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$url?key=$apiKey")  // ✅ Attach API key as a query parameter
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    textViewResult.text = "Error: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val responseText = responseBody.string()
                    println("API Response: $responseText")  // ✅ Debugging log

                    try {
                        val jsonObject = JSONObject(responseText)

                        // ✅ Extract sentiment score
                        val documentSentiment = jsonObject.optJSONObject("documentSentiment")
                        if (documentSentiment != null) {
                            val score = documentSentiment.optDouble("score", 0.0)

                            runOnUiThread {
                                updateUI(score)
                            }
                        } else {
                            runOnUiThread {
                                textViewResult.text = "Error: Missing sentiment data!"
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        runOnUiThread {
                            textViewResult.text = "Error: Failed to parse response!"
                        }
                    }
                }
            }
        })
    }

    // ✅ Updates UI with emoji + sentiment text
    private fun updateUI(score: Double) {
        when {
            score > 0.2 -> {
                textViewResult.text = "😊 Positive"
            }
            score < -0.2 -> {
                textViewResult.text = "😢 Negative"
            }
            else -> {
                textViewResult.text = "😐 Neutral"
            }
        }
    }
}
