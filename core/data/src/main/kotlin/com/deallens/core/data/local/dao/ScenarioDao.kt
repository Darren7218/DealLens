package com.deallens.core.data.local.dao

import androidx.room.*
import com.deallens.core.data.local.entity.CalculationSnapshotEntity
import com.deallens.core.data.local.entity.ScenarioEntity
import com.deallens.core.data.local.entity.ScenarioLineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenarioDao {
    @Query("SELECT * FROM scenarios ORDER BY updatedAt DESC")
    fun getAllScenarios(): Flow<List<ScenarioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenario(scenario: ScenarioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenarioLines(lines: List<ScenarioLineEntity>)
    
    @Query("SELECT * FROM scenario_lines WHERE scenarioId = :scenarioId")
    suspend fun getLinesForScenario(scenarioId: String): List<ScenarioLineEntity>
}

@Dao
interface SnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculationSnapshot(snapshot: CalculationSnapshotEntity)

    @Query("SELECT * FROM calculation_snapshots ORDER BY calculatedAt DESC")
    fun getAllSnapshots(): Flow<List<CalculationSnapshotEntity>>
}
