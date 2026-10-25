package com.deallens.core.data.mapping

import com.deallens.core.domain.*
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class MoneyDto(
    val minorUnits: Long,
    val currencyCode: String
)

@Serializable
data class CartLineDto(
    val id: String,
    val price: MoneyDto,
    val quantity: Int
)

@Serializable
data class VoucherRuleVersionDto(
    val id: String,
    val version: Int,
    val type: String,
    val valueStr: String,
    val cap: MoneyDto?,
    val minimumSpend: MoneyDto?,
    val thresholdBasis: String
)

@Serializable
data class CostComponentDto(
    val type: String,
    val amount: MoneyDto?,
    val isUnknown: Boolean
)

@Serializable
data class ExchangeRateSnapshotDto(
    val fromCurrency: String,
    val toCurrency: String,
    val rateStr: String,
    val capturedAtEpochMilli: Long
)

@Serializable
data class QuoteRequestDto(
    val items: List<CartLineDto>,
    val voucherRules: List<VoucherRuleVersionDto>,
    val shipping: CostComponentDto?,
    val exchangeRate: ExchangeRateSnapshotDto?,
    val evaluatedAtEpochMilli: Long
)

@Serializable
data class CalculationStepDto(
    val description: String,
    val amount: MoneyDto?
)

@Serializable
data class QuoteResultDto(
    val amountDue: MoneyDto?,
    val reportingAmountDue: MoneyDto?,
    val immediateDiscounts: MoneyDto,
    val futureRewards: MoneyDto,
    val appliedVouchers: List<VoucherRuleVersionDto>,
    val rejectedVouchers: List<Pair<VoucherRuleVersionDto, String>>,
    val missingInformation: List<String>,
    val trace: List<CalculationStepDto>,
    val status: String
)
