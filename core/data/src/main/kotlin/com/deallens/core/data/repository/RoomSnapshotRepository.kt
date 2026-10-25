package com.deallens.core.data.repository

import com.deallens.core.data.local.dao.SnapshotDao
import com.deallens.core.data.local.entity.CalculationSnapshotEntity
import com.deallens.core.data.mapping.QuoteRequestDto
import com.deallens.core.data.mapping.QuoteResultDto
import com.deallens.core.data.mapping.toDomain
import com.deallens.core.data.mapping.toDto
import com.deallens.core.domain.repository.SavedSnapshot
import com.deallens.core.domain.repository.SnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class RoomSnapshotRepository @Inject constructor(
    private val snapshotDao: SnapshotDao
) : SnapshotRepository {

    override fun getAllSnapshots(): Flow<List<SavedSnapshot>> {
        return snapshotDao.getAllSnapshots().map { entities ->
            entities.map { entity ->
                val requestDto = Json.decodeFromString<QuoteRequestDto>(entity.inputJson)
                val resultDto = Json.decodeFromString<QuoteResultDto>(entity.outputJson)
                SavedSnapshot(
                    id = entity.id,
                    scenarioId = entity.scenarioId,
                    request = requestDto.toDomain(),
                    result = resultDto.toDomain(),
                    calculatedAt = entity.calculatedAt
                )
            }
        }
    }

    override suspend fun saveSnapshot(snapshot: SavedSnapshot) {
        val requestDto = snapshot.request.toDto()
        val resultDto = snapshot.result.toDto()
        val entity = CalculationSnapshotEntity(
            id = snapshot.id,
            scenarioId = snapshot.scenarioId,
            inputJson = Json.encodeToString(requestDto),
            outputJson = Json.encodeToString(resultDto),
            calculatedAt = snapshot.calculatedAt
        )
        snapshotDao.insertCalculationSnapshot(entity)
    }
}
