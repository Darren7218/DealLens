package com.deallens.core.domain.repository

import com.deallens.core.domain.Money
import com.deallens.core.domain.Priority
import com.deallens.core.domain.Product
import com.deallens.core.domain.ProductHistory
import com.deallens.core.domain.QuoteRequest
import com.deallens.core.domain.QuoteResult
import com.deallens.core.domain.VoucherRuleVersion
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class VoucherProfile(
    val id: String,
    val name: String,
    val platform: String?,
    val currentVersion: Int,
    val expiresAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val latestRule: VoucherRuleVersion?
)

interface VoucherRepository {
    fun getAllVouchers(): Flow<List<VoucherProfile>>
    suspend fun getVoucherById(id: String): VoucherProfile?
    suspend fun getVouchersExpiringSoon(now: Instant, withinHours: Long = 48): List<VoucherProfile>
    suspend fun saveVoucher(profile: VoucherProfile, rule: VoucherRuleVersion)
    suspend fun deleteVoucher(id: String)
}

interface ProductRepository {
    fun getAllProducts(): Flow<List<Product>>
    fun getWishlistProducts(): Flow<List<Product>>
    fun getProductHistory(productId: String): Flow<ProductHistory?>
    suspend fun saveObservation(
        productName: String,
        platform: String,
        price: Money,
        targetPrice: Money? = null,
        priority: Priority? = null,
        isWishlist: Boolean = false
    )
    suspend fun saveWishlistItem(
        productName: String,
        targetPrice: Money,
        priority: Priority
    )
}

data class SavedSnapshot(
    val id: String,
    val scenarioId: String?,
    val request: QuoteRequest,
    val result: QuoteResult,
    val calculatedAt: Instant
)

interface SnapshotRepository {
    fun getAllSnapshots(): Flow<List<SavedSnapshot>>
    suspend fun saveSnapshot(snapshot: SavedSnapshot)
}

interface BackupRepository {
    suspend fun exportBackup(): String
    suspend fun restoreBackup(json: String)
}

interface PurchaseRepository {
    fun getAllPurchases(): Flow<List<com.deallens.core.domain.ConfirmedPurchase>>
    suspend fun getPurchaseById(id: String): com.deallens.core.domain.ConfirmedPurchase?
    suspend fun savePurchase(purchase: com.deallens.core.domain.ConfirmedPurchase)
    suspend fun deletePurchase(purchaseId: String)
}
