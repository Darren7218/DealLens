package com.deallens.core.domain

import java.math.BigDecimal
import java.time.Instant

enum class VoucherType {
    PERCENTAGE, FIXED, SHIPPING, REWARD
}

enum class ThresholdBasis {
    ORIGINAL_SUBTOTAL, AFTER_DISCOUNTS
}

data class VoucherRuleVersion(
    val id: String,
    val version: Int,
    val type: VoucherType,
    val value: BigDecimal,
    val cap: Money? = null,
    val minimumSpend: Money? = null,
    val thresholdBasis: ThresholdBasis = ThresholdBasis.ORIGINAL_SUBTOTAL
)

data class CartLine(
    val id: String,
    val price: Money,
    val quantity: Int
)

enum class ComponentType {
    SHIPPING, TAX, FEE
}

data class CostComponent(
    val type: ComponentType,
    val amount: Money?,
    val isUnknown: Boolean = false
)

data class ExchangeRateSnapshot(
    val fromCurrency: CurrencyCode,
    val toCurrency: CurrencyCode,
    val rate: BigDecimal,
    val capturedAt: Instant
)

data class QuoteRequest(
    val items: List<CartLine>,
    val voucherRules: List<VoucherRuleVersion>,
    val shipping: CostComponent?,
    val exchangeRate: ExchangeRateSnapshot?,
    val evaluatedAt: Instant
)

enum class QuoteStatus {
    COMPLETE, CONDITIONAL, INCOMPLETE
}

data class CalculationStep(
    val description: String,
    val amount: Money? = null
)

data class QuoteResult(
    val amountDue: Money?,
    val reportingAmountDue: Money?,
    val immediateDiscounts: Money,
    val futureRewards: Money,
    val appliedVouchers: List<VoucherRuleVersion>,
    val rejectedVouchers: List<Pair<VoucherRuleVersion, String>>,
    val missingInformation: List<String>,
    val trace: List<CalculationStep>,
    val status: QuoteStatus
)
