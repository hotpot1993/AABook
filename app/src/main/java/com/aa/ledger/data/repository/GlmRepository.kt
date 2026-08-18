package com.aa.ledger.data.repository

import com.aa.ledger.BuildConfig
import com.aa.ledger.data.remote.GlmApi
import com.aa.ledger.data.remote.dto.*
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlmRepository @Inject constructor() {

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val api: GlmApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://open.bigmodel.cn/api/paas/v4/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GlmApi::class.java)
    }

    private val gson = Gson()

    /** 用 GLM 视觉模型识别小票，返回结构化结果；失败/未配置 key 返回 null */
    suspend fun recognizeReceipt(imageBytes: ByteArray, mime: String): GlmReceipt? {
        val key = BuildConfig.GLM_API_KEY
        if (key.isBlank()) return null

        return try {
            val base64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            val dataUrl = "data:$mime;base64,$base64"
            val prompt = "这是一张小票/账单图片。请提取金额信息，只返回 JSON（不要 markdown、不要解释）：" +
                "{\"total\": 合计/实付总金额（数字，例如 1000 或 12.34），" +
                "\"currency\": 货币代码（CNY/USD/JPY/EUR/HKD 等，无法判断填 null），" +
                "\"subtotal\": 小计金额（数字，无则 null），" +
                "\"tax\": 税额（数字，无则 null），" +
                "\"merchant\": 商户名称（字符串，无则 null）}"

            val request = GlmRequest(
                messages = listOf(
                    GlmMessage(
                        content = listOf(
                            GlmContentPart(type = "image_url", imageUrl = GlmImageUrl(dataUrl)),
                            GlmContentPart(type = "text", text = prompt)
                        )
                    )
                )
            )

            val response = api.chat("Bearer $key", request)
            if (!response.isSuccessful) {
                android.util.Log.e("GlmRepository", "GLM HTTP ${response.code()}")
                return null
            }
            val content = response.body()?.choices?.firstOrNull()?.message?.content ?: return null
            android.util.Log.d("GlmRepository", "GLM 返回: $content")
            val json = extractJson(content)
            gson.fromJson(json, GlmReceipt::class.java)
        } catch (e: Exception) {
            android.util.Log.e("GlmRepository", "GLM OCR error", e)
            null
        }
    }

    /** 从模型输出中提取 JSON（去掉 markdown 代码块与多余文字） */
    private fun extractJson(content: String): String {
        var s = content.trim()
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```JSON").removePrefix("```").trim()
            s = s.substringBefore("```").trim()
        }
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        return if (start >= 0 && end > start) s.substring(start, end + 1) else s
    }
}
