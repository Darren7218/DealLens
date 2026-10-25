package com.deallens.core.data.repository

import com.deallens.core.data.local.dao.VoucherDao
import com.deallens.core.data.local.entity.VoucherEntity
import com.deallens.core.data.local.entity.VoucherRuleVersionEntity
import com.deallens.core.domain.repository.VoucherProfile
import com.deallens.core.domain.repository.VoucherRepository
import com.deallens.core.domain.VoucherRuleVersion
import com.deallens.core.domain.VoucherType
import com.deallens.core.domain.ThresholdBasis
import com.deallens.core.domain.Money
import com.deallens.core.domain.CurrencyCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject

class RoomVoucherRepository @Inject constructor(
    private val voucherDao: VoucherDao
) : VoucherRepository {

    override fun getAllVouchers(): Flow<List<VoucherProfile>> {
        return voucherDao.getAllVouchers().map { entities ->
            entities.map { entity ->
                val versions = voucherDao.getVoucherVersions(entity.id)
                val latest = versions.firstOrNull()
                VoucherProfile(
                    id = entity.id,
                    name = entity.name,
                    platform = entity.platform,
                    currentVersion = entity.currentVersion,
                    expiresAt = entity.expiresAt,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    latestRule = latest?.toDomain()
                )
            }
        }
    }

    override suspend fun getVoucherById(id: String): VoucherProfile? {
        val entity = voucherDao.getVoucherById(id) ?: return null
        val versions = voucherDao.getVoucherVersions(entity.id)
        val latest = versions.firstOrNull()
        return VoucherProfile(
            id = entity.id,
            name = entity.name,
            platform = entity.platform,
            currentVersion = entity.currentVersion,
            expiresAt = entity.expiresAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            latestRule = latest?.toDomain()
        )
    }

    override suspend fun getVouchersExpiringSoon(now: java.time.Instant, withinHours: Long): List<VoucherProfile> {
        val maxExpiry = now.plus(withinHours, java.time.temporal.ChronoUnit.HOURS)
        val entities = voucherDao.getVouchersExpiringSoon(now, maxExpiry)
        return entities.map { entity ->
            val versions = voucherDao.getVoucherVersions(entity.id)
            val latest = versions.firstOrNull()
            VoucherProfile(
                id = entity.id,
                name = entity.name,
                platform = entity.platform,
                currentVersion = entity.currentVersion,
                expiresAt = entity.expiresAt,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                latestRule = latest?.toDomain()
            )
        }
    }

    override suspend fun saveVoucher(profile: VoucherProfile, rule: VoucherRuleVersion) {
        val entity = VoucherEntity(
            id = profile.id,
            name = profile.name,
            platform = profile.platform,
            currentVersion = rule.version,
            expiresAt = profile.expiresAt,
            createdAt = profile.createdAt,
            updatedAt = profile.updatedAt
        )
        
        val versionEntity = VoucherRuleVersionEntity(
            voucherId = profile.id,
            version = rule.version,
            type = rule.type.name,
            valueStr = rule.value.toPlainString(),
            capMinorUnits = rule.cap?.minorUnits,
            capCurrency = rule.cap?.currency?.name,
            minSpendMinorUnits = rule.minimumSpend?.minorUnits,
            minSpendCurrency = rule.minimumSpend?.currency?.name,
            thresholdBasis = rule.thresholdBasis.name,
            createdAt = profile.updatedAt
        )
        
        voucherDao.saveVoucherWithVersion(entity, versionEntity)
    }

    override suspend fun deleteVoucher(id: String) {
        voucherDao.deleteVoucherById(id)
    }
    
    private fun VoucherRuleVersionEntity.toDomain() = VoucherRuleVersion(
        id = this.voucherId,
        version = this.version,
        type = VoucherType.valueOf(this.type),
        value = BigDecimal(this.valueStr),
        cap = if (capMinorUnits != null && capCurrency != null) Money(capMinorUnits, CurrencyCode.valueOf(capCurrency)) else null,
        minimumSpend = if (minSpendMinorUnits != null && minSpendCurrency != null) Money(minSpendMinorUnits, CurrencyCode.valueOf(minSpendCurrency)) else null,
        thresholdBasis = ThresholdBasis.valueOf(this.thresholdBasis)
    )
}
