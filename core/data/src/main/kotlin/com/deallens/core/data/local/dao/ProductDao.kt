package com.deallens.core.data.local.dao

import androidx.room.*
import com.deallens.core.data.local.entity.ListingEntity
import com.deallens.core.data.local.entity.PriceObservationEntity
import com.deallens.core.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isWishlist = 1 ORDER BY createdAt DESC")
    fun getWishlistProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM listings WHERE productId = :productId ORDER BY createdAt DESC")
    suspend fun getListingsForProduct(productId: String): List<ListingEntity>

    @Query("SELECT * FROM price_observations WHERE listingId = :listingId ORDER BY observedAt ASC")
    suspend fun getObservationsForListing(listingId: String): List<PriceObservationEntity>

    @Query("SELECT po.* FROM price_observations po INNER JOIN listings l ON po.listingId = l.id WHERE l.productId = :productId ORDER BY po.observedAt ASC")
    fun getObservationsForProduct(productId: String): Flow<List<PriceObservationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: ListingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceObservation(observation: PriceObservationEntity)

    @Transaction
    suspend fun saveObservation(
        product: ProductEntity,
        listing: ListingEntity,
        observation: PriceObservationEntity
    ) {
        insertProduct(product)
        insertListing(listing)
        insertPriceObservation(observation)
    }
}
