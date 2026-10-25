package com.deallens.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deallens.core.domain.*
import com.deallens.core.domain.repository.VoucherProfile
import com.deallens.core.domain.repository.VoucherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val voucherRepository: VoucherRepository
) : ViewModel() {

    val vouchers = voucherRepository.getAllVouchers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveCustomVoucher(
        existingProfile: VoucherProfile? = null,
        name: String,
        platform: String,
        type: VoucherType,
        valueStr: String,
        currency: CurrencyCode,
        capStr: String,
        minSpendStr: String,
        thresholdBasis: ThresholdBasis,
        expiryHours: Long?
    ) {
        val valueBd = valueStr.trim().toBigDecimalOrNull() ?: return
        if (valueBd <= BigDecimal.ZERO) return

        val capMinor = capStr.trim().toBigDecimalOrNull()?.multiply(BigDecimal(100))?.toLong()
        val minSpendMinor = minSpendStr.trim().toBigDecimalOrNull()?.multiply(BigDecimal(100))?.toLong()

        viewModelScope.launch {
            val now = Instant.now()
            val id = existingProfile?.id ?: UUID.randomUUID().toString()
            val nextVersion = (existingProfile?.currentVersion ?: 0) + 1
            val expiresAt = expiryHours?.let { now.plus(it, ChronoUnit.HOURS) } ?: existingProfile?.expiresAt

            val profile = VoucherProfile(
                id = id,
                name = name.ifBlank { "${platform.ifBlank { "Deal" }} Voucher" },
                platform = platform.ifBlank { "Generic" },
                currentVersion = nextVersion,
                expiresAt = expiresAt,
                createdAt = existingProfile?.createdAt ?: now,
                updatedAt = now,
                latestRule = null
            )
            val rule = VoucherRuleVersion(
                id = id,
                version = nextVersion,
                type = type,
                value = valueBd,
                cap = capMinor?.let { if (it > 0) Money(it, currency) else null },
                minimumSpend = minSpendMinor?.let { if (it > 0) Money(it, currency) else null },
                thresholdBasis = thresholdBasis
            )
            voucherRepository.saveVoucher(profile, rule)
        }
    }

    fun deleteVoucher(id: String) {
        viewModelScope.launch {
            voucherRepository.deleteVoucher(id)
        }
    }

    fun addCuratedPresets() {
        viewModelScope.launch {
            val now = Instant.now()
            // 1. Shopee 22% Cap RM150 Min RM350
            val s1Id = UUID.randomUUID().toString()
            voucherRepository.saveVoucher(
                VoucherProfile(
                    id = s1Id,
                    name = "Shopee Mall 22% Off (Cap RM150)",
                    platform = "Shopee",
                    currentVersion = 1,
                    expiresAt = now.plus(36, ChronoUnit.HOURS),
                    createdAt = now,
                    updatedAt = now,
                    latestRule = null
                ),
                VoucherRuleVersion(
                    id = s1Id,
                    version = 1,
                    type = VoucherType.PERCENTAGE,
                    value = BigDecimal("22"),
                    cap = Money(15000, CurrencyCode.MYR),
                    minimumSpend = Money(35000, CurrencyCode.MYR),
                    thresholdBasis = ThresholdBasis.ORIGINAL_SUBTOTAL
                )
            )

            // 2. Lazada RM40 Off Min RM300
            val l1Id = UUID.randomUUID().toString()
            voucherRepository.saveVoucher(
                VoucherProfile(
                    id = l1Id,
                    name = "Lazada Campaign RM40 Off",
                    platform = "Lazada",
                    currentVersion = 1,
                    expiresAt = now.plus(7, ChronoUnit.DAYS),
                    createdAt = now,
                    updatedAt = now,
                    latestRule = null
                ),
                VoucherRuleVersion(
                    id = l1Id,
                    version = 1,
                    type = VoucherType.FIXED,
                    value = BigDecimal("40"),
                    cap = null,
                    minimumSpend = Money(30000, CurrencyCode.MYR),
                    thresholdBasis = ThresholdBasis.ORIGINAL_SUBTOTAL
                )
            )

            // 3. PDD ¥50 Off Min ¥400
            val p1Id = UUID.randomUUID().toString()
            voucherRepository.saveVoucher(
                VoucherProfile(
                    id = p1Id,
                    name = "PDD Super Brand ¥50 Off",
                    platform = "PDD",
                    currentVersion = 1,
                    expiresAt = now.plus(14, ChronoUnit.DAYS),
                    createdAt = now,
                    updatedAt = now,
                    latestRule = null
                ),
                VoucherRuleVersion(
                    id = p1Id,
                    version = 1,
                    type = VoucherType.FIXED,
                    value = BigDecimal("50"),
                    cap = null,
                    minimumSpend = Money(40000, CurrencyCode.CNY),
                    thresholdBasis = ThresholdBasis.ORIGINAL_SUBTOTAL
                )
            )
        }
    }
}
