package com.deallens.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deallens.core.data.export.MoneyLensExporter
import com.deallens.core.domain.*
import com.deallens.core.domain.repository.BackupRepository
import com.deallens.core.domain.repository.ProductRepository
import com.deallens.core.domain.repository.PurchaseRepository
import com.deallens.core.domain.repository.SavedSnapshot
import com.deallens.core.domain.repository.SnapshotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SavedTab(val label: String) {
    SNAPSHOTS("Snapshots"),
    PURCHASES("Purchases"),
    PRICE_HISTORY("Price History"),
    WISHLIST("Wishlist")
}

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val snapshotRepository: SnapshotRepository,
    private val backupRepository: BackupRepository,
    private val productRepository: ProductRepository,
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    val snapshots = snapshotRepository.getAllSnapshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases = purchaseRepository.getAllPurchases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products = productRepository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wishlist = productRepository.getWishlistProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _selectedTab = MutableStateFlow(SavedTab.SNAPSHOTS)
    val selectedTab: StateFlow<SavedTab> = _selectedTab

    fun selectTab(tab: SavedTab) {
        _selectedTab.value = tab
    }

    fun getProductHistory(productId: String) = productRepository.getProductHistory(productId)

    fun confirmPurchaseFromSnapshot(
        snapshot: SavedSnapshot,
        sellerOrPlatform: String = "Online Checkout",
        note: String? = null,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            val purchase = MoneyLensExporter.snapshotToConfirmedPurchase(
                snapshot = snapshot,
                sellerOrPlatform = sellerOrPlatform,
                receiptNote = note,
                receiptImageUri = imageUri
            )
            purchaseRepository.savePurchase(purchase)
            _message.value = "Confirmed purchase saved!"
        }
    }

    fun saveDirectPurchase(purchase: ConfirmedPurchase) {
        viewModelScope.launch {
            purchaseRepository.savePurchase(purchase)
            _message.value = "Confirmed purchase saved!"
        }
    }

    fun generateMoneyLensExportJson(purchase: ConfirmedPurchase): String {
        val export = MoneyLensExporter.toExpenseExport(purchase)
        return MoneyLensExporter.serializeJson(export)
    }

    fun generateMoneyLensExportJsonFromSnapshot(snapshot: SavedSnapshot, platform: String = "Online Order"): String {
        val purchase = MoneyLensExporter.snapshotToConfirmedPurchase(snapshot, sellerOrPlatform = platform)
        val export = MoneyLensExporter.toExpenseExport(purchase)
        return MoneyLensExporter.serializeJson(export)
    }

    fun generatePurchasesCsvExport(): String {
        return MoneyLensExporter.exportPurchasesToCsv(purchases.value)
    }

    fun addWishlistItem(name: String, targetAmountMinor: Long, currency: CurrencyCode, priority: Priority) {
        viewModelScope.launch {
            productRepository.saveWishlistItem(
                productName = name,
                targetPrice = Money(targetAmountMinor, currency),
                priority = priority
            )
            _message.value = "Added $name to Wishlist!"
        }
    }

    fun recordObservation(name: String, platform: String, amountMinor: Long, currency: CurrencyCode) {
        viewModelScope.launch {
            productRepository.saveObservation(
                productName = name,
                platform = platform,
                price = Money(amountMinor, currency)
            )
            _message.value = "Recorded price for $name"
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun exportData(onDataReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = backupRepository.exportBackup()
                onDataReady(json)
            } catch (e: Exception) {
                _message.value = "Export failed: ${e.message}"
            }
        }
    }

    fun restoreData(json: String) {
        viewModelScope.launch {
            try {
                backupRepository.restoreBackup(json)
                _message.value = "Restore successful!"
            } catch (e: Exception) {
                _message.value = "Restore failed: ${e.message}"
            }
        }
    }
}
