package com.aa.ledger.data.remote

import retrofit2.http.GET

data class ExchangeRateResponse(
    val result: String,
    val base_code: String,
    val conversion_rates: Map<String, Double>
)

interface ExchangeRateApi {
    @GET("latest/CNY")
    suspend fun getRates(): ExchangeRateResponse
}
