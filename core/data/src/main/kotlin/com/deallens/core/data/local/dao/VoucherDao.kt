package com.deallens.core.data.local.dao

import androidx.room.*
import com.deallens.core.data.local.entity.VoucherEntity
import com.deallens.core.data.local.entity.VoucherRuleVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoucherDao {
    @Query("SELECT * FROM vouchers ORDER BY updatedAt DESC")
    fun getAllVouchers(): Flow<List<VoucherEntity>>

    @Query("SELECT * FROM vouchers WHERE id = :id")
    suspend fun getVoucherById(id: String): VoucherEntity?

    @Query("SELECT * FROM vouchers WHERE expiresAt IS NOT NULL AND expiresAt > :now AND expiresAt <= :maxExpiry ORDER BY expiresAt ASC")
    suspend fun getVouchersExpiringSoon(now: java.time.Instant, maxExpiry: java.time.Instant): List<VoucherEntity>

    @Query("SELECT * FROM voucher_versions WHERE voucherId = :voucherId ORDER BY version DESC")
    suspend fun getVoucherVersions(voucherId: String): List<VoucherRuleVersionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: VoucherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucherVersion(version: VoucherRuleVersionEntity)

    @Transaction
    suspend fun saveVoucherWithVersion(voucher: VoucherEntity, version: VoucherRuleVersionEntity) {
        insertVoucher(voucher)
        insertVoucherVersion(version)
    }

    @Query("DELETE FROM vouchers WHERE id = :id")
    suspend fun deleteVoucherById(id: String)
}
