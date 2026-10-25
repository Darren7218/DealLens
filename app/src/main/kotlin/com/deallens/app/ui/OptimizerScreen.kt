package com.deallens.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deallens.core.domain.*
import com.deallens.core.domain.repository.SavedSnapshot
import com.deallens.core.pricing.*
import java.math.BigDecimal
import java.util.UUID

enum class CompareMode(val label: String) {
    SIDE_BY_SIDE("Side-by-Side Quotes"),
    CART_OPTIMIZER("Multi-Order Optimizer")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizerScreen(
    savedViewModel: SavedViewModel = hiltViewModel(),
    walletViewModel: WalletViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val optimizer = remember { CartOptimizer() }

    val savedSnapshots by savedViewModel.snapshots.collectAsState()
    val walletVouchers by walletViewModel.vouchers.collectAsState()

    var mode by remember { mutableStateOf(CompareMode.SIDE_BY_SIDE) }

    // Interactive Cart Optimizer state
    val cartItems = remember {
        mutableStateListOf(
            CartItem("1", "Wireless Mouse", "Shopee", Money(15000, CurrencyCode.MYR)),
            CartItem("2", "Mechanical Keyboard", "Lazada", Money(35000, CurrencyCode.MYR)),
            CartItem("3", "Desk Mat XL", "Shopee", Money(5000, CurrencyCode.MYR))
        )
    }

    var showAddCartItemDialog by remember { mutableStateOf(false) }

    // Combine wallet vouchers + default fallback vouchers if wallet is empty
    val activeCartVouchers = remember(walletVouchers) {
        if (walletVouchers.isNotEmpty()) {
            walletVouchers.mapNotNull { profile ->
                profile.latestRule?.let { rule ->
                    CartVoucher(
                        voucher = rule,
                        sellerId = if (profile.platform.equals("All Platforms", ignoreCase = true)) null else profile.platform,
                        isReusable = false,
                        isStackable = true
                    )
                }
            }
        } else {
            listOf(
                CartVoucher(
                    voucher = VoucherRuleVersion("v1", 1, VoucherType.PERCENTAGE, BigDecimal("20"), cap = Money(3000, CurrencyCode.MYR)),
                    isReusable = false,
                    isStackable = true
                ),
                CartVoucher(
                    voucher = VoucherRuleVersion("v2", 1, VoucherType.FIXED, BigDecimal("15"), minimumSpend = Money(10000, CurrencyCode.MYR)),
                    sellerId = "Shopee",
                    isReusable = false,
                    isStackable = true
                )
            )
        }
    }

    val request = remember(cartItems.toList(), activeCartVouchers) {
        CartOptimizationRequest(cartItems.toList(), activeCartVouchers)
    }
    val result = remember(request) { optimizer.optimize(request) }

    var jsonToExportSaf by remember { mutableStateOf<String?>(null) }
    var moneyLensExportJsonForDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    val moneyLensFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { dest ->
            jsonToExportSaf?.let { json ->
                try {
                    context.contentResolver.openOutputStream(dest)?.use { out ->
                        out.write(json.toByteArray())
                    }
                } catch (_: Exception) {}
            }
        }
        jsonToExportSaf = null
    }

