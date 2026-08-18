package com.aa.ledger.data.remote

import com.aa.ledger.data.remote.dto.GlmRequest
import com.aa.ledger.data.remote.dto.GlmResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GlmApi {
    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body request: GlmRequest
    ): Response<GlmResponse>
}
