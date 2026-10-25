package com.deallens.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deallens.core.data.local.entity.*

@Dao
interface BackupDao {
    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM listings")
    suspend fun getAllListings(): List<ListingEntity>

    @Query("SELECT * FROM price_observations")
    suspend fun getAllPriceObservations(): List<PriceObservationEntity>

    @Query("SELECT * FROM vouchers")
    suspend fun getAllVouchers(): List<VoucherEntity>

    @Query("SELECT * FROM voucher_versions")
    suspend fun getAllVoucherVersions(): List<VoucherRuleVersionEntity>

    @Query("SELECT * FROM scenarios")
    suspend fun getAllScenarios(): List<ScenarioEntity>

    @Query("SELECT * FROM scenario_lines")
    suspend fun getAllScenarioLines(): List<ScenarioLineEntity>

    @Query("SELECT * FROM calculation_snapshots")
    suspend fun getAllSnapshots(): List<CalculationSnapshotEntity>

    @Query("SELECT * FROM exchange_rate_snapshots")
    suspend fun getAllExchangeRates(): List<ExchangeRateSnapshotEntity>

    @Query("SELECT * FROM confirmed_purchases")
    suspend fun getAllPurchases(): List<PurchaseEntity>

    // Wiping the database
    @Query("DELETE FROM products")
    suspend fun clearProducts()
    @Query("DELETE FROM listings")
    suspend fun clearListings()
    @Query("DELETE FROM price_observations")
    suspend fun clearPriceObservations()
    @Query("DELETE FROM vouchers")
    suspend fun clearVouchers()
    @Query("DELETE FROM voucher_versions")
    suspend fun clearVoucherVersions()
    @Query("DELETE FROM scenarios")
    suspend fun clearScenarios()
    @Query("DELETE FROM scenario_lines")
    suspend fun clearScenarioLines()
    @Query("DELETE FROM calculation_snapshots")
    suspend fun clearSnapshots()
    @Query("DELETE FROM exchange_rate_snapshots")
    suspend fun clearExchangeRates()
    @Query("DELETE FROM confirmed_purchases")
    suspend fun clearPurchases()

    // Inserts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(items: List<ProductEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListings(items: List<ListingEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceObservations(items: List<PriceObservationEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVouchers(items: List<VoucherEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucherVersions(items: List<VoucherRuleVersionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenarios(items: List<ScenarioEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenarioLines(items: List<ScenarioLineEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshots(items: List<CalculationSnapshotEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRates(items: List<ExchangeRateSnapshotEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchases(items: List<PurchaseEntity>)
}
