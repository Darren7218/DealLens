package com.deallens.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deallens.core.domain.CurrencyCode
import com.deallens.core.domain.QuoteStatus
import com.deallens.core.domain.ThresholdBasis
import com.deallens.core.domain.VoucherType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val walletVouchers by viewModel.walletVouchers.collectAsState()
    var showWalletPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick scenario test presets
        PresetScenarioChips(viewModel = viewModel)

        // Result Card (Leads with Pay now as requested)
        ResultCard(state = state)

        // Quick Input Fields Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Listing & Price Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Product Name & Platform Selector
                OutlinedTextField(
                    value = state.productNameInput,
                    onValueChange = viewModel::updateProductName,
                    label = { Text("Item / Listing Name (e.g. Galaxy Buds3 Pro)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Shopee", "Lazada", "PDD", "Samsung").forEach { plat ->
                        FilterChip(
                            selected = state.platformInput.equals(plat, ignoreCase = true),
                            onClick = {
                                viewModel.updatePlatform(plat)
                                if (plat == "PDD") {
                                    viewModel.updateCurrency(CurrencyCode.CNY)
                                }
                            },
                            label = { Text(plat) }
                        )
                    }
                }

                // Price and Currency Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.priceInput,
                        onValueChange = viewModel::updatePrice,
                        label = { Text("Item Price") },
                        prefix = {
                            Text(
                                when (state.selectedCurrency) {
                                    CurrencyCode.MYR -> "RM "
                                    CurrencyCode.CNY -> "¥ "
                                    CurrencyCode.USD -> "$ "
                                    CurrencyCode.EUR -> "€ "
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // Currency Toggle (MYR / CNY)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    ) {
                        FilterChip(
                            selected = state.selectedCurrency == CurrencyCode.MYR,
                            onClick = { viewModel.updateCurrency(CurrencyCode.MYR) },
                            label = { Text("MYR") }
                        )
                        FilterChip(
                            selected = state.selectedCurrency == CurrencyCode.CNY,
                            onClick = { viewModel.updateCurrency(CurrencyCode.CNY) },
                            label = { Text("CNY") }
                        )
                    }
                }

                // Quantity Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Quantity", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.updateQuantity(state.quantity - 1) },
                            enabled = state.quantity > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            text = "${state.quantity}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { viewModel.updateQuantity(state.quantity + 1) }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Voucher Rules",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        state.selectedWalletVoucherName?.let { vName ->
                            Text(
                                text = "Applied: $vName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { showWalletPicker = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick from Wallet (${walletVouchers.size})", fontSize = 12.sp)
                    }
                }

                // Voucher Type Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.voucherType == VoucherType.PERCENTAGE,
                        onClick = { viewModel.updateVoucherType(VoucherType.PERCENTAGE) },
                        label = { Text("Percentage (%)") }
                    )
                    FilterChip(
                        selected = state.voucherType == VoucherType.FIXED,
                        onClick = { viewModel.updateVoucherType(VoucherType.FIXED) },
                        label = { Text("Fixed Amount") }
                    )
                }

                // Voucher Value & Cap Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.voucherValueInput,
                        onValueChange = viewModel::updateVoucherValue,
                        label = {
                            Text(if (state.voucherType == VoucherType.PERCENTAGE) "Discount %" else "Discount Value")
                        },
                        suffix = {
                            if (state.voucherType == VoucherType.PERCENTAGE) Text("%")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = state.voucherCapInput,
                        onValueChange = viewModel::updateVoucherCap,
                        label = { Text("Cap (Max RM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = state.voucherType == VoucherType.PERCENTAGE
                    )
                }

                // Minimum Spend & Shipping Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.minSpendInput,
                        onValueChange = viewModel::updateMinSpend,
                        label = { Text("Min Spend") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = state.shippingInput,
                        onValueChange = viewModel::updateShipping,
                        label = { Text("Shipping") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !state.shippingUnknown
                    )
                }

                // Unknown Shipping Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = state.shippingUnknown,
                        onCheckedChange = viewModel::toggleShippingUnknown
                    )
                    Text(
                        text = "Shipping cost is unknown / not provided",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Expandable Panel for Advanced Conditions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Advanced Conditions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = viewModel::toggleAdvancedPanel) {
                        Icon(
                            imageVector = if (state.isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (state.isAdvancedExpanded) "Collapse" else "Expand"
                        )
                    }
                }

                AnimatedVisibility(visible = state.isAdvancedExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Threshold Spend Basis:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.thresholdBasis == ThresholdBasis.ORIGINAL_SUBTOTAL,
                                onClick = { viewModel.updateThresholdBasis(ThresholdBasis.ORIGINAL_SUBTOTAL) },
                                label = { Text("Original Subtotal") }
                            )
                            FilterChip(
                                selected = state.thresholdBasis == ThresholdBasis.AFTER_DISCOUNTS,
                                onClick = { viewModel.updateThresholdBasis(ThresholdBasis.AFTER_DISCOUNTS) },
                                label = { Text("After Prior Discounts") }
                            )
                        }

                        // Exchange Rate (For PDD CNY -> MYR)
                        OutlinedTextField(
                            value = state.exchangeRateInput,
                            onValueChange = viewModel::updateExchangeRate,
                            label = { Text("Exchange Rate to MYR (e.g. 0.65 for CNY)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Future Cashback / Rewards
                        OutlinedTextField(
                            value = state.cashbackRewardInput,
                            onValueChange = viewModel::updateCashbackReward,
                            label = { Text("Future Cashback / Coins (Separate from checkout)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Calculation Explanation Trace Card
        state.quoteResult?.let { result ->
            CalculationTraceCard(result = result)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.saveCalculation() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Calculation Snapshot")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.recordPriceObservation() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Price Observation to History")
        }
    }

    if (showWalletPicker) {
        AlertDialog(
            onDismissRequest = { showWalletPicker = false },
            title = { Text("Select Saved Wallet Voucher") },
            text = {
                if (walletVouchers.isEmpty()) {
                    Text("No saved vouchers in your Wallet yet. Open the Wallet tab to add or load presets.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        walletVouchers.forEach { voucher ->
                            OutlinedCard(
                                onClick = {
                                    viewModel.applyWalletVoucher(voucher)
                                    showWalletPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(voucher.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Platform: ${voucher.platform ?: "Generic"} • v${voucher.currentVersion}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWalletPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ResultCard(state: CalculatorUiState) {
    val result = state.quoteResult

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHECKOUT QUOTE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    letterSpacing = 1.2.sp
                )

                if (result != null) {
                    val isComplete = result.status == QuoteStatus.COMPLETE
                    val badgeColor = if (isComplete) Color(0xFF2E7D32) else Color(0xFFE65100)
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
                    ) {
                        Text(
                            text = if (isComplete) "Complete" else "Incomplete",
                            color = badgeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Lead: Pay now
            val amountDue = result?.amountDue
            if (amountDue != null) {
                Text(
                    text = "Pay now: ${amountDue.formatDisplay()}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // Converted reporting currency if available
                val reportingAmountDue = result.reportingAmountDue
                if (reportingAmountDue != null && reportingAmountDue != amountDue) {
                    Text(
                        text = "≈ ${reportingAmountDue.formatDisplay()} (Reporting Currency)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            } else {
                Text(
                    text = "Pay now: Unknown",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Mathematical summary line: "Item RM800 − voucher RM150 + shipping RM8"
            if (state.formulaSummary.isNotBlank()) {
                Text(
                    text = state.formulaSummary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            // Contextual observations
            state.contextualNotes.forEach { note ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // Separate line for Future Cashback as strictly specified
            if (result != null && result.futureRewards.minorUnits > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Future cashback: ${result.futureRewards.formatDisplay()} (Requires full checkout payment upfront)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresetScenarioChips(viewModel: CalculatorViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Quick Fixture Presets:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilledTonalButton(
                onClick = viewModel::loadFixture1,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("RM800 @ 22%", fontSize = 12.sp)
            }
            FilledTonalButton(
                onClick = viewModel::loadFixture2,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("RM800 @ 30%", fontSize = 12.sp)
            }
            FilledTonalButton(
                onClick = viewModel::loadFixtureCashback,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Cashback", fontSize = 12.sp)
            }
            FilledTonalButton(
                onClick = viewModel::loadFixturePddCny,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("PDD CNY", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CalculationTraceCard(result: com.deallens.core.domain.QuoteResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Calculation Trace (Pricing Engine)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            result.trace.forEach { step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    step.amount?.let { amt ->
                        Text(
                            text = amt.formatDisplay(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (result.rejectedVouchers.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                result.rejectedVouchers.forEach { (voucher, reason) ->
                    Text(
                        text = "Voucher ${voucher.id} ineligible: $reason",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
