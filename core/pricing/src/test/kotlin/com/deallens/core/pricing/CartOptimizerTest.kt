package com.deallens.core.pricing

import com.deallens.core.domain.CurrencyCode
import com.deallens.core.domain.Money
import com.deallens.core.domain.VoucherRuleVersion
import com.deallens.core.domain.VoucherType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class CartOptimizerTest {

    private fun money(amount: Long) = Money(amount, CurrencyCode.MYR)
    
    private val myr = CurrencyCode.MYR

    @Test
    fun `test splitting is cheaper than combining due to capped voucher`() {
        val optimizer = CartOptimizer()
        
        // Two items, each 100
        val item1 = CartItem("1", "Item A", "SellerX", money(10000))
        val item2 = CartItem("2", "Item B", "SellerY", money(10000))

        // A reusable 20% off voucher capped at 10. (20% of 100 is 20, but capped at 10).
        // If combined (200), 20% is 40, capped at 10. Discount = 10. Total = 190.
        // If split (100 and 100), 10 off each. Discount = 20. Total = 180.
        val voucherRule = VoucherRuleVersion(
            id = "v1",
            version = 1,
            type = VoucherType.PERCENTAGE,
            value = BigDecimal("20"),
            cap = money(1000) // cap at 10 MYR
        )
        val cartVoucher = CartVoucher(voucherRule, isReusable = true)

        val request = CartOptimizationRequest(
            items = listOf(item1, item2),
            vouchers = listOf(cartVoucher)
        )

        val result = optimizer.optimize(request)

        assertEquals(2, result.splits.size, "Should split into 2 orders")
        assertEquals(money(18000), result.totalAmountDue)
        assertEquals(money(2000), result.totalDiscount)
    }

    @Test
    fun `test shared single-use voucher prevents double spending`() {
        val optimizer = CartOptimizer()
        
        val item1 = CartItem("1", "Item A", "SellerX", money(10000))
        val item2 = CartItem("2", "Item B", "SellerY", money(10000))

        // Single-use 20% off voucher capped at 10.
        // Even if we split, we can only use it on ONE of the orders.
        val voucherRule = VoucherRuleVersion(
            id = "v1",
            version = 1,
            type = VoucherType.PERCENTAGE,
            value = BigDecimal("20"),
            cap = money(1000)
        )
        val cartVoucher = CartVoucher(voucherRule, isReusable = false) // Single use!

        val request = CartOptimizationRequest(
            items = listOf(item1, item2),
            vouchers = listOf(cartVoucher)
        )

        val result = optimizer.optimize(request)

        // Total should be 190, because we can only get 10 discount max.
        // Splitting into 2 orders still only yields 10 discount.
        // It might not matter if it splits or not, but total discount must be 10.
        assertEquals(money(1000), result.totalDiscount)
        assertEquals(money(19000), result.totalAmountDue)
    }

    @Test
    fun `test is adding item worth it marginal spend comparison`() {
        val optimizer = CartOptimizer()
        
        // Item 1 is 90.
        val item1 = CartItem("1", "Item A", "SellerX", money(9000))
        // Item 2 is 20. (Total = 110)
        val item2 = CartItem("2", "Item B", "SellerX", money(2000))

        // Voucher: 50 fixed off, min spend 100.
        val voucherRule = VoucherRuleVersion(
            id = "v1",
            version = 1,
            type = VoucherType.FIXED,
            value = BigDecimal("50"),
            minimumSpend = money(10000)
        )
        val cartVoucher = CartVoucher(voucherRule, isReusable = true)

        val request = CartOptimizationRequest(
            items = listOf(item1, item2),
            vouchers = listOf(cartVoucher)
        )

        val result = optimizer.optimize(request)
        
        // With Item 1 alone (90): no discount. Pay 90.
        // With both (110): 50 discount. Pay 60.
        assertEquals(money(6000), result.totalAmountDue)

        val evalItem2 = result.marginalEvaluations.find { it.item.id == "2" }!!
        
        // Marginal cash required: total amount due with both (60) - total amount due without (90) = -30
        assertEquals(money(-3000), evalItem2.marginalCashRequired)
        // Marginal discount gained: 50 - 0 = 50
        assertEquals(money(5000), evalItem2.marginalDiscountGained)
        
        assertTrue(evalItem2.explanation.contains("Worth it"))
    }
}
