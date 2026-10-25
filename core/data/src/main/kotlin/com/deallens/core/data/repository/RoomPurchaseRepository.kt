package com.deallens.core.data.repository

import com.deallens.core.data.local.dao.PurchaseDao
import com.deallens.core.data.local.entity.PurchaseEntity
import com.deallens.core.data.local.entity.PurchaseItemDto
import com.deallens.core.domain.ConfirmedPurchase
import com.deallens.core.domain.CurrencyCode
import com.deallens.core.domain.Money
import com.deallens.core.domain.PurchaseItem
import com.deallens.core.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class RoomPurchaseRepository @Inject constructor(
    private val purchaseDao: PurchaseDao
) : PurchaseRepository {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override fun getAllPurchases(): Flow<List<ConfirmedPurchase>> {
        return purchaseDao.getAllPurchases().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPurchaseById(id: String): ConfirmedPurchase? {
        return purchaseDao.getPurchaseById(id)?.toDomain()
    }

    override suspend fun savePurchase(purchase: ConfirmedPurchase) {
        purchaseDao.insertPurchase(purchase.toEntity())
    }

    override suspend fun deletePurchase(purchaseId: String) {
        purchaseDao.deletePurchase(purchaseId)
    }

    private fun PurchaseEntity.toDomain(): ConfirmedPurchase {
        val currencyEnum = CurrencyCode.valueOf(currency)
        val repCurrencyEnum = CurrencyCode.valueOf(reportingCurrency)
        val itemDtos = try {
            json.decodeFromString<List<PurchaseItemDto>>(itemsJson)
        } catch (_: Exception) {
            emptyList()
        }
        val domainItems = itemDtos.map { dto ->
            PurchaseItem(
                id = dto.id,
                name = dto.name,
                unitPrice = Money(dto.unitPriceMinorUnits, CurrencyCode.valueOf(dto.currency)),
                quantity = dto.quantity
            )
        }

        return ConfirmedPurchase(
            purchaseId = id,
            timestamp = timestamp,
            items = domainItems,
            sellerOrPlatform = sellerOrPlatform,
            payNowAmount = Money(payNowAmountMinorUnits, currencyEnum),
            originalCurrency = currencyEnum,
            reportingCurrency = repCurrencyEnum,
            immediateDiscountsTotal = Money(immediateDiscountsMinorUnits, currencyEnum),
            futureCashback = Money(futureCashbackMinorUnits, currencyEnum),
            receiptNote = receiptNote,
            receiptImageUri = receiptImageUri,
            snapshotId = snapshotId
        )
    }

    private fun ConfirmedPurchase.toEntity(): PurchaseEntity {
        val dtos = items.map { item ->
            PurchaseItemDto(
                id = item.id,
                name = item.name,
                unitPriceMinorUnits = item.unitPrice.minorUnits,
                currency = item.unitPrice.currency.name,
                quantity = item.quantity
            )
        }
        return PurchaseEntity(
            id = purchaseId,
            timestamp = timestamp,
            itemsJson = json.encodeToString(dtos),
            sellerOrPlatform = sellerOrPlatform,
            payNowAmountMinorUnits = payNowAmount.minorUnits,
            currency = originalCurrency.name,
            reportingCurrency = reportingCurrency.name,
            immediateDiscountsMinorUnits = immediateDiscountsTotal.minorUnits,
            futureCashbackMinorUnits = futureCashback.minorUnits,
            receiptNote = receiptNote,
            receiptImageUri = receiptImageUri,
            snapshotId = snapshotId
        )
    }
}
