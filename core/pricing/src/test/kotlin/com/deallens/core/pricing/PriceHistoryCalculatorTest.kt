package com.deallens.core.pricing

import com.deallens.core.domain.CurrencyCode
import com.deallens.core.domain.Money
import com.deallens.core.domain.PriceHistoryCalculator
import com.deallens.core.domain.PriceObservation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PriceHistoryCalculatorTest {

    @Test
    fun `verifies PriceObservation chronological ordering`() {
        val t1 = Instant.parse("2026-10-01T10:00:00Z")
        val t2 = Instant.parse("2026-10-03T10:00:00Z")
        val t3 = Instant.parse("2026-10-05T10:00:00Z")

        val obs1 = PriceObservation("1", "l1", Money(85000, CurrencyCode.MYR), false, t2)
        val obs2 = PriceObservation("2", "l1", Money(80000, CurrencyCode.MYR), false, t1)
        val obs3 = PriceObservation("3", "l1", Money(75000, CurrencyCode.MYR), false, t3)

        val unSorted = listOf(obs1, obs2, obs3)
        val sorted = PriceHistoryCalculator.sortByObservedAtAscending(unSorted)

        assertEquals("2", sorted[0].id, "First should be oldest observation (t1)")
        assertEquals("1", sorted[1].id, "Second should be middle observation (t2)")
        assertEquals("3", sorted[2].id, "Third should be newest observation (t3)")
    }

    @Test
    fun `calculates lowest recorded price correctly and ignores crossed out strike through prices`() {
        val now = Instant.now()
        val obsList = listOf(
            PriceObservation("1", "l1", Money(85000, CurrencyCode.MYR), false, now), // RM850
            PriceObservation("2", "l1", Money(79900, CurrencyCode.MYR), false, now), // RM799 (Lowest valid)
            PriceObservation("3", "l1", Money(90000, CurrencyCode.MYR), false, now), // RM900
            PriceObservation("4", "l1", Money(65000, CurrencyCode.MYR), true, now)   // RM650 (Crossed-out/Invalid RRP)
        )

        val lowest = PriceHistoryCalculator.calculateLowestRecordedPrice(obsList)
        assertNotNull(lowest)
        assertEquals(79900L, lowest.minorUnits, "Should identify RM799 as lowest recorded price, ignoring crossed-out RM650")
        assertEquals(CurrencyCode.MYR, lowest.currency)
    }

    @Test
    fun `returns null lowest recorded price when observations are empty or all crossed out`() {
        val emptyObs = emptyList<PriceObservation>()
        assertNull(PriceHistoryCalculator.calculateLowestRecordedPrice(emptyObs))

        val allCrossedOut = listOf(
            PriceObservation("1", "l1", Money(50000, CurrencyCode.MYR), true, Instant.now())
        )
        assertNull(PriceHistoryCalculator.calculateLowestRecordedPrice(allCrossedOut))
    }

    @Test
    fun `visually flags when observed price meets or beats wishlist target price`() {
        val targetPrice = Money(80000, CurrencyCode.MYR) // Target: RM800

        // Price beats target: RM799 <= RM800 -> true
        val beatingPrice = Money(79900, CurrencyCode.MYR)
        assertTrue(PriceHistoryCalculator.meetsOrBeatsTarget(beatingPrice, targetPrice))

        // Price exactly meets target: RM800 <= RM800 -> true
        val exactPrice = Money(80000, CurrencyCode.MYR)
        assertTrue(PriceHistoryCalculator.meetsOrBeatsTarget(exactPrice, targetPrice))

        // Price is higher than target: RM801 > RM800 -> false
        val higherPrice = Money(80100, CurrencyCode.MYR)
        assertFalse(PriceHistoryCalculator.meetsOrBeatsTarget(higherPrice, targetPrice))

        // Currency mismatch: CNY 700 vs MYR 800 -> false
        val differentCurrency = Money(70000, CurrencyCode.CNY)
        assertFalse(PriceHistoryCalculator.meetsOrBeatsTarget(differentCurrency, targetPrice))
    }
}
