package com.deallens.core.domain

import java.time.Instant

enum class Priority {
    HIGH, MEDIUM, LOW
}

data class Product(
    val id: String,
    val name: String,
    val description: String?,
    val targetPrice: Money? = null,
    val priority: Priority? = null,
    val isWishlist: Boolean = false,
    val createdAt: Instant
)

data class Listing(
    val id: String,
    val productId: String,
    val platform: String,
    val sellerName: String,
    val url: String? = null,
    val condition: String? = null,
    val warranty: String? = null,
    val createdAt: Instant
)

data class PriceObservation(
    val id: String,
    val listingId: String,
    val price: Money,
    val isCrossedOut: Boolean = false,
    val observedAt: Instant
)

data class ProductHistory(
    val product: Product,
    val listings: List<Listing>,
    val observations: List<PriceObservation>,
    val lowestRecordedPrice: Money?
)

object PriceHistoryCalculator {
    fun calculateLowestRecordedPrice(observations: List<PriceObservation>): Money? {
        val validObs = observations.filter { !it.isCrossedOut }
        if (validObs.isEmpty()) return null
        return validObs.minByOrNull { it.price.minorUnits }?.price
    }

    fun sortByObservedAtAscending(observations: List<PriceObservation>): List<PriceObservation> {
        return observations.sortedBy { it.observedAt }
    }

    fun meetsOrBeatsTarget(currentPrice: Money, targetPrice: Money): Boolean {
        if (currentPrice.currency != targetPrice.currency) return false
        return currentPrice.minorUnits <= targetPrice.minorUnits
    }
}
