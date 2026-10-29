package com.cocobiz.app.data.remote

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * CocoBiz backend (backend/src/routes.rs). Field names are converted to the
 * server's snake_case by the Gson naming policy in NetworkModule.
 */
interface CocoBizApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): JsonObject

    @GET("api/v1/profile")
    suspend fun getProfile(): UserProfileDto

    @PUT("api/v1/profile")
    suspend fun putProfile(@Body body: UserProfileDto): UserProfileDto

    @GET("api/v1/sync")
    suspend fun getSync(): FullBackupDto

    @PUT("api/v1/sync")
    suspend fun putSync(@Body body: FullBackupDto): FullBackupDto
}

// Every property has a default so Gson uses the no-arg constructor and a
// field missing from the server's JSON falls back to a sane value, not null.

data class RegisterRequest(
    val username: String = "",
    val password: String = "",
    val email: String = "",
    val phone: String = "",
    val businessName: String = "",
    val ownerName: String = ""
)

data class LoginRequest(
    val username: String = "",
    val password: String = ""
)

data class ResetPasswordRequest(
    val username: String = "",
    val phone: String = "",
    val newPassword: String = ""
)

data class AuthResponse(
    val token: String = "",
    val tokenType: String = "Bearer",
    val expiresIn: Long = 0L,
    val username: String = ""
)

data class UserProfileDto(
    val businessName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val alternatePhone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val gstNumber: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class DealerDto(
    val id: Long = 0L,
    val dealerName: String = "",
    val place: String = "",
    val phone: String = "",
    val alternatePhone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class SalesEntryDto(
    val id: Long = 0L,
    val dealerId: Long = 0L,
    val dealerName: String = "",
    val dealerPlace: String = "",
    val salesDate: Long = 0L,
    val nextSalesDate: Long = 0L,
    val quantity: Double = 0.0,
    val rate: Double = 0.0,
    val marketRate: Double = 0.0,
    val totalAmount: Double = 0.0,
    val coconutType: String = "",
    val cycleDays: Int = 60,
    val status: String = "ACTIVE",
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class AppSettingsDto(
    val reminderDays: Int = 5,
    val notificationEnabled: Boolean = true,
    val emailEnabled: Boolean = false,
    val senderEmail: String = "",
    val darkMode: String = "SYSTEM",
    val backupEnabled: Boolean = false,
    val defaultCycleDays: Int = 60,
    val editCompletedEnabled: Boolean = false,
    val reminderChannel: String = "EMAIL",
    val reminderFrequency: String = "DAILY"
)

data class FullBackupDto(
    val dealers: List<DealerDto> = emptyList(),
    val sales: List<SalesEntryDto> = emptyList(),
    val profile: UserProfileDto? = null,
    val settings: AppSettingsDto? = null
)
