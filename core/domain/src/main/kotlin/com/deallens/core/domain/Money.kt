package com.deallens.core.domain

enum class CurrencyCode {
    MYR, CNY, USD, EUR
}

data class Money(val minorUnits: Long, val currency: CurrencyCode) {
    // Allow negative money for marginal comparisons
    operator fun unaryMinus(): Money = Money(-minorUnits, currency)

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies: $currency and ${other.currency}" }
        return Money(minorUnits + other.minorUnits, currency)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Cannot subtract different currencies: $currency and ${other.currency}" }
        return Money(minorUnits - other.minorUnits, currency)
    }

    operator fun compareTo(other: Money): Int {
        require(currency == other.currency) { "Cannot compare different currencies: $currency and ${other.currency}" }
        return minorUnits.compareTo(other.minorUnits)
    }
}
