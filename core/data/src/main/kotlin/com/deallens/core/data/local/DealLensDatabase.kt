package com.deallens.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.deallens.core.data.local.dao.*
import com.deallens.core.data.local.entity.*
import java.time.Instant

class InstantConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilli()
    }
}

@Database(
    entities = [
        ProductEntity::class,
        ListingEntity::class,
        PriceObservationEntity::class,
        VoucherEntity::class,
        VoucherRuleVersionEntity::class,
        ScenarioEntity::class,
        ScenarioLineEntity::class,
        CalculationSnapshotEntity::class,
        ExchangeRateSnapshotEntity::class,
        PurchaseEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(InstantConverter::class)
abstract class DealLensDatabase : RoomDatabase() {
    abstract fun voucherDao(): VoucherDao
    abstract fun productDao(): ProductDao
    abstract fun scenarioDao(): ScenarioDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun backupDao(): BackupDao
    abstract fun purchaseDao(): PurchaseDao
}
