package com.deallens.core.data.mapping

import com.deallens.core.domain.*
import java.math.BigDecimal
import java.time.Instant

fun Money.toDto() = MoneyDto(minorUnits, currency.name)
fun MoneyDto.toDomain() = Money(minorUnits, CurrencyCode.valueOf(currencyCode))

fun CartLine.toDto() = CartLineDto(id, price.toDto(), quantity)
fun CartLineDto.toDomain() = CartLine(id, price.toDomain(), quantity)

fun VoucherRuleVersion.toDto() = VoucherRuleVersionDto(
    id = id,
    version = version,
    type = type.name,
    valueStr = value.toPlainString(),
    cap = cap?.toDto(),
    minimumSpend = minimumSpend?.toDto(),
    thresholdBasis = thresholdBasis.name
)

fun VoucherRuleVersionDto.toDomain() = VoucherRuleVersion(
    id = id,
    version = version,
    type = VoucherType.valueOf(type),
    value = BigDecimal(valueStr),
    cap = cap?.toDomain(),
    minimumSpend = minimumSpend?.toDomain(),
    thresholdBasis = ThresholdBasis.valueOf(thresholdBasis)
)

fun CostComponent.toDto() = CostComponentDto(
    type = type.name,
    amount = amount?.toDto(),
    isUnknown = isUnknown
)

fun CostComponentDto.toDomain() = CostComponent(
    type = ComponentType.valueOf(type),
    amount = amount?.toDomain(),
    isUnknown = isUnknown
)

fun ExchangeRateSnapshot.toDto() = ExchangeRateSnapshotDto(
    fromCurrency = fromCurrency.name,
    toCurrency = toCurrency.name,
    rateStr = rate.toPlainString(),
    capturedAtEpochMilli = capturedAt.toEpochMilli()
)

fun ExchangeRateSnapshotDto.toDomain() = ExchangeRateSnapshot(
    fromCurrency = CurrencyCode.valueOf(fromCurrency),
    toCurrency = CurrencyCode.valueOf(toCurrency),
    rate = BigDecimal(rateStr),
    capturedAt = Instant.ofEpochMilli(capturedAtEpochMilli)
)

fun QuoteRequest.toDto() = QuoteRequestDto(
    items = items.map { it.toDto() },
    voucherRules = voucherRules.map { it.toDto() },
    shipping = shipping?.toDto(),
    exchangeRate = exchangeRate?.toDto(),
    evaluatedAtEpochMilli = evaluatedAt.toEpochMilli()
)

fun QuoteRequestDto.toDomain() = QuoteRequest(
    items = items.map { it.toDomain() },
    voucherRules = voucherRules.map { it.toDomain() },
    shipping = shipping?.toDomain(),
    exchangeRate = exchangeRate?.toDomain(),
    evaluatedAt = Instant.ofEpochMilli(evaluatedAtEpochMilli)
)

fun CalculationStep.toDto() = CalculationStepDto(
    description = description,
    amount = amount?.toDto()
)
fun CalculationStepDto.toDomain() = CalculationStep(
    description = description,
    amount = amount?.toDomain()
)

fun QuoteResult.toDto() = QuoteResultDto(
    amountDue = amountDue?.toDto(),
    reportingAmountDue = reportingAmountDue?.toDto(),
    immediateDiscounts = immediateDiscounts.toDto(),
    futureRewards = futureRewards.toDto(),
    appliedVouchers = appliedVouchers.map { it.toDto() },
    rejectedVouchers = rejectedVouchers.map { it.first.toDto() to it.second },
    missingInformation = missingInformation,
    trace = trace.map { it.toDto() },
    status = status.name
)

fun QuoteResultDto.toDomain() = QuoteResult(
    amountDue = amountDue?.toDomain(),
    reportingAmountDue = reportingAmountDue?.toDomain(),
    immediateDiscounts = immediateDiscounts.toDomain(),
    futureRewards = futureRewards.toDomain(),
    appliedVouchers = appliedVouchers.map { it.toDomain() },
    rejectedVouchers = rejectedVouchers.map { it.first.toDomain() to it.second },
    missingInformation = missingInformation,
    trace = trace.map { it.toDomain() },
    status = QuoteStatus.valueOf(status)
)
