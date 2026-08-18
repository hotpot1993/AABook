package com.aa.ledger.data.remote

import com.aa.ledger.data.remote.dto.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface CloudApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/ledgers")
    suspend fun getLedgers(@Header("Authorization") token: String): Response<LedgerListResponse>

    @POST("api/ledgers")
    suspend fun createLedger(@Header("Authorization") token: String, @Body request: CreateLedgerRequest): Response<CreateLedgerResponse>

    @GET("api/ledgers/{id}")
    suspend fun getLedgerDetail(@Header("Authorization") token: String, @Path("id") ledgerId: Int): Response<LedgerDetailResponse>

    @POST("api/ledgers/join-by-code")
    suspend fun joinLedger(@Header("Authorization") token: String, @Body request: JoinLedgerRequest): Response<JoinLedgerResponse>

    @DELETE("api/ledgers/my")
    suspend fun clearMyLedgers(@Header("Authorization") token: String): Response<Map<String, Any>>

    // Backup endpoints
    @GET("api/backup")
    suspend fun downloadBackup(@Header("Authorization") token: String): Response<okhttp3.ResponseBody>

    @POST("api/backup")
    @Headers("Content-Type: application/zip")
    suspend fun uploadBackup(
        @Header("Authorization") token: String,
        @Header("X-Backup-Mode") mode: String,
        @Header("X-Base-Revision") baseRevision: Long,
        @Body backup: okhttp3.RequestBody
    ): Response<okhttp3.ResponseBody>

    // Admin
    @GET("api/admin/check")
    suspend fun checkAdmin(@Header("Authorization") token: String): Response<Map<String, Any>>

    @GET("api/admin/users")
    suspend fun getAdminUsers(@Header("Authorization") token: String): Response<Map<String, Any>>

    @DELETE("api/admin/users/{id}")
    suspend fun deleteAdminUser(@Header("Authorization") token: String, @Path("id") userId: Int): Response<Map<String, Any>>
}
