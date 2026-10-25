package com.deallens.core.data.di

import android.content.Context
import androidx.room.Room
import com.deallens.core.data.local.DealLensDatabase
import com.deallens.core.data.local.dao.ScenarioDao
import com.deallens.core.data.local.dao.SnapshotDao
import com.deallens.core.data.local.dao.VoucherDao
import com.deallens.core.data.repository.RoomSnapshotRepository
import com.deallens.core.data.repository.RoomVoucherRepository
import com.deallens.core.domain.repository.SnapshotRepository
import com.deallens.core.domain.repository.VoucherRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DealLensDatabase {
        return Room.databaseBuilder(
            context,
            DealLensDatabase::class.java,
            "deallens_db"
        ).build()
    }

    @Provides
    fun provideVoucherDao(db: DealLensDatabase): VoucherDao = db.voucherDao()

    @Provides
    fun provideProductDao(db: DealLensDatabase): com.deallens.core.data.local.dao.ProductDao = db.productDao()

    @Provides
    fun provideScenarioDao(db: DealLensDatabase): ScenarioDao = db.scenarioDao()

    @Provides
    fun provideSnapshotDao(db: DealLensDatabase): SnapshotDao = db.snapshotDao()

    @Provides
    fun provideBackupDao(db: DealLensDatabase): com.deallens.core.data.local.dao.BackupDao = db.backupDao()

    @Provides
    fun providePurchaseDao(db: DealLensDatabase): com.deallens.core.data.local.dao.PurchaseDao = db.purchaseDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindVoucherRepository(
        impl: RoomVoucherRepository
    ): VoucherRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        impl: com.deallens.core.data.repository.RoomProductRepository
    ): com.deallens.core.domain.repository.ProductRepository

    @Binds
    @Singleton
    abstract fun bindSnapshotRepository(
        impl: RoomSnapshotRepository
    ): SnapshotRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        impl: com.deallens.core.data.repository.RoomBackupRestoreRepository
    ): com.deallens.core.domain.repository.BackupRepository

    @Binds
    @Singleton
    abstract fun bindPurchaseRepository(
        impl: com.deallens.core.data.repository.RoomPurchaseRepository
    ): com.deallens.core.domain.repository.PurchaseRepository
}
