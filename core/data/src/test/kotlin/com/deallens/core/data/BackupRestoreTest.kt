package com.deallens.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.deallens.core.data.local.DealLensDatabase
import com.deallens.core.data.local.entity.ProductEntity
import com.deallens.core.data.local.entity.PurchaseEntity
import com.deallens.core.data.repository.RoomBackupRestoreRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRestoreTest {
    private lateinit var db: DealLensDatabase
    private lateinit var backupRepo: RoomBackupRestoreRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DealLensDatabase::class.java
        ).allowMainThreadQueries().build()
        backupRepo = RoomBackupRestoreRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `successful export, wipe, restore roundtrip`() = runBlocking {
        // 1. Insert some initial data
        val product = ProductEntity("p1", "Test Product", "Desc", Instant.now())
        val purchase = PurchaseEntity(
            id = "pur1",
            timestamp = Instant.parse("2026-10-10T10:00:00Z"),
            itemsJson = "[]",
            sellerOrPlatform = "Shopee",
            payNowAmountMinorUnits = 9900L,
            currency = "MYR",
            reportingCurrency = "MYR",
            immediateDiscountsMinorUnits = 1000L,
            futureCashbackMinorUnits = 0L,
            receiptNote = "Test Purchase",
            receiptImageUri = null,
            snapshotId = null
        )
        db.backupDao().insertProducts(listOf(product))
        db.backupDao().insertPurchases(listOf(purchase))

        // Verify product and purchase exist
        val initialProducts = db.backupDao().getAllProducts()
        assertEquals(1, initialProducts.size)
        assertEquals("Test Product", initialProducts[0].name)
        assertEquals(1, db.backupDao().getAllPurchases().size)

        // 2. Export backup
        val json = backupRepo.exportBackup()
        assertTrue(json.contains("Test Product"))
        assertTrue(json.contains("Test Purchase"))
        assertTrue(json.contains("\"version\":1"))

        // 3. Wipe database manually
        db.backupDao().clearProducts()
        db.backupDao().clearPurchases()
        assertEquals(0, db.backupDao().getAllProducts().size)
        assertEquals(0, db.backupDao().getAllPurchases().size)

        // 4. Restore backup
        backupRepo.restoreBackup(json)

        // 5. Verify product and purchase are back
        val restoredProducts = db.backupDao().getAllProducts()
        assertEquals(1, restoredProducts.size)
        assertEquals("Test Product", restoredProducts[0].name)
        val restoredPurchases = db.backupDao().getAllPurchases()
        assertEquals(1, restoredPurchases.size)
        assertEquals("Test Purchase", restoredPurchases[0].receiptNote)
    }

    @Test
    fun `malformed JSON restore fails safely without corrupting DB`() = runBlocking {
        // Insert initial data
        val product = ProductEntity("p2", "Safe Product", "Desc", Instant.now())
        db.backupDao().insertProducts(listOf(product))

        // Attempt restore with bad JSON
        val badJson = "{ \"version\": 1, \"products\": [ { broken "
        
        try {
            backupRepo.restoreBackup(badJson)
            fail("Expected exception was not thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Invalid backup format") == true)
        }

        // Verify database is untouched
        val safeProducts = db.backupDao().getAllProducts()
        assertEquals(1, safeProducts.size)
        assertEquals("Safe Product", safeProducts[0].name)
    }

    @Test
    fun `unsupported version fails safely`() = runBlocking {
        // Attempt restore with future version
        val badVersionJson = """{"version": 999, "exportedAt": "2026-01-01T00:00:00Z", "products": [], "listings": [], "priceObservations": [], "vouchers": [], "voucherVersions": [], "scenarios": [], "scenarioLines": [], "snapshots": [], "exchangeRates": []}"""
        
        try {
            backupRepo.restoreBackup(badVersionJson)
            fail("Expected exception was not thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Unsupported backup version") == true)
        }
    }
}
