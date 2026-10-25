package com.deallens.core.data.repository

import com.deallens.core.data.local.dao.ProductDao
import com.deallens.core.data.local.entity.ListingEntity
import com.deallens.core.data.local.entity.PriceObservationEntity
import com.deallens.core.data.local.entity.ProductEntity
import com.deallens.core.domain.*
import com.deallens.core.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class RoomProductRepository @Inject constructor(
    private val productDao: ProductDao
) : ProductRepository {

    override fun getAllProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getWishlistProducts(): Flow<List<Product>> {
        return productDao.getWishlistProducts().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getProductHistory(productId: String): Flow<ProductHistory?> {
        return productDao.getObservationsForProduct(productId).map { observationEntities ->
            val productEntity = productDao.getProductById(productId) ?: return@map null
            val listings = productDao.getListingsForProduct(productId).map { it.toDomain() }
            val observations = observationEntities.map { it.toDomain() }
            val sortedObservations = PriceHistoryCalculator.sortByObservedAtAscending(observations)
            val lowestPrice = PriceHistoryCalculator.calculateLowestRecordedPrice(sortedObservations)

            ProductHistory(
                product = productEntity.toDomain(),
                listings = listings,
                observations = sortedObservations,
                lowestRecordedPrice = lowestPrice
            )
        }
    }

    override suspend fun saveObservation(
        productName: String,
        platform: String,
        price: Money,
        targetPrice: Money?,
        priority: Priority?,
        isWishlist: Boolean
    ) {
        val now = Instant.now()
        val productId = UUID.randomUUID().toString()
        val listingId = UUID.randomUUID().toString()
        val observationId = UUID.randomUUID().toString()

        val productEntity = ProductEntity(
            id = productId,
            name = productName,
            description = null,
            targetPriceMinorUnits = targetPrice?.minorUnits,
            targetCurrency = targetPrice?.currency?.name,
            priority = priority?.name,
            isWishlist = isWishlist,
            createdAt = now
        )

        val listingEntity = ListingEntity(
            id = listingId,
            productId = productId,
            platform = platform,
            sellerName = "Unknown Seller",
            url = null,
            condition = null,
            warranty = null,
            createdAt = now
        )

        val observationEntity = PriceObservationEntity(
            id = observationId,
            listingId = listingId,
            priceMinorUnits = price.minorUnits,
            currencyCode = price.currency.name,
            isCrossedOut = false,
            observedAt = now
        )

        productDao.saveObservation(productEntity, listingEntity, observationEntity)
    }

    override suspend fun saveWishlistItem(
        productName: String,
        targetPrice: Money,
        priority: Priority
    ) {
        val now = Instant.now()
        val productEntity = ProductEntity(
            id = UUID.randomUUID().toString(),
            name = productName,
            description = null,
            targetPriceMinorUnits = targetPrice.minorUnits,
            targetCurrency = targetPrice.currency.name,
            priority = priority.name,
            isWishlist = true,
            createdAt = now
        )
        productDao.insertProduct(productEntity)
    }

    private fun ProductEntity.toDomain() = Product(
        id = id,
        name = name,
        description = description,
        targetPrice = if (targetPriceMinorUnits != null && targetCurrency != null) {
            Money(targetPriceMinorUnits, CurrencyCode.valueOf(targetCurrency))
        } else null,
        priority = priority?.let { Priority.valueOf(it) },
        isWishlist = isWishlist,
        createdAt = createdAt
    )

    private fun ListingEntity.toDomain() = Listing(
        id = id,
        productId = productId,
        platform = platform,
        sellerName = sellerName,
        url = url,
        condition = condition,
        warranty = warranty,
        createdAt = createdAt
    )

    private fun PriceObservationEntity.toDomain() = PriceObservation(
        id = id,
        listingId = listingId,
        price = Money(priceMinorUnits, CurrencyCode.valueOf(currencyCode)),
        isCrossedOut = isCrossedOut,
        observedAt = observedAt
    )
}
