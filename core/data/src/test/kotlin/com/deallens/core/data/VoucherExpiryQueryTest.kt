package com.deallens.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.deallens.core.data.local.DealLensDatabase
import com.deallens.core.data.local.entity.VoucherEntity
import com.deallens.core.data.local.entity.VoucherRuleVersionEntity
import com.deallens.core.data.repository.RoomVoucherRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoucherExpiryQueryTest {

    private lateinit var db: DealLensDatabase
    private lateinit var voucherRepository: RoomVoucherRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DealLensDatabase::class.java
        ).allowMainThreadQueries().build()
        voucherRepository = RoomVoucherRepository(db.voucherDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `identifies vouchers nearing expiry within 48 hours without touching expired or distant ones`() = runBlocking {
        val now = Instant.parse("2026-10-10T12:00:00Z")

        // 1. Voucher expiring in 12 hours (valid, nearing expiry)
        val v1 = VoucherEntity(
            id = "v-expiring-12h",
            name = "Flash Sale 12h",
            platform = "Shopee",
            currentVersion = 1,
            expiresAt = now.plus(12, ChronoUnit.HOURS),
            createdAt = now.minus(24, ChronoUnit.HOURS),
            updatedAt = now.minus(24, ChronoUnit.HOURS)
        )

        // 2. Voucher expiring in 36 hours (valid, nearing expiry)
        val v2 = VoucherEntity(
            id = "v-expiring-36h",
            name = "PayDay 36h",
            platform = "Lazada",
            currentVersion = 1,
            expiresAt = now.plus(36, ChronoUnit.HOURS),
            createdAt = now.minus(12, ChronoUnit.HOURS),
            updatedAt = now.minus(12, ChronoUnit.HOURS)
        )

        // 3. Voucher already expired 2 hours ago (SHOULD NOT BE TOUCHED)
        val v3 = VoucherEntity(
            id = "v-expired-2h-ago",
            name = "Expired Coupon",
            platform = "Shopee",
            currentVersion = 1,
            expiresAt = now.minus(2, ChronoUnit.HOURS),
            createdAt = now.minus(48, ChronoUnit.HOURS),
            updatedAt = now.minus(48, ChronoUnit.HOURS)
        )

        // 4. Voucher expiring in 5 days (far future, SHOULD NOT BE TOUCHED)
        val v4 = VoucherEntity(
            id = "v-future-5d",
            name = "Monthly Mega Voucher",
            platform = "PDD",
            currentVersion = 1,
            expiresAt = now.plus(120, ChronoUnit.HOURS),
            createdAt = now,
            updatedAt = now
        )

        // 5. Voucher with no expiration date (indefinite, SHOULD NOT BE TOUCHED)
        val v5 = VoucherEntity(
            id = "v-indefinite",
            name = "Welcome Gift No Expiry",
            platform = "Shopee",
            currentVersion = 1,
            expiresAt = null,
            createdAt = now,
            updatedAt = now
        )

        val dao = db.voucherDao()
        dao.insertVoucher(v1)
        dao.insertVoucher(v2)
        dao.insertVoucher(v3)
        dao.insertVoucher(v4)
        dao.insertVoucher(v5)

        // Add dummy version for each so profile mapping works
        listOf(v1, v2, v3, v4, v5).forEach {
            dao.insertVoucherVersion(
                VoucherRuleVersionEntity(
                    voucherId = it.id,
                    version = 1,
                    type = "PERCENTAGE",
                    valueStr = "10",
                    capMinorUnits = null,
                    capCurrency = null,
                    minSpendMinorUnits = null,
                    minSpendCurrency = null,
                    thresholdBasis = "ORIGINAL_SUBTOTAL",
                    createdAt = now
                )
            )
        }

        // Execute query through repository
        val results = voucherRepository.getVouchersExpiringSoon(now = now, withinHours = 48)

        assertEquals("Should only return the 2 vouchers nearing expiry within 48h", 2, results.size)
        val resultIds = results.map { it.id }.toSet()
        assertTrue("Must include voucher expiring in 12h", resultIds.contains("v-expiring-12h"))
        assertTrue("Must include voucher expiring in 36h", resultIds.contains("v-expiring-36h"))
        assertFalse("Must NOT include expired voucher", resultIds.contains("v-expired-2h-ago"))
        assertFalse("Must NOT include far future voucher", resultIds.contains("v-future-5d"))
        assertFalse("Must NOT include indefinite voucher", resultIds.contains("v-indefinite"))
    }
}
