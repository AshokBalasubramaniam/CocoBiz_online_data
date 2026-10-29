package com.cocobiz.app.data.repository

import com.cocobiz.app.data.local.dao.DealerDao
import com.cocobiz.app.data.local.entity.DealerEntity
import com.cocobiz.app.data.sync.CloudSyncManager
import com.cocobiz.app.domain.model.Dealer
import com.cocobiz.app.domain.repository.DealerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DealerRepositoryImpl @Inject constructor(
    private val dao: DealerDao,
    private val cloudSync: CloudSyncManager
) : DealerRepository {

    override fun getAllDealers(): Flow<List<Dealer>> =
        dao.getAllDealers().map { list -> list.map { it.toDomain() } }

    override fun getDealerById(id: Long): Flow<Dealer?> =
        dao.getDealerById(id).map { it?.toDomain() }

    override fun searchDealers(query: String): Flow<List<Dealer>> =
        dao.searchDealers(query).map { list -> list.map { it.toDomain() } }

    override suspend fun addDealer(dealer: Dealer): Long =
        dao.insert(dealer.toEntity()).also { cloudSync.onLocalChange() }

    override suspend fun updateDealer(dealer: Dealer) {
        dao.update(dealer.copy(updatedAt = System.currentTimeMillis()).toEntity())
        cloudSync.onLocalChange()
    }

    override suspend fun deleteDealer(id: Long) {
        dao.deleteById(id)
        cloudSync.onLocalChange()
    }

    override suspend fun getDealerCount(): Int = dao.getDealerCount()

    private fun DealerEntity.toDomain(): Dealer = Dealer(
        id = id, dealerName = dealerName, place = place,
        phone = phone, alternatePhone = alternatePhone, email = email,
        address = address, city = city, state = state, pincode = pincode,
        notes = notes, photoPath = photoPath,
        isActive = isActive,
        createdAt = createdAt, updatedAt = updatedAt
    )

    private fun Dealer.toEntity(): DealerEntity = DealerEntity(
        id = id, dealerName = dealerName, place = place,
        phone = phone, alternatePhone = alternatePhone, email = email,
        address = address, city = city, state = state, pincode = pincode,
        notes = notes, photoPath = photoPath,
        isActive = isActive,
        createdAt = createdAt, updatedAt = updatedAt
    )
}
