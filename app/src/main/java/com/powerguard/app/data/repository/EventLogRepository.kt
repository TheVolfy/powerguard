package com.powerguard.app.data.repository

import com.powerguard.app.data.db.AppDatabase
import com.powerguard.app.data.db.EventLogEntity
import com.powerguard.app.domain.model.EventLogItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventLogRepository(db: AppDatabase) {

    private val dao = db.eventLogDao()

    fun observeAll(): Flow<List<EventLogItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun getRecent(limit: Int = 10): List<EventLogItem> =
        dao.getRecent(limit).map { it.toDomain() }

    suspend fun insert(entity: EventLogEntity) = dao.insert(entity)

    suspend fun pruneOlderThan(days: Long) {
        val cutoff = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        dao.deleteOlderThan(cutoff)
    }

    suspend fun clearAll() = dao.deleteAll()
}
