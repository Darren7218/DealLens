package com.deallens.core.pricing

import com.deallens.core.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class PricingEngineTest {

    private val engine = PricingEngine()

    @Test
    fun `fixture 1 - 22 percent capped at 150 minimum 350`() {
        val request = QuoteRequest(
            items = listOf(CartLine("1", Money(80000, CurrencyCode.MYR), 1)),
            voucherRules = listOf(
                VoucherRuleVersion(
                    id = "v1", version = 1,
                    type = VoucherType.PERCENTAGE,
                    value = BigDecimal("22"),
                    cap = Money(15000, CurrencyCode.MYR),
                    minimumSpend = Money(35000, CurrencyCode.MYR)
                )
            ),
            shipping = CostComponent(ComponentType.SHIPPING, Money(800, CurrencyCode.MYR)),
            exchangeRate = null,
            evaluatedAt = Instant.now()
        )

        val result = engine.calculate(request)
        
        assertEquals(Money(65800, CurrencyCode.MYR), result.amountDue)
        assertEquals(Money(15000, CurrencyCode.MYR), result.immediateDiscounts)
        assertEquals(1, result.appliedVouchers.size)
        assertEquals(0, result.rejectedVouchers.size)
        assertEquals(QuoteStatus.COMPLETE, result.status)
    }

    @Test
    fun `fixture 2 - 30 percent capped at 80 minimum 100`() {
        val request = QuoteRequest(
            items = listOf(CartLine("1", Money(80000, CurrencyCode.MYR), 1)),
            voucherRules = listOf(
                VoucherRuleVersion(
                    id = "v2", version = 1,
                    type = VoucherType.PERCENTAGE,
                    value = BigDecimal("30"),
                    cap = Money(8000, CurrencyCode.MYR),
                    minimumSpend = Money(10000, CurrencyCode.MYR)
                )
            ),
            shipping = CostComponent(ComponentType.SHIPPING, Money(800, CurrencyCode.MYR)),
            exchangeRate = null,
            evaluatedAt = Instant.now()
        )

        val result = engine.calculate(request)
        
        assertEquals(Money(72800, CurrencyCode.MYR), result.amountDue)
        assertEquals(Money(8000, CurrencyCode.MYR), result.immediateDiscounts)
        assertEquals(QuoteStatus.COMPLETE, result.status)
    }

    @Test
    fun `fixture 3 - RM349_99 with minimum RM350 is ineligible`() {
        val request = QuoteRequest(
            items = listOf(CartLine("1", Money(34999, CurrencyCode.MYR), 1)),
            voucherRules = listOf(
                VoucherRuleVersion(
                    id = "v3", version = 1,
                    type = VoucherType.PERCENTAGE,
                    value = BigDecimal("10"),
                    minimumSpend = Money(35000, CurrencyCode.MYR)
                )
            ),
            shipping = CostComponent(ComponentType.SHIPPING, Money(0, CurrencyCode.MYR)),
            exchangeRate = null,
            evaluatedAt = Instant.now()
        )

        val result = engine.calculate(request)
        
        assertEquals(Money(34999, CurrencyCode.MYR), result.amountDue)
        assertEquals(0, result.appliedVouchers.size)
        assertEquals(1, result.rejectedVouchers.size)
    }

    @Test
    fun `fixture 4 - RM350 with minimum RM350 and 22 percent discount`() {
        val request = QuoteRequest(
            items = listOf(CartLine("1", Money(35000, CurrencyCode.MYR), 1)),
            voucherRules = listOf(
                VoucherRuleVersion(
                    id = "v4", version = 1,
                    type = VoucherType.PERCENTAGE,
                    value = BigDecimal("22"),
                    minimumSpend = Money(35000, CurrencyCode.MYR)
                )
            ),
            shipping = CostComponent(ComponentType.SHIPPING, Money(0, CurrencyCode.MYR)),
            exchangeRate = null,
            evaluatedAt = Instant.now()
        )

        val result = engine.calculate(request)
        
        assertEquals(Money(27300, CurrencyCode.MYR), result.amountDue)
    }

    @Test
    fun `fixture 5 - RM500 due with RM50 future cashback`() {
        val request = QuoteRequest(
            items = listOf(CartLine("1", Money(50000, CurrencyCode.MYR), 1)),
            voucherRules = listOf(
                VoucherRuleVersion(
                    id = "v5", version = 1,
                    type = VoucherType.REWARD,
                    value = BigDecimal("10"),
                    cap = Money(5000, CurrencyCode.MYR)
                )
            ),
            shipping = CostComponent(ComponentType.SHIPPING, Money(0, CurrencyCode.MYR)),
            exchangeRate = null,
            evaluatedAt = Instant.now()
        )

        val result = engine.calculate(request)
        
        assertEquals(Money(50000, CurrencyCode.MYR), result.amountDue)
        assertEquals(Money(5000, CurrencyCode.MYR), result.futureRewards)
    }

    @Test
    fun `fixture 6 - shipping unavailable`() {
        val request = QuoteRequest(
            items = listOf(CartLine("1", Money(10000, CurrencyCode.MYR), 1)),
            voucherRules = emptyList(),
            shipping = CostComponent(ComponentType.SHIPPING, null, isUnknown = true),
            exchangeRate = null,
            evaluatedAt = Instant.now()
        )

        val result = engine.calculate(request)
        
        assertNull(result.amountDue)
        assertEquals(QuoteStatus.INCOMPLETE, result.status)
        assertTrue(result.missingInformation.contains("Shipping cost is unknown"))
    }
}
