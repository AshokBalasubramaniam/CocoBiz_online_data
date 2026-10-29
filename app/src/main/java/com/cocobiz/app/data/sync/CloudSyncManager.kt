package com.cocobiz.app.data.sync

import android.content.Context
import androidx.room.withTransaction
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.cocobiz.app.data.local.AuthPreferences
import com.cocobiz.app.data.local.database.AppDatabase
import com.cocobiz.app.data.local.entity.DealerEntity
import com.cocobiz.app.data.local.entity.SalesEntryEntity
import com.cocobiz.app.data.local.entity.SettingsEntity
import com.cocobiz.app.data.local.entity.UserProfileEntity
import com.cocobiz.app.data.remote.AppSettingsDto
import com.cocobiz.app.data.remote.CocoBizApi
import com.cocobiz.app.data.remote.DealerDto
import com.cocobiz.app.data.remote.FullBackupDto
import com.cocobiz.app.data.remote.SalesEntryDto
import com.cocobiz.app.data.remote.UserProfileDto
import com.cocobiz.app.util.BackupManager
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the account's data on the CocoBiz server. The server copy is the
 * source of truth; Room is only an offline cache so the UI keeps working
 * without network. Every local write calls [onLocalChange], which uploads the
 * whole account (dealers, sales, profile, settings) via [CloudSyncWorker] as
 * soon as the device is online.
 */
