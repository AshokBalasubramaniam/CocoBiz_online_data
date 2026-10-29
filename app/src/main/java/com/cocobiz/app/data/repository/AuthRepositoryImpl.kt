package com.cocobiz.app.data.repository

import com.cocobiz.app.data.local.AuthPreferences
import com.cocobiz.app.data.local.dao.UserProfileDao
import com.cocobiz.app.data.remote.CocoBizApi
import com.cocobiz.app.data.remote.LoginRequest
import com.cocobiz.app.data.remote.RegisterRequest
import com.cocobiz.app.data.remote.ResetPasswordRequest
import com.cocobiz.app.data.sync.CloudSyncManager
import com.cocobiz.app.domain.repository.AuthRepository
import com.cocobiz.app.domain.repository.LocalUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: CocoBizApi,
    private val authPrefs: AuthPreferences,
    private val userProfileDao: UserProfileDao,
    private val cloudSync: CloudSyncManager
) : AuthRepository {

    override suspend fun register(
        username: String,
        password: String,
        email: String,
        phone: String,
        businessName: String,
        ownerName: String
    ): LocalUser {
        val response = api.register(
            RegisterRequest(
                username = username,
                password = password,
                email = email,
                phone = phone,
                businessName = businessName,
                ownerName = ownerName
            )
        )
        return LocalUser(
            username = response.username, email = email, phone = phone,
            businessName = businessName, ownerName = ownerName
        )
    }

    override suspend fun login(username: String, password: String): LocalUser {
        val response = api.login(LoginRequest(username = username, password = password))
        authPrefs.token = response.token
        authPrefs.tokenExpiresAt = System.currentTimeMillis() + response.expiresIn * 1000
        try {
            cloudSync.reconcileAfterLogin(response.username)
        } catch (e: Exception) {
            authPrefs.endSession()
            throw e
        }
        val profile = userProfileDao.getProfileSync()
        return LocalUser(
            username = response.username,
            email = profile?.email.orEmpty(),
            phone = profile?.phone.orEmpty(),
            businessName = profile?.businessName.orEmpty(),
            ownerName = profile?.ownerName.orEmpty(),
            reminderChannel = authPrefs.reminderChannel,
            reminderFrequency = authPrefs.reminderFrequency
        )
    }

    override suspend fun resetPassword(username: String, phone: String, newPassword: String) {
        api.resetPassword(
            ResetPasswordRequest(username = username, phone = phone, newPassword = newPassword)
        )
    }

    override suspend fun updateReminderPrefs(channel: String, frequency: String) {
        authPrefs.reminderChannel = channel
        authPrefs.reminderFrequency = frequency
        cloudSync.onLocalChange()
    }
}
