package com.deallens.core.domain.parser

data class ReviewDraft(
    val platform: String?, // "Shopee", "Lazada", "PDD"
    val candidatePrices: List<ParsedPrice>,
    val shippingCandidates: List<ParsedPrice>,
    val voucherTerms: List<String>,
    val warnings: List<String>
)

data class ParsedPrice(
    val amountMinorUnits: Long,
    val currencyCode: String,
    val sourceText: String,
    val isProbableInstallment: Boolean,
    val isProbableOriginalPrice: Boolean
)
