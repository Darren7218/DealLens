package com.deallens.core.data.export

import com.deallens.core.domain.ConfirmedPurchase
import com.deallens.core.domain.Money
import com.deallens.core.domain.PurchaseItem
import com.deallens.core.domain.repository.SavedSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

@Serializable
data class MoneyLensExpenseTransaction(
    val category: String,
    val title: String,
    val amount: Double,
    val currency: String,
    val notes: String,
    val timestamp: String
)

@Serializable
data class MoneyLensExpenseExport(
    val version: Int = 1,
    val source: String = "DealLens",
    val exportedAt: String,
    val transactions: List<MoneyLensExpenseTransaction>
)

object MoneyLensExporter {

    private val jsonSerializer = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toExpenseExport(
        purchase: ConfirmedPurchase,
        category: String = "Shopping"
    ): MoneyLensExpenseExport {
        return toExpenseExport(listOf(purchase), category)
    }

    fun toExpenseExport(
        purchases: List<ConfirmedPurchase>,
        category: String = "Shopping"
    ): MoneyLensExpenseExport {
        val now = Instant.now().toString()
        val txs = purchases.map { p ->
            val decimalAmount = BigDecimal(p.payNowAmount.minorUnits)
                .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                .toDouble()

            val itemsSummary = if (p.items.isNotEmpty()) {
                p.items.joinToString(separator = ", ") { "${it.name} (x${it.quantity})" }
            } else {
                "Purchase from ${p.sellerOrPlatform}"
            }

            val title = if (p.items.isNotEmpty()) {
                "${p.sellerOrPlatform}: ${p.items.first().name}" + if (p.items.size > 1) " +${p.items.size - 1} more" else ""
            } else {
                "${p.sellerOrPlatform} Purchase"
            }

            val notesBuilder = StringBuilder()
            notesBuilder.append("Items: $itemsSummary\n")
            notesBuilder.append("Paid Now: ${p.payNowAmount.currency.name} $decimalAmount\n")
            if (p.immediateDiscountsTotal.minorUnits > 0) {
                val discountDecimal = BigDecimal(p.immediateDiscountsTotal.minorUnits)
                    .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                notesBuilder.append("Discounts saved: ${p.immediateDiscountsTotal.currency.name} $discountDecimal\n")
            }
            if (p.futureCashback.minorUnits > 0) {
                val cashbackDecimal = BigDecimal(p.futureCashback.minorUnits)
                    .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                notesBuilder.append("Future rewards/cashback: ${p.futureCashback.currency.name} $cashbackDecimal\n")
            }
            p.receiptNote?.let { note ->
                if (note.isNotBlank()) notesBuilder.append("Notes: $note\n")
            }
            p.receiptImageUri?.let { uri ->
                if (uri.isNotBlank()) notesBuilder.append("Receipt: $uri\n")
            }

            MoneyLensExpenseTransaction(
                category = category,
                title = title,
                amount = decimalAmount,
                currency = p.payNowAmount.currency.name,
                notes = notesBuilder.toString().trim(),
                timestamp = p.timestamp.toString()
            )
        }

        return MoneyLensExpenseExport(
            version = 1,
            source = "DealLens",
            exportedAt = now,
            transactions = txs
        )
    }

    fun snapshotToConfirmedPurchase(
        snapshot: SavedSnapshot,
        sellerOrPlatform: String = "Online Checkout",
        receiptNote: String? = null,
        receiptImageUri: String? = null
    ): ConfirmedPurchase {
        val currency = snapshot.request.items.firstOrNull()?.price?.currency ?: com.deallens.core.domain.CurrencyCode.MYR
        val purchaseItems = snapshot.request.items.map { item ->
            PurchaseItem(
                id = item.id,
                name = if (item.id == "item-1") "Checkout Item" else item.id,
                unitPrice = item.price,
                quantity = item.quantity
            )
        }
        val actualPaid = snapshot.result.amountDue ?: snapshot.request.items.fold(Money(0, currency)) { acc, it ->
            acc + Money(it.price.minorUnits * it.quantity, currency)
        }

        return ConfirmedPurchase(
            purchaseId = java.util.UUID.randomUUID().toString(),
            timestamp = snapshot.calculatedAt,
            items = purchaseItems,
            sellerOrPlatform = sellerOrPlatform,
            payNowAmount = actualPaid,
            originalCurrency = currency,
            reportingCurrency = snapshot.result.reportingAmountDue?.currency ?: currency,
            immediateDiscountsTotal = snapshot.result.immediateDiscounts,
            futureCashback = snapshot.result.futureRewards,
            receiptNote = receiptNote,
            receiptImageUri = receiptImageUri,
            snapshotId = snapshot.id
        )
    }

    fun serializeJson(export: MoneyLensExpenseExport): String {
        return jsonSerializer.encodeToString(export)
    }

    /**
     * Flattened CSV report for personal finance import (e.g. MoneyLens CSV statement import).
     * Escapes spreadsheet formula prefixes (=, +, -, @) per DealLens security spec.
     */
    fun exportPurchasesToCsv(purchases: List<ConfirmedPurchase>): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Merchant,Description,Amount,Currency,DiscountsSaved,FutureCashback,Notes")
        for (p in purchases) {
            val dateStr = p.timestamp.toString().take(10)
            val merchant = escapeCsvCell(p.sellerOrPlatform)
            val desc = escapeCsvCell(p.items.joinToString("; ") { "${it.name} x${it.quantity}" }.ifBlank { "Purchase" })
            val amt = BigDecimal(p.payNowAmount.minorUnits).divide(BigDecimal(100), 2, RoundingMode.HALF_UP).toPlainString()
            val curr = p.payNowAmount.currency.name
            val disc = BigDecimal(p.immediateDiscountsTotal.minorUnits).divide(BigDecimal(100), 2, RoundingMode.HALF_UP).toPlainString()
            val cashback = BigDecimal(p.futureCashback.minorUnits).divide(BigDecimal(100), 2, RoundingMode.HALF_UP).toPlainString()
            val note = escapeCsvCell(p.receiptNote ?: "")
            sb.appendLine("$dateStr,$merchant,$desc,$amt,$curr,$disc,$cashback,$note")
        }
        return sb.toString().trim()
    }

    internal fun escapeCsvCell(raw: String): String {
        val trimmed = raw.replace("\r", " ").replace("\n", " ").trim()
        val formulaSafe = if (trimmed.isNotEmpty() && (trimmed[0] == '=' || trimmed[0] == '+' || trimmed[0] == '-' || trimmed[0] == '@')) {
            "'$trimmed"
        } else {
            trimmed
        }
        return if (formulaSafe.contains(",") || formulaSafe.contains("\"")) {
            "\"${formulaSafe.replace("\"", "\"\"")}\""
        } else {
            formulaSafe
        }
    }
}
