package com.deallens.core.data.local.dao

import androidx.room.*
import com.deallens.core.data.local.entity.PurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM confirmed_purchases ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM confirmed_purchases WHERE id = :id LIMIT 1")
    suspend fun getPurchaseById(id: String): PurchaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(entity: PurchaseEntity)

    @Query("DELETE FROM confirmed_purchases WHERE id = :id")
    suspend fun deletePurchase(id: String)
}
