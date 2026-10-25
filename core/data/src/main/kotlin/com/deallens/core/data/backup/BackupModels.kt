package com.deallens.core.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupArchive(
    val version: Int,
    val exportedAt: String,
    val products: List<ProductBackup>,
    val listings: List<ListingBackup>,
    val priceObservations: List<PriceObservationBackup>,
    val vouchers: List<VoucherBackup>,
    val voucherVersions: List<VoucherVersionBackup>,
    val scenarios: List<ScenarioBackup>,
    val scenarioLines: List<ScenarioLineBackup>,
    val snapshots: List<SnapshotBackup>,
    val exchangeRates: List<ExchangeRateBackup>,
    val purchases: List<PurchaseBackup> = emptyList()
)

@Serializable
data class PurchaseBackup(
    val id: String,
    val timestamp: String,
    val itemsJson: String,
    val sellerOrPlatform: String,
    val payNowAmountMinorUnits: Long,
    val currency: String,
    val reportingCurrency: String,
    val immediateDiscountsMinorUnits: Long,
    val futureCashbackMinorUnits: Long,
    val receiptNote: String? = null,
    val receiptImageUri: String? = null,
    val snapshotId: String? = null
)

@Serializable
data class ProductBackup(
    val id: String,
    val name: String,
    val description: String?,
    val targetPriceMinorUnits: Long? = null,
    val targetCurrency: String? = null,
    val priority: String? = null,
    val isWishlist: Boolean = false,
    val createdAt: String
)

@Serializable
data class ListingBackup(
    val id: String,
    val productId: String,
    val platform: String,
    val sellerName: String,
    val url: String?,
    val condition: String?,
    val warranty: String?,
    val createdAt: String
)

@Serializable
data class PriceObservationBackup(
    val id: String,
    val listingId: String,
    val priceMinorUnits: Long,
    val currencyCode: String,
    val isCrossedOut: Boolean,
    val observedAt: String
)

@Serializable
data class VoucherBackup(
    val id: String,
    val name: String,
    val platform: String?,
    val currentVersion: Int,
    val expiresAt: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class VoucherVersionBackup(
    val voucherId: String,
    val version: Int,
    val type: String,
    val valueStr: String,
    val capMinorUnits: Long?,
    val capCurrency: String?,
    val minSpendMinorUnits: Long?,
    val minSpendCurrency: String?,
    val thresholdBasis: String,
    val createdAt: String
)

@Serializable
data class ScenarioBackup(
    val id: String,
    val name: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ScenarioLineBackup(
    val id: String,
    val scenarioId: String,
    val productId: String?,
    val listingId: String?,
    val quantity: Int,
    val addedAt: String
)

@Serializable
data class SnapshotBackup(
    val id: String,
    val scenarioId: String?,
    val inputJson: String,
    val outputJson: String,
    val calculatedAt: String
)

@Serializable
data class ExchangeRateBackup(
    val id: String,
    val fromCurrency: String,
    val toCurrency: String,
    val rateStr: String,
    val capturedAt: String
)
