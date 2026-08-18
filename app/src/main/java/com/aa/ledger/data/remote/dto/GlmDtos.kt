package com.aa.ledger.data.remote.dto

import com.google.gson.annotations.SerializedName

// ── GLM 视觉模型 请求/响应 DTO ────────────────────────────────

data class GlmRequest(
    val model: String = "glm-4.1v-thinking-flash",
    val messages: List<GlmMessage> = emptyList()
)

data class GlmMessage(
    val role: String = "user",
    val content: List<GlmContentPart> = emptyList()
)

data class GlmContentPart(
    val type: String,                          // "text" 或 "image_url"
    val text: String? = null,
    @SerializedName("image_url") val imageUrl: GlmImageUrl? = null
)

data class GlmImageUrl(val url: String)

data class GlmResponse(
    val choices: List<GlmChoice> = emptyList()
)

data class GlmChoice(
    val message: GlmRespMessage? = null
)

data class GlmRespMessage(
    val content: String? = null
)

// ── 结构化小票结果 ────────────────────────────────────────────

data class GlmReceipt(
    val total: Double? = null,
    val currency: String? = null,
    val subtotal: Double? = null,
    val tax: Double? = null,
    val merchant: String? = null
)