@Singleton
class CloudSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: CocoBizApi,
    private val db: AppDatabase,
    private val authPrefs: AuthPreferences,
    private val backupManager: Lazy<BackupManager>
) {
    private val mutex = Mutex()

    fun onLocalChange() {
        synchronized(this) { authPrefs.localVersion = authPrefs.localVersion + 1 }
        enqueuePush()
    }

    private fun enqueuePush() {
        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(2, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(CloudSyncWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Uploads everything in the local cache to the server. */
    suspend fun push() = mutex.withLock { pushLocked() }

    /**
     * Pulls the latest server copy (e.g. edits made on another phone) into the
     * local cache. If this device has changes the server hasn't seen yet,
     * those are uploaded instead so they are never overwritten.
     */
    suspend fun refresh() = mutex.withLock {
        if (authPrefs.hasUnsyncedChanges) {
            pushLocked()
            return@withLock
        }
        val versionBefore = authPrefs.localVersion
        val remote = fetchRemote()
        if (authPrefs.localVersion != versionBefore) return@withLock // edited while downloading
        applyRemote(remote, sameAccount = true)
    }

    /** Decides whether the server's or this phone's data wins right after signing in. */
    suspend fun reconcileAfterLogin(username: String) = mutex.withLock {
        val sameAccount = authPrefs.syncedUsername.equals(username, ignoreCase = true)
        if (sameAccount && authPrefs.hasUnsyncedChanges) {
            pushLocked()
            return@withLock
        }

        val remote = fetchRemote()
        val localHasData = withContext(Dispatchers.IO) {
            db.dealerDao().getDealerCount() > 0 || db.salesEntryDao().getAllSync().isNotEmpty()
        }
        val localNeverSynced = authPrefs.syncedUsername.isBlank()
        val remoteEmpty = remote.dealers.isEmpty() && remote.sales.isEmpty()

        if (localHasData && remoteEmpty && (sameAccount || localNeverSynced)) {
            // First sign-in after upgrading from the offline-only app:
            // upload what is already on this phone instead of wiping it.
            if (db.userProfileDao().getProfileSync() == null) {
                remote.profile?.let { db.userProfileDao().insertOrUpdate(it.toEntity(logoPath = "")) }
            }
            authPrefs.syncedUsername = username
            pushLocked()
            return@withLock
        }

        if (localHasData && localNeverSynced) {
            // Phone data that was never uploaded is about to be replaced by the
            // server copy — keep a .cocobak in Downloads/CocoBiz just in case.
            backupManager.get().backup()
        }
        applyRemote(remote, sameAccount)
        authPrefs.syncedUsername = username
    }

    /**
     * Uploads any pending changes, then wipes this account's data from the
     * phone. Returns false (and keeps everything) if pending changes could not
     * be uploaded, so nothing is lost by signing out offline.
     */
    suspend fun signOut(): Boolean = mutex.withLock {
        if (authPrefs.hasUnsyncedChanges) {
            if (!authPrefs.hasValidSession) return@withLock false
            try {
                pushLocked()
            } catch (e: Exception) {
                return@withLock false
            }
        }
        WorkManager.getInstance(context).cancelUniqueWork(CloudSyncWorker.WORK_NAME)
        withContext(Dispatchers.IO) { db.clearAllTables() }
        authPrefs.clear()
        true
    }

    private suspend fun pushLocked() {
        val version = authPrefs.localVersion
        val bundle = withContext(Dispatchers.IO) { buildBundle() }
        api.putSync(bundle)
        bundle.profile?.let { api.putProfile(it) }
        if (authPrefs.pushedVersion < version) authPrefs.pushedVersion = version
    }

    private suspend fun fetchRemote(): FullBackupDto {
        val bundle = try {
            api.getSync()
        } catch (e: HttpException) {
            if (e.code() == 404) FullBackupDto() else throw e
        }
        // A brand-new account only has the profile entered at registration.
        if (bundle.profile != null) return bundle
        val profile = try {
            api.getProfile()
        } catch (e: HttpException) {
            if (e.code() == 404) null else throw e
        }
        return bundle.copy(profile = profile)
    }

    private suspend fun applyRemote(remote: FullBackupDto, sameAccount: Boolean) {
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val dealerDao = db.dealerDao()
                val salesDao = db.salesEntryDao()
                val profileDao = db.userProfileDao()

                // Photo/logo paths point at files on this phone, so they are not
                // stored online; keep them when refreshing the same account.
                val photoPaths = if (sameAccount) {
                    dealerDao.getAllSync().associate { it.id to it.photoPath }
                } else emptyMap()
                val logoPath = if (sameAccount) profileDao.getProfileSync()?.logoPath.orEmpty() else ""

                salesDao.deleteAll()
                dealerDao.deleteAll()
                dealerDao.insertAll(remote.dealers.map { it.toEntity(photoPaths[it.id].orEmpty()) })
                salesDao.insertAll(remote.sales.map { it.toEntity() })

                profileDao.deleteAll()
                remote.profile?.let { profileDao.insertOrUpdate(it.toEntity(logoPath)) }

                val settings = remote.settings
                if (settings != null) {
                    db.settingsDao().insertOrUpdate(settings.toEntity())
                    authPrefs.reminderChannel = settings.reminderChannel
                    authPrefs.reminderFrequency = settings.reminderFrequency
                } else if (!sameAccount) {
                    db.settingsDao().insertOrUpdate(SettingsEntity())
                }
            }
        }
    }

    private suspend fun buildBundle(): FullBackupDto = FullBackupDto(
        dealers = db.dealerDao().getAllSync().map { it.toDto() },
        sales = db.salesEntryDao().getAllSync().map { it.toDto() },
        profile = db.userProfileDao().getProfileSync()?.toDto(),
        settings = (db.settingsDao().getSettingsSync() ?: SettingsEntity()).toDto(
            reminderChannel = authPrefs.reminderChannel,
            reminderFrequency = authPrefs.reminderFrequency
        )
    )

    private fun DealerEntity.toDto() = DealerDto(
        id = id, dealerName = dealerName, place = place, phone = phone,
        alternatePhone = alternatePhone, email = email, address = address,
        city = city, state = state, pincode = pincode, notes = notes,
        isActive = isActive, createdAt = createdAt, updatedAt = updatedAt
    )

    private fun DealerDto.toEntity(photoPath: String) = DealerEntity(
        id = id, dealerName = dealerName, place = place, phone = phone,
        alternatePhone = alternatePhone, email = email, address = address,
        city = city, state = state, pincode = pincode, notes = notes,
        photoPath = photoPath, isActive = isActive,
        createdAt = createdAt, updatedAt = updatedAt
    )

    private fun SalesEntryEntity.toDto() = SalesEntryDto(
        id = id, dealerId = dealerId, dealerName = dealerName, dealerPlace = dealerPlace,
        salesDate = salesDate, nextSalesDate = nextSalesDate, quantity = quantity,
        rate = rate, marketRate = marketRate, totalAmount = totalAmount,
        coconutType = coconutType, cycleDays = cycleDays, status = status, notes = notes,
        createdAt = createdAt, updatedAt = updatedAt
    )

    private fun SalesEntryDto.toEntity() = SalesEntryEntity(
        id = id, dealerId = dealerId, dealerName = dealerName, dealerPlace = dealerPlace,
        salesDate = salesDate, nextSalesDate = nextSalesDate, quantity = quantity,
        rate = rate, marketRate = marketRate, totalAmount = totalAmount,
        coconutType = coconutType, cycleDays = cycleDays, status = status, notes = notes,
        createdAt = createdAt, updatedAt = updatedAt
    )

    private fun UserProfileEntity.toDto() = UserProfileDto(
        businessName = businessName, ownerName = ownerName, phone = phone,
        alternatePhone = alternatePhone, email = email, address = address,
        city = city, state = state, pincode = pincode, gstNumber = gstNumber,
        createdAt = createdAt, updatedAt = updatedAt
    )

    private fun UserProfileDto.toEntity(logoPath: String) = UserProfileEntity(
        businessName = businessName, ownerName = ownerName, phone = phone,
        alternatePhone = alternatePhone, email = email, address = address,
        city = city, state = state, pincode = pincode, gstNumber = gstNumber,
        logoPath = logoPath, createdAt = createdAt, updatedAt = updatedAt
    )

    private fun SettingsEntity.toDto(reminderChannel: String, reminderFrequency: String) = AppSettingsDto(
        reminderDays = reminderDays, notificationEnabled = notificationEnabled,
        emailEnabled = emailEnabled, senderEmail = senderEmail, darkMode = darkMode,
        backupEnabled = backupEnabled, defaultCycleDays = defaultCycleDays,
        editCompletedEnabled = editCompletedEnabled,
        reminderChannel = reminderChannel, reminderFrequency = reminderFrequency
    )

    private fun AppSettingsDto.toEntity() = SettingsEntity(
        reminderDays = reminderDays, notificationEnabled = notificationEnabled,
        emailEnabled = emailEnabled, senderEmail = senderEmail, darkMode = darkMode,
        backupEnabled = backupEnabled, defaultCycleDays = defaultCycleDays,
        editCompletedEnabled = editCompletedEnabled
    )
}