    Scaffold(
        floatingActionButton = {
            if (mode == CompareMode.CART_OPTIMIZER) {
                ExtendedFloatingActionButton(
                    onClick = { showAddCartItemDialog = true },
                    icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = "Add Item") },
                    text = { Text("Add Cart Item") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = mode.ordinal) {
                CompareMode.values().forEach { tabMode ->
                    Tab(
                        selected = mode == tabMode,
                        onClick = { mode = tabMode },
                        text = { Text(tabMode.label) }
                    )
                }
            }

            when (mode) {
                CompareMode.SIDE_BY_SIDE -> {
                    SideBySideComparisonView(
                        snapshots = savedSnapshots,
                        onConfirmPurchase = { snap ->
                            savedViewModel.confirmPurchaseFromSnapshot(
                                snapshot = snap,
                                sellerOrPlatform = snap.scenarioId ?: "Online Store"
                            )
                        },
                        onExportMoneyLens = { snap ->
                            val json = savedViewModel.generateMoneyLensExportJsonFromSnapshot(
                                snap,
                                platform = snap.scenarioId ?: "Online Store"
                            )
                            moneyLensExportJsonForDialog = Pair("Comparison Quote", json)
                        }
                    )
                }

                CompareMode.CART_OPTIMIZER -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Optimal Basket Strategy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Badge {
                                            Text("${activeCartVouchers.size} Vouchers Pool")
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(result.explanation, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        "Total Pay Now: ${result.totalAmountDue.formatDisplay()}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        "Total Immediate Discount: ${result.totalDiscount.formatDisplay()}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Basket Items (${cartItems.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Tap trash icon to test removing items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        items(cartItems, key = { it.id }) { item ->
                            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${item.sellerId} • ${item.price.formatDisplay()}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (cartItems.size > 1) {
                                        IconButton(onClick = { cartItems.remove(item) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Recommended Checkout Splits (${result.splits.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        items(result.splits) { split ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    val sellerName = split.items.firstOrNull()?.sellerId ?: "Order"
                                    Text("Order Group ($sellerName • ${split.items.size} items)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    split.items.forEach { item ->
                                        Text("• ${item.name} (${item.price.formatDisplay()}) — ${item.sellerId}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text("Applied Vouchers: ${split.appliedVouchers.size} (${split.immediateDiscount.formatDisplay()} saved)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    Text("Order Pay Now: ${split.amountDue.formatDisplay()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val purchase = ConfirmedPurchase(
                                                    purchaseId = UUID.randomUUID().toString(),
                                                    items = split.items.map { PurchaseItem(id = it.id, name = it.name, unitPrice = it.price, quantity = it.quantity) },
                                                    sellerOrPlatform = sellerName,
                                                    payNowAmount = split.amountDue,
                                                    originalCurrency = split.amountDue.currency,
                                                    immediateDiscountsTotal = split.immediateDiscount,
                                                    receiptNote = "Optimized Order Split"
                                                )
                                                savedViewModel.saveDirectPurchase(purchase)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Confirm", style = MaterialTheme.typography.labelSmall)
                                        }

                                        Button(
                                            onClick = {
                                                val purchase = ConfirmedPurchase(
                                                    purchaseId = UUID.randomUUID().toString(),
                                                    items = split.items.map { PurchaseItem(id = it.id, name = it.name, unitPrice = it.price, quantity = it.quantity) },
                                                    sellerOrPlatform = sellerName,
                                                    payNowAmount = split.amountDue,
                                                    originalCurrency = split.amountDue.currency,
                                                    immediateDiscountsTotal = split.immediateDiscount,
                                                    receiptNote = "Optimized Order Split"
                                                )
                                                val json = savedViewModel.generateMoneyLensExportJson(purchase)
                                                moneyLensExportJsonForDialog = Pair(sellerName, json)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("MoneyLens", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Marginal Value Check ('Is adding this item worth it?')", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        items(result.marginalEvaluations) { eval ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("Item: ${eval.item.name} (${eval.item.price.formatDisplay()})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(eval.explanation, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        item {
                            Spacer(Modifier.height(64.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddCartItemDialog) {
        AddCartItemDialog(
            onDismiss = { showAddCartItemDialog = false },
            onAdd = { name, seller, priceMinor ->
                cartItems.add(
                    CartItem(
                        id = UUID.randomUUID().toString().take(6),
                        name = name,
                        sellerId = seller,
                        price = Money(priceMinor, CurrencyCode.MYR)
                    )
                )
                showAddCartItemDialog = false
            }
        )
    }

    moneyLensExportJsonForDialog?.let { (title, json) ->
        MoneyLensExportSheetDialog(
            title = title,
            json = json,
            onDismiss = { moneyLensExportJsonForDialog = null },
            onShareSheet = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, json)
                    putExtra(Intent.EXTRA_TITLE, "MoneyLens Expense - $title")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Export to Expense App")
                context.startActivity(shareIntent)
                moneyLensExportJsonForDialog = null
            },
            onSaveSaf = {
                jsonToExportSaf = json
                moneyLensFileLauncher.launch("moneylens_expense_${System.currentTimeMillis()}.json")
                moneyLensExportJsonForDialog = null
            }
        )
    }
}

@Composable
fun SideBySideComparisonView(
    snapshots: List<SavedSnapshot>,
    onConfirmPurchase: (SavedSnapshot) -> Unit,
    onExportMoneyLens: (SavedSnapshot) -> Unit
) {
    if (snapshots.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("No Saved Quotes to Compare Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Save 2 or more quotes from the Calculate tab (e.g. Shopee vs Lazada vs PDD) to compare their Pay Now totals, shipping, and conditions side-by-side.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // Determine if any quote is incomplete, and find the lowest complete quote in MYR reporting terms
    val hasIncomplete = snapshots.any { it.result.status != QuoteStatus.COMPLETE }
    val lowestSnapshotId = snapshots
        .filter { it.result.status == QuoteStatus.COMPLETE }
        .minByOrNull { snap ->
            (snap.result.reportingAmountDue ?: snap.result.amountDue)?.minorUnits ?: Long.MAX_VALUE
        }?.id

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (hasIncomplete) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Conditional Comparison: One or more listings have unknown shipping or missing rates. Verify missing costs before deciding.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        items(snapshots, key = { it.id }) { snapshot ->
            val isCheapest = snapshot.id == lowestSnapshotId && snapshots.size > 1
            val itemTitle = snapshot.request.items.firstOrNull()?.id ?: "Item"
            val platform = snapshot.scenarioId ?: "Quote"
            val effectiveReportingDue = snapshot.result.reportingAmountDue ?: snapshot.result.amountDue

            Card(
                modifier = Modifier.fillMaxWidth(),
                border = if (isCheapest && !hasIncomplete) {
                    androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2E7D32))
                } else null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$platform — $itemTitle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Saved ${snapshot.calculatedAt.toString().take(16).replace('T', ' ')}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isCheapest) {
                            Surface(
                                color = if (hasIncomplete) Color(0xFFE65100).copy(alpha = 0.15f) else Color(0xFF2E7D32).copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = if (hasIncomplete) "Conditional Lowest" else "Lowest Pay Now",
                                    color = if (hasIncomplete) Color(0xFFE65100) else Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Pay Now: ${snapshot.result.amountDue?.formatDisplay() ?: "Unknown (Incomplete)"}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (snapshot.result.amountDue != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )

                    if (effectiveReportingDue != null && effectiveReportingDue != snapshot.result.amountDue) {
                        Text(
                            text = "≈ ${effectiveReportingDue.formatDisplay()} (Converted Reporting Total)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    val shipText = when {
                        snapshot.request.shipping?.isUnknown == true -> "Unknown ⚠️"
                        snapshot.request.shipping?.amount != null -> snapshot.request.shipping!!.amount!!.formatDisplay()
                        else -> "Free"
                    }
                    Text(
                        text = "Immediate Discount: ${snapshot.result.immediateDiscounts.formatDisplay()} • Shipping: $shipText",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (snapshot.result.futureRewards.minorUnits > 0) {
                        Text(
                            text = "Future Cashback (Not deducted now): ${snapshot.result.futureRewards.formatDisplay()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onConfirmPurchase(snapshot) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark Purchased", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { onExportMoneyLens(snapshot) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export MoneyLens", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddCartItemDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, seller: String, priceMinor: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var seller by remember { mutableStateOf("Shopee") }
    var priceStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Item to Cart Optimizer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    singleLine = true
                )
                Text("Platform / Seller:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Shopee", "Lazada", "Samsung").forEach { s ->
                        FilterChip(
                            selected = seller == s,
                            onClick = { seller = s },
                            label = { Text(s) }
                        )
                    }
                }
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Price (RM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minor = ((priceStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    if (name.isNotBlank() && minor > 0) {
                        onAdd(name, seller, minor)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
