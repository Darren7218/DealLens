package com.deallens.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val createdAt: Instant,
    val targetPriceMinorUnits: Long? = null,
    val targetCurrency: String? = null,
    val priority: String? = null,
    val isWishlist: Boolean = false
)

@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey val id: String,
    val productId: String, // Foreign key to Product
    val platform: String,
    val sellerName: String,
    val url: String?,
    val condition: String?,
    val warranty: String?,
    val createdAt: Instant
)

@Entity(tableName = "price_observations")
data class PriceObservationEntity(
    @PrimaryKey val id: String,
    val listingId: String, // Foreign key to Listing
    val priceMinorUnits: Long,
    val currencyCode: String,
    val isCrossedOut: Boolean,
    val observedAt: Instant
)
