package com.deallens.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deallens.core.data.export.MoneyLensExpenseExport
import com.deallens.core.data.export.MoneyLensExporter
import com.deallens.core.data.local.DealLensDatabase
import com.deallens.core.data.repository.RoomPurchaseRepository
import com.deallens.core.domain.ConfirmedPurchase
import com.deallens.core.domain.CurrencyCode
import com.deallens.core.domain.Money
import com.deallens.core.domain.PurchaseItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MoneyLensExportTest {

    private lateinit var db: DealLensDatabase
    private lateinit var purchaseRepository: RoomPurchaseRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DealLensDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        purchaseRepository = RoomPurchaseRepository(db.purchaseDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `test confirmed purchase serializes into exact MoneyLensExpenseExport contract`() {
        val testInstant = Instant.parse("2026-10-10T12:00:00Z")
        val purchase = ConfirmedPurchase(
            purchaseId = "pur-12345",
            timestamp = testInstant,
            items = listOf(
                PurchaseItem(
                    id = "item-1",
                    name = "Logitech MX Master 3S",
                    unitPrice = Money(38900, CurrencyCode.MYR),
                    quantity = 1
                ),
                PurchaseItem(
                    id = "item-2",
                    name = "Mechanical Keyboard Keycaps",
                    unitPrice = Money(4500, CurrencyCode.MYR),
                    quantity = 2
                )
            ),
            sellerOrPlatform = "Shopee Official Store",
            payNowAmount = Money(42900, CurrencyCode.MYR), // 429.00 MYR
            originalCurrency = CurrencyCode.MYR,
            reportingCurrency = CurrencyCode.MYR,
            immediateDiscountsTotal = Money(5000, CurrencyCode.MYR), // 50.00 MYR saved
            futureCashback = Money(1500, CurrencyCode.MYR), // 15.00 MYR rewards
            receiptNote = "10.10 Double Digit Flash Sale",
            receiptImageUri = "content://media/external/images/media/42"
        )

        val export = MoneyLensExporter.toExpenseExport(purchase, category = "Electronics")
        val jsonString = MoneyLensExporter.serializeJson(export)

        // Parse with JsonElement to verify exact keys and schema contract
        val jsonElement = Json.parseToJsonElement(jsonString) as JsonObject

        // Check top-level envelope contract
        assertTrue("Must contain version", jsonElement.containsKey("version"))
        assertEquals(1, jsonElement["version"]?.jsonPrimitive?.int)
        assertTrue("Must contain source", jsonElement.containsKey("source"))
        assertEquals("DealLens", jsonElement["source"]?.jsonPrimitive?.content)
        assertTrue("Must contain exportedAt", jsonElement.containsKey("exportedAt"))
        assertTrue("Must contain transactions array", jsonElement.containsKey("transactions"))

        val txArray = jsonElement["transactions"]?.jsonArray
        assertNotNull(txArray)
        assertEquals(1, txArray!!.size)

        val tx = txArray[0].jsonObject

        // Verify exact contract fields: category, title, amount, currency, notes, timestamp
        assertTrue("Contract field 'category' required", tx.containsKey("category"))
        assertEquals("Electronics", tx["category"]?.jsonPrimitive?.content)

        assertTrue("Contract field 'title' required", tx.containsKey("title"))
        val title = tx["title"]?.jsonPrimitive?.content ?: ""
        assertTrue(title.contains("Shopee Official Store"))
        assertTrue(title.contains("Logitech MX Master 3S"))

        assertTrue("Contract field 'amount' required", tx.containsKey("amount"))
        assertEquals(429.00, tx["amount"]?.jsonPrimitive?.double ?: 0.0, 0.001)

        assertTrue("Contract field 'currency' required", tx.containsKey("currency"))
        assertEquals("MYR", tx["currency"]?.jsonPrimitive?.content)

        assertTrue("Contract field 'notes' required", tx.containsKey("notes"))
        val notes = tx["notes"]?.jsonPrimitive?.content ?: ""
        assertTrue(notes.contains("Logitech MX Master 3S (x1)"))
        assertTrue(notes.contains("Mechanical Keyboard Keycaps (x2)"))
        assertTrue(notes.contains("Discounts saved: MYR 50.00"))
        assertTrue(notes.contains("Future rewards/cashback: MYR 15.00"))
        assertTrue(notes.contains("Notes: 10.10 Double Digit Flash Sale"))
        assertTrue(notes.contains("Receipt: content://media/external/images/media/42"))

        assertTrue("Contract field 'timestamp' required", tx.containsKey("timestamp"))
        assertEquals("2026-10-10T12:00:00Z", tx["timestamp"]?.jsonPrimitive?.content)
    }

    @Test
    fun `test purchase repository save and load roundtrip`() = runBlocking {
        val purchase = ConfirmedPurchase(
            purchaseId = "pur-room-1",
            timestamp = Instant.now(),
            items = listOf(
                PurchaseItem(
                    id = "i-1",
                    name = "Coffee Beans 1kg",
                    unitPrice = Money(6500, CurrencyCode.MYR),
                    quantity = 2
                )
            ),
            sellerOrPlatform = "Lazada Roaster",
            payNowAmount = Money(11000, CurrencyCode.MYR),
            originalCurrency = CurrencyCode.MYR,
            reportingCurrency = CurrencyCode.MYR,
            immediateDiscountsTotal = Money(2000, CurrencyCode.MYR),
            futureCashback = Money(0, CurrencyCode.MYR),
            receiptNote = "Morning Brew Stock"
        )

        purchaseRepository.savePurchase(purchase)

        val retrieved = purchaseRepository.getPurchaseById("pur-room-1")
        assertNotNull(retrieved)
        assertEquals("pur-room-1", retrieved!!.purchaseId)
        assertEquals("Lazada Roaster", retrieved.sellerOrPlatform)
        assertEquals(11000L, retrieved.payNowAmount.minorUnits)
        assertEquals(1, retrieved.items.size)
        assertEquals("Coffee Beans 1kg", retrieved.items[0].name)
        assertEquals(2, retrieved.items[0].quantity)
        assertEquals("Morning Brew Stock", retrieved.receiptNote)

        val allPurchases = purchaseRepository.getAllPurchases().first()
        assertEquals(1, allPurchases.size)
    }

    @Test
    fun `test CSV export escapes spreadsheet formula prefixes and quotes commas`() {
        val testInstant = Instant.parse("2026-10-12T08:30:00Z")
        val purchase = ConfirmedPurchase(
            purchaseId = "pur-csv-1",
            timestamp = testInstant,
            items = listOf(
                PurchaseItem(
                    id = "item-csv",
                    name = "=SUM(A1:A10), Special Edition",
                    unitPrice = Money(9900, CurrencyCode.MYR),
                    quantity = 1
                )
            ),
            sellerOrPlatform = "+ShopeeMall",
            payNowAmount = Money(8900, CurrencyCode.MYR),
            originalCurrency = CurrencyCode.MYR,
            reportingCurrency = CurrencyCode.MYR,
            immediateDiscountsTotal = Money(1000, CurrencyCode.MYR),
            futureCashback = Money(500, CurrencyCode.MYR),
            receiptNote = "@PromoNote"
        )

        val csv = MoneyLensExporter.exportPurchasesToCsv(listOf(purchase))
        val lines = csv.lines()
        assertEquals(2, lines.size)
        assertEquals("Date,Merchant,Description,Amount,Currency,DiscountsSaved,FutureCashback,Notes", lines[0])

        val dataRow = lines[1]
        // Leading '+' on platform should be prefixed with single quote
        assertTrue("Formula prefix '+' should be escaped", dataRow.contains("'+ShopeeMall"))
        // Leading '=' and comma in description should be formula-escaped and wrapped in quotes
        assertTrue("Commas and '=' should be escaped and wrapped in quotes", dataRow.contains("\"'=SUM(A1:A10), Special Edition x1\""))
        assertTrue("Leading '@' in note should be escaped", dataRow.contains("'@PromoNote"))
        assertTrue("PayNow formatted amount", dataRow.contains(",89.00,MYR,10.00,5.00,"))
    }
}
