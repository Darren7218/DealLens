package com.deallens.core.data.repository

import androidx.room.withTransaction
import com.deallens.core.data.backup.*
import com.deallens.core.data.local.DealLensDatabase
import com.deallens.core.data.local.entity.*
import com.deallens.core.domain.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject

class RoomBackupRestoreRepository @Inject constructor(
    private val db: DealLensDatabase
) : BackupRepository {
    
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        val backupDao = db.backupDao()
        
        val archive = BackupArchive(
            version = 1,
            exportedAt = Instant.now().toString(),
            products = backupDao.getAllProducts().map {
                ProductBackup(it.id, it.name, it.description, it.targetPriceMinorUnits, it.targetCurrency, it.priority, it.isWishlist, it.createdAt.toString())
            },
            listings = backupDao.getAllListings().map {
                ListingBackup(it.id, it.productId, it.platform, it.sellerName, it.url, it.condition, it.warranty, it.createdAt.toString())
            },
            priceObservations = backupDao.getAllPriceObservations().map {
                PriceObservationBackup(it.id, it.listingId, it.priceMinorUnits, it.currencyCode, it.isCrossedOut, it.observedAt.toString())
            },
            vouchers = backupDao.getAllVouchers().map {
                VoucherBackup(it.id, it.name, it.platform, it.currentVersion, it.expiresAt?.toString(), it.createdAt.toString(), it.updatedAt.toString())
            },
            voucherVersions = backupDao.getAllVoucherVersions().map {
                VoucherVersionBackup(it.voucherId, it.version, it.type, it.valueStr, it.capMinorUnits, it.capCurrency, it.minSpendMinorUnits, it.minSpendCurrency, it.thresholdBasis, it.createdAt.toString())
            },
            scenarios = backupDao.getAllScenarios().map {
                ScenarioBackup(it.id, it.name, it.createdAt.toString(), it.updatedAt.toString())
            },
            scenarioLines = backupDao.getAllScenarioLines().map {
                ScenarioLineBackup(it.id, it.scenarioId, it.productId, it.listingId, it.quantity, it.addedAt.toString())
            },
            snapshots = backupDao.getAllSnapshots().map {
                SnapshotBackup(it.id, it.scenarioId, it.inputJson, it.outputJson, it.calculatedAt.toString())
            },
            exchangeRates = backupDao.getAllExchangeRates().map {
                ExchangeRateBackup(it.id, it.fromCurrency, it.toCurrency, it.rateStr, it.capturedAt.toString())
            },
            purchases = backupDao.getAllPurchases().map {
                PurchaseBackup(
                    id = it.id,
                    timestamp = it.timestamp.toString(),
                    itemsJson = it.itemsJson,
                    sellerOrPlatform = it.sellerOrPlatform,
                    payNowAmountMinorUnits = it.payNowAmountMinorUnits,
                    currency = it.currency,
                    reportingCurrency = it.reportingCurrency,
                    immediateDiscountsMinorUnits = it.immediateDiscountsMinorUnits,
                    futureCashbackMinorUnits = it.futureCashbackMinorUnits,
                    receiptNote = it.receiptNote,
                    receiptImageUri = it.receiptImageUri,
                    snapshotId = it.snapshotId
                )
            }
        )
        
        jsonConfig.encodeToString(archive)
    }

    override suspend fun restoreBackup(json: String): Unit = withContext(Dispatchers.IO) {
        // First parse safely before touching DB
        val archive: BackupArchive = try {
            jsonConfig.decodeFromString(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid backup format or corrupted JSON", e)
        }
        
        if (archive.version > 1) {
            throw IllegalArgumentException("Unsupported backup version: ${archive.version}")
        }
        
        val backupDao = db.backupDao()
        
        db.withTransaction {
            // Wipe everything
            backupDao.clearPurchases()
            backupDao.clearExchangeRates()
            backupDao.clearSnapshots()
            backupDao.clearScenarioLines()
            backupDao.clearScenarios()
            backupDao.clearVoucherVersions()
            backupDao.clearVouchers()
            backupDao.clearPriceObservations()
            backupDao.clearListings()
            backupDao.clearProducts()
            
            // Insert everything
            backupDao.insertProducts(archive.products.map {
                ProductEntity(it.id, it.name, it.description, Instant.parse(it.createdAt), it.targetPriceMinorUnits, it.targetCurrency, it.priority, it.isWishlist)
            })
            backupDao.insertListings(archive.listings.map {
                ListingEntity(it.id, it.productId, it.platform, it.sellerName, it.url, it.condition, it.warranty, Instant.parse(it.createdAt))
            })
            backupDao.insertPriceObservations(archive.priceObservations.map {
                PriceObservationEntity(it.id, it.listingId, it.priceMinorUnits, it.currencyCode, it.isCrossedOut, Instant.parse(it.observedAt))
            })
            backupDao.insertVouchers(archive.vouchers.map {
                VoucherEntity(it.id, it.name, it.platform, it.currentVersion, it.expiresAt?.let { exp -> Instant.parse(exp) }, Instant.parse(it.createdAt), Instant.parse(it.updatedAt))
            })
            backupDao.insertVoucherVersions(archive.voucherVersions.map {
                VoucherRuleVersionEntity(it.voucherId, it.version, it.type, it.valueStr, it.capMinorUnits, it.capCurrency, it.minSpendMinorUnits, it.minSpendCurrency, it.thresholdBasis, Instant.parse(it.createdAt))
            })
            backupDao.insertScenarios(archive.scenarios.map {
                ScenarioEntity(it.id, it.name, Instant.parse(it.createdAt), Instant.parse(it.updatedAt))
            })
            backupDao.insertScenarioLines(archive.scenarioLines.map {
                ScenarioLineEntity(it.id, it.scenarioId, it.productId, it.listingId, it.quantity, Instant.parse(it.addedAt))
            })
            backupDao.insertSnapshots(archive.snapshots.map {
                CalculationSnapshotEntity(it.id, it.scenarioId, it.inputJson, it.outputJson, Instant.parse(it.calculatedAt))
            })
            backupDao.insertExchangeRates(archive.exchangeRates.map {
                ExchangeRateSnapshotEntity(it.id, it.fromCurrency, it.toCurrency, it.rateStr, Instant.parse(it.capturedAt))
            })
            backupDao.insertPurchases(archive.purchases.map {
                PurchaseEntity(
                    id = it.id,
                    timestamp = Instant.parse(it.timestamp),
                    itemsJson = it.itemsJson,
                    sellerOrPlatform = it.sellerOrPlatform,
                    payNowAmountMinorUnits = it.payNowAmountMinorUnits,
                    currency = it.currency,
                    reportingCurrency = it.reportingCurrency,
                    immediateDiscountsMinorUnits = it.immediateDiscountsMinorUnits,
                    futureCashbackMinorUnits = it.futureCashbackMinorUnits,
                    receiptNote = it.receiptNote,
                    receiptImageUri = it.receiptImageUri,
                    snapshotId = it.snapshotId
                )
            })
        }
    }
}
