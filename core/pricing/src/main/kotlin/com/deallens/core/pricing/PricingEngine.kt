package com.deallens.core.pricing

import com.deallens.core.domain.*
import java.math.BigDecimal
import java.math.RoundingMode

class PricingEngine {

    fun calculate(request: QuoteRequest): QuoteResult {
        val trace = mutableListOf<CalculationStep>()
        val missingInfo = mutableListOf<String>()
        val appliedVouchers = mutableListOf<VoucherRuleVersion>()
        val rejectedVouchers = mutableListOf<Pair<VoucherRuleVersion, String>>()
        
        if (request.items.isEmpty()) {
            return QuoteResult(
                amountDue = null,
                reportingAmountDue = null,
                immediateDiscounts = Money(0, CurrencyCode.MYR),
                futureRewards = Money(0, CurrencyCode.MYR),
                appliedVouchers = emptyList(),
                rejectedVouchers = emptyList(),
                missingInformation = listOf("Cart is empty"),
                trace = trace,
                status = QuoteStatus.INCOMPLETE
            )
        }

        val baseCurrency = request.items.first().price.currency
        if (request.items.any { it.price.currency != baseCurrency }) {
            throw IllegalArgumentException("All items must have the same currency")
        }

        var subtotal = Money(0, baseCurrency)
        for (item in request.items) {
            subtotal += Money(item.price.minorUnits * item.quantity, baseCurrency)
        }
        trace.add(CalculationStep("Subtotal", subtotal))

        var totalDiscount = Money(0, baseCurrency)
        var totalReward = Money(0, baseCurrency)
        var currentAmount = subtotal
        var shippingAmount = request.shipping?.amount ?: Money(0, baseCurrency)
        
        val shipping = request.shipping
        var isShippingUnknown = false
        if (shipping == null || shipping.isUnknown) {
            isShippingUnknown = true
            missingInfo.add("Shipping cost is unknown")
        }

        for (voucher in request.voucherRules) {
            val minimumSpend = voucher.minimumSpend ?: Money(0, baseCurrency)
            val basisAmount = if (voucher.thresholdBasis == ThresholdBasis.ORIGINAL_SUBTOTAL) subtotal else currentAmount

            if (basisAmount < minimumSpend) {
                rejectedVouchers.add(Pair(voucher, "Minimum spend not reached"))
                trace.add(CalculationStep("Rejected voucher ${voucher.id}: Minimum spend not reached"))
                continue
            }

            if (voucher.type == VoucherType.PERCENTAGE) {
                val discountDecimal = BigDecimal(currentAmount.minorUnits)
                    .multiply(voucher.value)
                    .divide(BigDecimal("100"), 0, RoundingMode.HALF_UP)
                
                var discountAmount = Money(discountDecimal.toLong(), baseCurrency)
                
                val cap = voucher.cap
                if (cap != null && discountAmount > cap) {
                    discountAmount = cap
                    trace.add(CalculationStep("Discount capped", discountAmount))
                }
                
                if (discountAmount > currentAmount) {
                    discountAmount = currentAmount
                }

                currentAmount -= discountAmount
                totalDiscount += discountAmount
                appliedVouchers.add(voucher)
                trace.add(CalculationStep("Applied percentage discount", discountAmount))

            } else if (voucher.type == VoucherType.FIXED) {
                var discountAmount = Money(voucher.value.multiply(BigDecimal("100")).toLong(), baseCurrency) // assuming value is major units
                if (discountAmount > currentAmount) {
                    discountAmount = currentAmount
                }
                currentAmount -= discountAmount
                totalDiscount += discountAmount
                appliedVouchers.add(voucher)
                trace.add(CalculationStep("Applied fixed discount", discountAmount))

            } else if (voucher.type == VoucherType.REWARD) {
                val rewardAmount = if (voucher.value <= BigDecimal("100")) { // it's percentage
                     val rewardDecimal = BigDecimal(currentAmount.minorUnits)
                        .multiply(voucher.value)
                        .divide(BigDecimal("100"), 0, RoundingMode.HALF_UP)
                     Money(rewardDecimal.toLong(), baseCurrency)
                } else {
                     Money(voucher.value.multiply(BigDecimal("100")).toLong(), baseCurrency)
                }
                val cap = voucher.cap
                val actualReward = if (cap != null && rewardAmount > cap) cap else rewardAmount
                totalReward += actualReward
                appliedVouchers.add(voucher)
                trace.add(CalculationStep("Future reward applied", actualReward))
            } else if (voucher.type == VoucherType.SHIPPING) {
                 // Ignore for now unless there's a shipping fixture
                 appliedVouchers.add(voucher)
            }
        }

        var amountDue: Money? = currentAmount
        if (isShippingUnknown) {
            amountDue = null
            trace.add(CalculationStep("Total incomplete due to unknown shipping"))
        } else {
            amountDue = currentAmount + shippingAmount
            trace.add(CalculationStep("Shipping applied", shippingAmount))
            trace.add(CalculationStep("Amount Due", amountDue))
        }

        var reportingAmountDue: Money? = null
        if (amountDue != null) {
            val exchangeRate = request.exchangeRate
            if (exchangeRate != null) {
                val reportingMinorUnits = BigDecimal(amountDue.minorUnits)
                    .multiply(exchangeRate.rate)
                    .setScale(0, RoundingMode.HALF_UP)
                reportingAmountDue = Money(reportingMinorUnits.toLong(), exchangeRate.toCurrency)
                trace.add(CalculationStep("Converted to reporting currency", reportingAmountDue))
            } else {
                reportingAmountDue = amountDue
            }
        }

        val status = if (isShippingUnknown) QuoteStatus.INCOMPLETE else QuoteStatus.COMPLETE

        return QuoteResult(
            amountDue = amountDue,
            reportingAmountDue = reportingAmountDue,
            immediateDiscounts = totalDiscount,
            futureRewards = totalReward,
            appliedVouchers = appliedVouchers,
            rejectedVouchers = rejectedVouchers,
            missingInformation = missingInfo,
            trace = trace,
            status = status
        )
    }
}
