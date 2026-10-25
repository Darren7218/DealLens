package com.deallens.app.ui

import androidx.lifecycle.ViewModel
import com.deallens.core.domain.*
import com.deallens.core.pricing.PricingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale
import androidx.lifecycle.viewModelScope
import com.deallens.core.domain.repository.SavedSnapshot
import com.deallens.core.domain.repository.SnapshotRepository
import com.deallens.core.domain.repository.ProductRepository
import com.deallens.core.domain.repository.VoucherProfile
import com.deallens.core.domain.repository.VoucherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class AppTab(val label: String) {
    CALCULATE("Calculate"),
    COMPARE("Compare"),
    WALLET("Wallet"),
    SAVED("Saved"),
    CAPTURE("Capture")
}

data class CalculatorUiState(
    val currentTab: AppTab = AppTab.CALCULATE,
    // Quick calculator inputs
    val productNameInput: String = "",
    val platformInput: String = "Shopee",
    val priceInput: String = "800",
    val selectedCurrency: CurrencyCode = CurrencyCode.MYR,
    val quantity: Int = 1,
    val selectedWalletVoucherName: String? = null,
    val selectedWalletVoucherId: String? = null,
    val selectedWalletVoucherVersion: Int = 1,
    val voucherType: VoucherType = VoucherType.PERCENTAGE,
    val voucherValueInput: String = "22",
    val voucherCapInput: String = "150",
    val minSpendInput: String = "350",
    val shippingInput: String = "8",
    val shippingUnknown: Boolean = false,
    
    // Advanced conditions panel
    val isAdvancedExpanded: Boolean = false,
    val thresholdBasis: ThresholdBasis = ThresholdBasis.ORIGINAL_SUBTOTAL,
    val exchangeRateInput: String = "0.65",
    val reportingCurrency: CurrencyCode = CurrencyCode.MYR,
    val cashbackRewardInput: String = "",
    val platformNotes: String = "Observed today; availability not checked.",

    // Calculation result
    val quoteResult: QuoteResult? = null,
    val formulaSummary: String = "",
    val contextualNotes: List<String> = emptyList()
)

