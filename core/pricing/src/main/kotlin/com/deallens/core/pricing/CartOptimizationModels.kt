package com.deallens.core.pricing

import com.deallens.core.domain.Money
import com.deallens.core.domain.VoucherRuleVersion

data class CartItem(
    val id: String,
    val name: String,
    val sellerId: String,
    val price: Money,
    val quantity: Int = 1
)

data class CartVoucher(
    val voucher: VoucherRuleVersion,
    val sellerId: String? = null, // null means it applies to all items in the order
    val isReusable: Boolean = false,
    val isStackable: Boolean = true
)

data class CartOptimizationRequest(
    val items: List<CartItem>,
    val vouchers: List<CartVoucher>
)

data class OrderSplit(
    val items: List<CartItem>,
    val appliedVouchers: List<CartVoucher>,
    val subtotal: Money,
    val immediateDiscount: Money,
    val amountDue: Money
)

data class CartOptimizationResult(
    val splits: List<OrderSplit>,
    val totalAmountDue: Money,
    val totalDiscount: Money,
    val explanation: String,
    val marginalEvaluations: List<MarginalEvaluation> = emptyList()
)

data class MarginalEvaluation(
    val item: CartItem,
    val marginalCashRequired: Money,
    val marginalDiscountGained: Money,
    val explanation: String
)
