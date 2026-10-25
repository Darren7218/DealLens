package com.deallens.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "scenarios")
data class ScenarioEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Entity(tableName = "scenario_lines")
data class ScenarioLineEntity(
    @PrimaryKey val id: String,
    val scenarioId: String,
    val productId: String?,
    val listingId: String?,
    val quantity: Int,
    val addedAt: Instant
)

@Entity(tableName = "calculation_snapshots")
data class CalculationSnapshotEntity(
    @PrimaryKey val id: String,
    val scenarioId: String?, 
    val inputJson: String, // Serialized QuoteRequest 
    val outputJson: String, // Serialized QuoteResult
    val calculatedAt: Instant
)

@Entity(tableName = "exchange_rate_snapshots")
data class ExchangeRateSnapshotEntity(
    @PrimaryKey val id: String,
    val fromCurrency: String,
    val toCurrency: String,
    val rateStr: String,
    val capturedAt: Instant
)