fun Money.formatDisplay(): String {
    val symbol = when (currency) {
        CurrencyCode.MYR -> "RM"
        CurrencyCode.CNY -> "¥"
        CurrencyCode.USD -> "$"
        CurrencyCode.EUR -> "€"
    }
    val major = minorUnits / 100.0
    val formattedNumber = String.format(Locale.US, "%.2f", major)
    return if (formattedNumber.endsWith(".00")) {
        "$symbol${formattedNumber.removeSuffix(".00")}"
    } else {
        "$symbol$formattedNumber"
    }
}

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val snapshotRepository: SnapshotRepository,
    private val productRepository: ProductRepository,
    private val voucherRepository: VoucherRepository
) : ViewModel() {
    private val pricingEngine: PricingEngine = PricingEngine()

    val walletVouchers = voucherRepository.getAllVouchers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        recalculate()
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun updateProductName(name: String) {
        _uiState.update { it.copy(productNameInput = name) }
    }

    fun updatePlatform(platform: String) {
        _uiState.update { it.copy(platformInput = platform) }
    }

    fun applyWalletVoucher(voucher: VoucherProfile) {
        val rule = voucher.latestRule ?: return
        val capStr = rule.cap?.let { (it.minorUnits / 100.0).toString().removeSuffix(".0") } ?: ""
        val minStr = rule.minimumSpend?.let { (it.minorUnits / 100.0).toString().removeSuffix(".0") } ?: ""
        val ruleCurrency = rule.cap?.currency ?: rule.minimumSpend?.currency ?: _uiState.value.selectedCurrency

        _uiState.update {
            it.copy(
                currentTab = AppTab.CALCULATE,
                selectedWalletVoucherName = "${voucher.name} (v${voucher.currentVersion})",
                selectedWalletVoucherId = voucher.id,
                selectedWalletVoucherVersion = voucher.currentVersion,
                selectedCurrency = ruleCurrency,
                platformInput = voucher.platform ?: it.platformInput,
                voucherType = if (rule.type == VoucherType.REWARD) VoucherType.PERCENTAGE else rule.type,
                voucherValueInput = if (rule.type == VoucherType.REWARD) "0" else rule.value.toPlainString(),
                voucherCapInput = capStr,
                minSpendInput = minStr,
                thresholdBasis = rule.thresholdBasis,
                cashbackRewardInput = if (rule.type == VoucherType.REWARD) rule.value.toPlainString() else it.cashbackRewardInput
            )
        }
        recalculate()
    }

    fun updatePrice(price: String) {
        _uiState.update { it.copy(priceInput = price) }
        recalculate()
    }

    fun updateCurrency(currency: CurrencyCode) {
        _uiState.update { it.copy(selectedCurrency = currency) }
        recalculate()
    }

    fun updateQuantity(quantity: Int) {
        if (quantity >= 1) {
            _uiState.update { it.copy(quantity = quantity) }
            recalculate()
        }
    }

    fun updateVoucherType(type: VoucherType) {
        _uiState.update { it.copy(voucherType = type) }
        recalculate()
    }

    fun updateVoucherValue(value: String) {
        _uiState.update { it.copy(voucherValueInput = value) }
        recalculate()
    }

    fun updateVoucherCap(cap: String) {
        _uiState.update { it.copy(voucherCapInput = cap) }
        recalculate()
    }

    fun updateMinSpend(minSpend: String) {
        _uiState.update { it.copy(minSpendInput = minSpend) }
        recalculate()
    }

    fun updateShipping(shipping: String) {
        _uiState.update { it.copy(shippingInput = shipping) }
        recalculate()
    }

    fun toggleShippingUnknown(isUnknown: Boolean) {
        _uiState.update { it.copy(shippingUnknown = isUnknown) }
        recalculate()
    }

    fun toggleAdvancedPanel() {
        _uiState.update { it.copy(isAdvancedExpanded = !it.isAdvancedExpanded) }
    }

    fun updateThresholdBasis(basis: ThresholdBasis) {
        _uiState.update { it.copy(thresholdBasis = basis) }
        recalculate()
    }

    fun updateExchangeRate(rate: String) {
        _uiState.update { it.copy(exchangeRateInput = rate) }
        recalculate()
    }

    fun updateCashbackReward(cashback: String) {
        _uiState.update { it.copy(cashbackRewardInput = cashback) }
        recalculate()
    }

    fun loadFixture1() {
        // RM800, 22% capped at RM150, min RM350, shipping RM8
        _uiState.update {
            it.copy(
                priceInput = "800",
                selectedCurrency = CurrencyCode.MYR,
                quantity = 1,
                voucherType = VoucherType.PERCENTAGE,
                voucherValueInput = "22",
                voucherCapInput = "150",
                minSpendInput = "350",
                shippingInput = "8",
                shippingUnknown = false,
                cashbackRewardInput = "",
                thresholdBasis = ThresholdBasis.ORIGINAL_SUBTOTAL
            )
        }
        recalculate()
    }

    fun loadFixture2() {
        // RM800, 30% capped at RM80, min RM100, shipping RM8
        _uiState.update {
            it.copy(
                priceInput = "800",
                selectedCurrency = CurrencyCode.MYR,
                quantity = 1,
                voucherType = VoucherType.PERCENTAGE,
                voucherValueInput = "30",
                voucherCapInput = "80",
                minSpendInput = "100",
                shippingInput = "8",
                shippingUnknown = false,
                cashbackRewardInput = "",
                thresholdBasis = ThresholdBasis.ORIGINAL_SUBTOTAL
            )
        }
        recalculate()
    }

    fun loadFixtureCashback() {
        // RM500 due with RM50 future cashback
        _uiState.update {
            it.copy(
                priceInput = "500",
                selectedCurrency = CurrencyCode.MYR,
                quantity = 1,
                voucherType = VoucherType.PERCENTAGE,
                voucherValueInput = "0",
                voucherCapInput = "",
                minSpendInput = "",
                shippingInput = "0",
                shippingUnknown = false,
                cashbackRewardInput = "50",
                isAdvancedExpanded = true
            )
        }
        recalculate()
    }

    fun loadFixturePddCny() {
        // PDD CNY conversion: e.g. 1000 CNY at rate 0.65 MYR
        _uiState.update {
            it.copy(
                priceInput = "1000",
                selectedCurrency = CurrencyCode.CNY,
                quantity = 1,
                voucherType = VoucherType.PERCENTAGE,
                voucherValueInput = "10",
                voucherCapInput = "100",
                minSpendInput = "500",
                shippingInput = "20",
                shippingUnknown = false,
                exchangeRateInput = "0.65",
                isAdvancedExpanded = true
            )
        }
        recalculate()
    }

    private fun parseAmountToMinor(input: String): Long? {
        val clean = input.trim()
        if (clean.isEmpty()) return null
        return try {
            val bd = BigDecimal(clean)
            bd.multiply(BigDecimal(100)).toLong()
        } catch (_: Exception) {
            null
        }
    }

    fun applyOcrDraft(
        draft: com.deallens.core.domain.parser.ReviewDraft,
        selectedPrice: com.deallens.core.domain.parser.ParsedPrice? = null,
        itemName: String = ""
    ) {
        val candidate = selectedPrice ?: draft.candidatePrices.firstOrNull { !it.isProbableInstallment }
        val priceInput = if (candidate != null) (candidate.amountMinorUnits / 100.0).toString().removeSuffix(".0") else ""
        val currency = if (candidate?.currencyCode == "CNY") CurrencyCode.CNY else CurrencyCode.MYR
        
        val shipCandidate = draft.shippingCandidates.firstOrNull()
        val shipInput = if (shipCandidate != null) (shipCandidate.amountMinorUnits / 100.0).toString().removeSuffix(".0") else "0"

        _uiState.update {
            it.copy(
                productNameInput = itemName.ifBlank { it.productNameInput },
                platformInput = draft.platform ?: it.platformInput,
                priceInput = priceInput,
                selectedCurrency = currency,
                shippingInput = shipInput,
                platformNotes = "Imported from ${draft.platform ?: "Screenshot"}"
            )
        }
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        val itemPriceMinor = parseAmountToMinor(state.priceInput)

        if (itemPriceMinor == null || itemPriceMinor <= 0) {
            _uiState.update {
                it.copy(
                    quoteResult = null,
                    formulaSummary = "Enter a valid item price",
                    contextualNotes = emptyList()
                )
            }
            return
        }

        val currency = state.selectedCurrency
        val itemId = state.productNameInput.trim().ifBlank { "item-1" }
        val items = listOf(
            CartLine(
                id = itemId,
                price = Money(itemPriceMinor, currency),
                quantity = state.quantity
            )
        )

        val vouchers = mutableListOf<VoucherRuleVersion>()
        val voucherVal = state.voucherValueInput.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
        val voucherRuleId = state.selectedWalletVoucherId ?: "voucher-main"
        if (voucherVal > BigDecimal.ZERO) {
            val capMinor = parseAmountToMinor(state.voucherCapInput)
            val minSpendMinor = parseAmountToMinor(state.minSpendInput)
            vouchers.add(
                VoucherRuleVersion(
                    id = voucherRuleId,
                    version = state.selectedWalletVoucherVersion,
                    type = state.voucherType,
                    value = voucherVal,
                    cap = capMinor?.let { Money(it, currency) },
                    minimumSpend = minSpendMinor?.let { Money(it, currency) },
                    thresholdBasis = state.thresholdBasis
                )
            )
        }

        // Additional cashback reward voucher if entered in advanced panel
        val cashbackRewardVal = state.cashbackRewardInput.trim().toBigDecimalOrNull()
        if (cashbackRewardVal != null && cashbackRewardVal > BigDecimal.ZERO) {
            vouchers.add(
                VoucherRuleVersion(
                    id = "reward-cashback",
                    version = 1,
                    type = VoucherType.REWARD,
                    value = cashbackRewardVal,
                    cap = null,
                    minimumSpend = null
                )
            )
        }

        val shippingComponent = if (state.shippingUnknown) {
            CostComponent(
                type = ComponentType.SHIPPING,
                amount = null,
                isUnknown = true
            )
        } else {
            val shipMinor = parseAmountToMinor(state.shippingInput) ?: 0L
            CostComponent(
                type = ComponentType.SHIPPING,
                amount = Money(shipMinor, currency),
                isUnknown = false
            )
        }

        val exchangeRateSnapshot = state.exchangeRateInput.trim().toBigDecimalOrNull()?.let { rate ->
            if (rate > BigDecimal.ZERO) {
                ExchangeRateSnapshot(
                    fromCurrency = currency,
                    toCurrency = state.reportingCurrency,
                    rate = rate,
                    capturedAt = Instant.now()
                )
            } else null
        }

        val request = QuoteRequest(
            items = items,
            voucherRules = vouchers,
            shipping = shippingComponent,
            exchangeRate = exchangeRateSnapshot,
            evaluatedAt = Instant.now()
        )
        currentRequest = request

        val result = pricingEngine.calculate(request)

        // Build dynamic contextual explanation matching DealLens specification
        val notes = mutableListOf<String>()
        val formulaParts = mutableListOf<String>()

        val rawSubtotal = Money(itemPriceMinor * state.quantity, currency)
        formulaParts.add("Item ${rawSubtotal.formatDisplay()}")

        if (result.immediateDiscounts.minorUnits > 0) {
            formulaParts.add("voucher −${result.immediateDiscounts.formatDisplay()}")
            // Check if voucher cap was hit
            val cap = vouchers.firstOrNull { it.id == voucherRuleId }?.cap
            if (cap != null && result.immediateDiscounts.minorUnits == cap.minorUnits) {
                notes.add("Voucher cap reached.")
            }
        }

        if (state.shippingUnknown) {
            notes.add("Shipping cost is unknown; total is conditional/incomplete.")
        } else {
            val ship = shippingComponent.amount ?: Money(0, currency)
            if (ship.minorUnits > 0) {
                formulaParts.add("+ shipping ${ship.formatDisplay()}")
                notes.add("Shipping entered manually.")
            }
        }

        if (state.platformNotes.isNotBlank()) {
            notes.add(state.platformNotes)
        }

        if (result.futureRewards.minorUnits > 0) {
            notes.add("Future cashback belongs on a separate line (does not reduce checkout amount).")
        }

        val summaryFormula = formulaParts.joinToString(" ")

        _uiState.update {
            it.copy(
                quoteResult = result,
                formulaSummary = summaryFormula,
                contextualNotes = notes
            )
        }
    }

    private var currentRequest: QuoteRequest? = null

    fun saveCalculation() {
        val req = currentRequest ?: return
        val res = uiState.value.quoteResult ?: return
        viewModelScope.launch {
            val snapshot = SavedSnapshot(
                id = UUID.randomUUID().toString(),
                scenarioId = uiState.value.platformInput.ifBlank { "Shopee" },
                request = req,
                result = res,
                calculatedAt = Instant.now()
            )
            snapshotRepository.saveSnapshot(snapshot)
            selectTab(AppTab.SAVED)
        }
    }

    fun recordPriceObservation(productName: String = "") {
        val state = uiState.value
        val priceMinor = parseAmountToMinor(state.priceInput) ?: return
        val currency = state.selectedCurrency
        val finalName = productName.ifBlank { state.productNameInput.ifBlank { "Recorded Item" } }
        viewModelScope.launch {
            productRepository.saveObservation(
                productName = finalName,
                platform = state.platformInput.ifBlank { "Shopee" },
                price = Money(priceMinor, currency)
            )
            selectTab(AppTab.SAVED)
        }
    }
}
