package com.aa.ledger.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aa.ledger.data.remote.CloudApi
import com.aa.ledger.data.remote.dto.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val baseUrl = "https://api.hotpot1993.top/"

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val api: CloudApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudApi::class.java)
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "aa_ledger_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("aa_ledger_auth", Context.MODE_PRIVATE)
        }
    }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        private set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var nickname: String?
        get() = prefs.getString(KEY_NICKNAME, null)
        private set(value) = prefs.edit().putString(KEY_NICKNAME, value).apply()

    var userId: Int
        get() = prefs.getInt(KEY_USER_ID, -1)
        private set(value) = prefs.edit().putInt(KEY_USER_ID, value).apply()

    val isLoggedIn: Boolean get() = token != null

    suspend fun login(nickname: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(nickname, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                token = body.token
                this.nickname = body.user.nickname
                userId = body.user.id
                Result.success(body)
            } else {
                val error = response.errorBody()?.string() ?: "登录失败"
                Result.failure(Exception(extractError(error)))
            }
        } catch (e: Exception) {
            Result.failure(Exception("网络错误：${e.message}"))
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    suspend fun getLedgers(): Result<LedgerListResponse> = apiCall {
        api.getLedgers("Bearer $token")
    }

    suspend fun createLedger(request: CreateLedgerRequest): Result<CreateLedgerResponse> = apiCall {
        api.createLedger("Bearer $token", request)
    }

    suspend fun getLedgerDetail(ledgerId: Int): Result<LedgerDetailResponse> = apiCall {
        api.getLedgerDetail("Bearer $token", ledgerId)
    }

    suspend fun joinLedger(inviteCode: String): Result<JoinLedgerResponse> = apiCall {
        api.joinLedger("Bearer $token", JoinLedgerRequest(inviteCode))
    }

    suspend fun clearMyLedgers(): Result<Int> = apiCall {
        api.clearMyLedgers("Bearer $token")
    }.map { (it["deleted"] as? Double)?.toInt() ?: 0 }

    suspend fun uploadBackup(zip: ByteArray, isFull: Boolean, baseRevision: Long): Result<Long> {
        return try {
            val body = zip.toRequestBody("application/zip".toMediaTypeOrNull())
            val mode = if (isFull) "full" else "delta"
            val response = api.uploadBackup("Bearer $token", mode, baseRevision, body)
            if (response.isSuccessful) {
                val revision = try {
                    com.google.gson.Gson().fromJson(response.body()?.string() ?: "{}", BackupUploadResponse::class.java).revision
                } catch (_: Exception) { 0L }
                Result.success(revision)
            } else if (response.code() == 409) {
                Result.failure(BackupConflictException())
            } else {
                Result.failure(Exception(response.errorBody()?.string()?.take(200) ?: "Unknown"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("网络错误：${e.message}"))
        }
    }

    suspend fun isAdmin(): Boolean {
        return try {
            val resp = api.checkAdmin("Bearer $token")
            resp.isSuccessful && (resp.body()?.get("isAdmin") as? Boolean ?: false)
        } catch (_: Exception) { false }
    }

    suspend fun getAdminUsers(): Result<List<Map<String, Any>>> {
        return try {
            val resp = api.getAdminUsers("Bearer $token")
            if (resp.isSuccessful) {
                val users = resp.body()?.get("users") as? List<*> ?: emptyList<Any>()
                @Suppress("UNCHECKED_CAST")
                Result.success(users.map { it as Map<String, Any> })
            } else Result.failure(Exception("获取失败"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteAdminUser(userId: Int): Result<String> {
        return try {
            val resp = api.deleteAdminUser("Bearer $token", userId)
            if (resp.isSuccessful) Result.success("已删除")
            else Result.failure(Exception(resp.errorBody()?.string()?.take(100) ?: "删除失败"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun downloadBackup(): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val response = api.downloadBackup("Bearer $token")
            if (response.isSuccessful) {
                val bytes = response.body()?.bytes() ?: ByteArray(0)
                android.util.Log.d("AuthRepository", "downloadBackup OK: ${bytes.size} bytes")
                Result.success(bytes)
            } else if (response.code() == 404) {
                Result.failure(Exception("云端暂无备份"))
            } else {
                val code = response.code()
                val errBody = response.errorBody()?.string()?.take(300) ?: ""
                Result.failure(Exception("HTTP $code: $errBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "downloadBackup error", e)
            Result.failure(Exception("网络: ${e.message}"))
        }
    }

    private suspend fun <T> apiCall(call: suspend () -> retrofit2.Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                val error = response.errorBody()?.string() ?: "请求失败"
                Result.failure(Exception(extractError(error)))
            }
        } catch (e: Exception) {
            Result.failure(Exception("网络错误：${e.message}"))
        }
    }

    private fun extractError(json: String): String {
        return try {
            com.google.gson.Gson().fromJson(json, ApiError::class.java).error
        } catch (e: Exception) { json.take(80) }
    }

    private data class BackupUploadResponse(val revision: Long = 0)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_USER_ID = "user_id"
    }
}

/** 增量上传时服务端 revision 不匹配（另一设备已更新），客户端应回退为全量上传 */
class BackupConflictException : Exception("服务端数据已更新，将回退为全量上传")
