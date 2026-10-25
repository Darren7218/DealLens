package com.deallens.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "vouchers")
data class VoucherEntity(
    @PrimaryKey val id: String,
    val name: String,
    val platform: String?,
    val currentVersion: Int,
    val expiresAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Entity(tableName = "voucher_versions", primaryKeys = ["voucherId", "version"])
data class VoucherRuleVersionEntity(
    val voucherId: String,
    val version: Int,
    val type: String, // PERCENTAGE, FIXED, REWARD
    val valueStr: String, // BigDecimal stored as String to preserve precision
    val capMinorUnits: Long?,
    val capCurrency: String?,
    val minSpendMinorUnits: Long?,
    val minSpendCurrency: String?,
    val thresholdBasis: String,
    val createdAt: Instant
)
