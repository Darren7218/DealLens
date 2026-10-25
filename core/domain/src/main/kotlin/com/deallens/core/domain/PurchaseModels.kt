package com.deallens.core.domain

import java.time.Instant

data class PurchaseItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val unitPrice: Money,
    val quantity: Int = 1
)

data class ConfirmedPurchase(
    val purchaseId: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Instant = Instant.now(),
    val items: List<PurchaseItem>,
    val sellerOrPlatform: String,
    val payNowAmount: Money,
    val originalCurrency: CurrencyCode,
    val reportingCurrency: CurrencyCode = originalCurrency,
    val immediateDiscountsTotal: Money = Money(0, originalCurrency),
    val futureCashback: Money = Money(0, originalCurrency),
    val receiptNote: String? = null,
    val receiptImageUri: String? = null,
    val snapshotId: String? = null
)
