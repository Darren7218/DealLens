package com.deallens.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deallens.core.data.local.DealLensDatabase
import com.deallens.core.data.repository.RoomSnapshotRepository
import com.deallens.core.data.repository.RoomVoucherRepository
import com.deallens.core.domain.*
import com.deallens.core.domain.repository.SavedSnapshot
import com.deallens.core.domain.repository.VoucherProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class VoucherImmutabilityTest {


    private lateinit var db: DealLensDatabase
    private lateinit var voucherRepo: RoomVoucherRepository
    private lateinit var snapshotRepo: RoomSnapshotRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DealLensDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        voucherRepo = RoomVoucherRepository(db.voucherDao())
        snapshotRepo = RoomSnapshotRepository(db.snapshotDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `updating voucher creates new version and preserves old snapshot`() = runBlocking {
        // 1. Create a Voucher (Version 1)
        val profile = VoucherProfile(
            id = "v123",
            name = "Shopee 22%",
            platform = "Shopee",
            currentVersion = 1,
            createdAt = Instant.parse("2026-10-09T10:00:00Z"),
            updatedAt = Instant.parse("2026-10-09T10:00:00Z"),
            latestRule = null
        )
        val ruleV1 = VoucherRuleVersion(
            id = "v123",
            version = 1,
            type = VoucherType.PERCENTAGE,
            value = BigDecimal("22"),
            cap = Money(15000, CurrencyCode.MYR), // RM150
            minimumSpend = Money(35000, CurrencyCode.MYR),
            thresholdBasis = ThresholdBasis.ORIGINAL_SUBTOTAL
        )
        voucherRepo.saveVoucher(profile, ruleV1)

        // 2. Perform a Calculation and Save Snapshot using V1
        val quoteRequest = QuoteRequest(
            items = listOf(CartLine("item1", Money(80000, CurrencyCode.MYR), 1)),
            voucherRules = listOf(ruleV1),
            shipping = CostComponent(ComponentType.SHIPPING, Money(800, CurrencyCode.MYR), false),
            exchangeRate = null,
            evaluatedAt = Instant.parse("2026-10-09T10:05:00Z")
        )
        val quoteResult = QuoteResult(
            amountDue = Money(65800, CurrencyCode.MYR),
            reportingAmountDue = null,
            immediateDiscounts = Money(15000, CurrencyCode.MYR),
            futureRewards = Money(0, CurrencyCode.MYR),
            appliedVouchers = listOf(ruleV1),
            rejectedVouchers = emptyList(),
            missingInformation = emptyList(),
            trace = emptyList(),
            status = QuoteStatus.COMPLETE
        )
        val snapshotV1 = SavedSnapshot(
            id = "snap-001",
            scenarioId = null,
            request = quoteRequest,
            result = quoteResult,
            calculatedAt = Instant.parse("2026-10-09T10:05:00Z")
        )
        snapshotRepo.saveSnapshot(snapshotV1)

        // 3. Update the Voucher (e.g., Cap changed to RM100 -> Version 2)
        val profileUpdated = profile.copy(
            currentVersion = 2,
            updatedAt = Instant.parse("2026-10-09T12:00:00Z")
        )
        val ruleV2 = ruleV1.copy(
            version = 2,
            cap = Money(10000, CurrencyCode.MYR)
        )
        voucherRepo.saveVoucher(profileUpdated, ruleV2)

        // 4. Verify DB State: Latest Voucher is V2
        val latestVoucher = voucherRepo.getVoucherById("v123")
        assertNotNull(latestVoucher)
        assertEquals(2, latestVoucher.currentVersion)
        assertEquals(10000L, latestVoucher.latestRule?.cap?.minorUnits)

        // 5. Verify the Historical Snapshot remains identical (Still referencing V1 and RM150 cap)
        val snapshots = snapshotRepo.getAllSnapshots().first()
        assertEquals(1, snapshots.size)
        
        val retrievedSnapshot = snapshots[0]
        val appliedRuleInSnapshot = retrievedSnapshot.result.appliedVouchers[0]
        
        assertEquals(1, appliedRuleInSnapshot.version, "Snapshot should preserve old version 1")
        assertEquals(15000L, appliedRuleInSnapshot.cap?.minorUnits, "Snapshot should preserve old cap RM150")
    }
}
