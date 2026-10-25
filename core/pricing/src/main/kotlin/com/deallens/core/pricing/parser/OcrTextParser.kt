package com.deallens.core.pricing.parser

import com.deallens.core.domain.parser.ParsedPrice
import com.deallens.core.domain.parser.ReviewDraft
import java.math.BigDecimal

object OcrTextParser {

    private val rmMoneyRegex = Regex("""(?:RM|MYR)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    private val cnyMoneyRegex = Regex("""(?:¥|CNY)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    
    private val installmentTraps = listOf("/bln", "x 12", "x3", "x6", "installment", "spaylater")
    private val strikeThroughTraps = listOf("rrp", "original price") // Hard to detect purely by text without formatting
    
    fun parseScreenshot(blocks: List<String>): ReviewDraft {
        var platform: String? = null
        val prices = mutableListOf<ParsedPrice>()
        val shipping = mutableListOf<ParsedPrice>()
        val vouchers = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        val fullText = blocks.joinToString("\n").lowercase()
        
        if (fullText.contains("shopee")) platform = "Shopee"
        else if (fullText.contains("lazada")) platform = "Lazada"
        else if (fullText.contains("pinduoduo") || fullText.contains("pdd")) platform = "PDD"

        for (block in blocks) {
            val lowerBlock = block.lowercase()
            
            // Check for installment traps
            val isInstallment = installmentTraps.any { lowerBlock.contains(it) }
            val isOriginal = strikeThroughTraps.any { lowerBlock.contains(it) }
            
            // Extract RM prices
            rmMoneyRegex.findAll(block).forEach { match ->
                val amountStr = match.groupValues[1].replace(",", "")
                val minorUnits = BigDecimal(amountStr).multiply(BigDecimal(100)).toLong()
                prices.add(ParsedPrice(minorUnits, "MYR", match.value, isInstallment, isOriginal))
            }
            
            // Extract CNY prices
            cnyMoneyRegex.findAll(block).forEach { match ->
                val amountStr = match.groupValues[1].replace(",", "")
                val minorUnits = BigDecimal(amountStr).multiply(BigDecimal(100)).toLong()
                prices.add(ParsedPrice(minorUnits, "CNY", match.value, isInstallment, isOriginal))
            }
            
            // Very naive shipping/voucher check
            if (lowerBlock.contains("shipping") || lowerBlock.contains("cukai") || lowerBlock.contains("fee")) {
                rmMoneyRegex.findAll(block).forEach { match ->
                    val amountStr = match.groupValues[1].replace(",", "")
                    val minorUnits = BigDecimal(amountStr).multiply(BigDecimal(100)).toLong()
                    shipping.add(ParsedPrice(minorUnits, "MYR", match.value, false, false))
                }
            }
            
            if (lowerBlock.contains("voucher") || lowerBlock.contains("off") || lowerBlock.contains("%")) {
                if (lowerBlock.length < 50) vouchers.add(block)
            }
        }
        
        if (prices.isEmpty()) {
            warnings.add("No prices found. Please enter manually.")
        }
        
        return ReviewDraft(
            platform = platform,
            candidatePrices = prices.distinctBy { it.amountMinorUnits },
            shippingCandidates = shipping.distinctBy { it.amountMinorUnits },
            voucherTerms = vouchers,
            warnings = warnings
        )
    }
}
