package com.deallens.core.pricing

import com.deallens.core.pricing.parser.OcrTextParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OcrTextParserTest {

    @Test
    fun `extracts Shopee price and detects installment trap`() {
        val blocks = listOf(
            "Shopee",
            "RM 1,299.00",
            "RM 108.25 /bln x 12",
            "SPayLater"
        )
        
        val draft = OcrTextParser.parseScreenshot(blocks)
        assertEquals("Shopee", draft.platform)
        
        val mainPrice = draft.candidatePrices.find { it.amountMinorUnits == 129900L }
        assertNotNull(mainPrice)
        assertEquals(false, mainPrice?.isProbableInstallment)
        assertEquals("MYR", mainPrice?.currencyCode)
        
        val installmentPrice = draft.candidatePrices.find { it.amountMinorUnits == 10825L }
        assertNotNull(installmentPrice)
        assertEquals(true, installmentPrice?.isProbableInstallment)
    }

    @Test
    fun `extracts PDD screenshot with CNY amounts`() {
        val blocks = listOf(
            "Pinduoduo",
            "¥ 599.50",
            "Free shipping"
        )
        
        val draft = OcrTextParser.parseScreenshot(blocks)
        assertEquals("PDD", draft.platform)
        
        val price = draft.candidatePrices.first()
        assertEquals(59950L, price.amountMinorUnits)
        assertEquals("CNY", price.currencyCode)
    }

    @Test
    fun `fallback to editable empty draft if no prices recognized`() {
        val blocks = listOf(
            "Just some random text",
            "No prices here",
            "Hello world"
        )
        
        val draft = OcrTextParser.parseScreenshot(blocks)
        assertTrue(draft.candidatePrices.isEmpty())
        assertTrue(draft.warnings.contains("No prices found. Please enter manually."))
    }
}
