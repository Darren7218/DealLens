package com.deallens.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deallens.core.domain.*
import com.deallens.core.domain.repository.SavedSnapshot
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    viewModel: SavedViewModel = hiltViewModel()
) {
    val snapshots by viewModel.snapshots.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val products by viewModel.products.collectAsState()
    val wishlist by viewModel.wishlist.collectAsState()
    val currentTab by viewModel.selectedTab.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddWishlistDialog by remember { mutableStateOf(false) }
    var snapshotToConfirm by remember { mutableStateOf<SavedSnapshot?>(null) }
    var jsonToExportSaf by remember { mutableStateOf<String?>(null) }
    var moneyLensExportJsonForDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // title to json

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { dest ->
            pendingExportJson?.let { json ->
                try {
                    context.contentResolver.openOutputStream(dest)?.use { out ->
                        out.write(json.toByteArray())
                    }
                    viewModel.clearMessage()
                } catch (_: Exception) {}
            }
        }
        pendingExportJson = null
    }

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

    var csvToExportSaf by remember { mutableStateOf<String?>(null) }
    val csvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { dest ->
            csvToExportSaf?.let { csv ->
                try {
                    context.contentResolver.openOutputStream(dest)?.use { out ->
                        out.write(csv.toByteArray())
                    }
                } catch (_: Exception) {}
            }
        }
        csvToExportSaf = null
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { src ->
            try {
                val json = context.contentResolver.openInputStream(src)?.bufferedReader()?.use { it.readText() }
                if (json != null) {
                    viewModel.restoreData(json)
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Saved & History") },
                actions = {
                    IconButton(onClick = {
                        viewModel.exportData { json ->
                            pendingExportJson = json
                            backupExportLauncher.launch("deallens_backup.json")
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = "Export Backup")
                    }
                    IconButton(onClick = {
                        restoreLauncher.launch(arrayOf("application/json"))
                    }) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Restore Backup")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentTab == SavedTab.WISHLIST) {
                FloatingActionButton(onClick = { showAddWishlistDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Wishlist Target")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            TabRow(selectedTabIndex = currentTab.ordinal) {
                SavedTab.values().forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.label) }
                    )
                }
            }

            when (currentTab) {
                SavedTab.SNAPSHOTS -> {
                    if (snapshots.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No saved calculation snapshots yet.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(snapshots) { snapshot ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = "Snapshot: ${snapshot.id.take(8)}", style = MaterialTheme.typography.titleMedium)
                                        Text(text = "Calculated: ${snapshot.calculatedAt}", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val result = snapshot.result
                                        Text(text = "Amount Due: ${result.amountDue?.formatDisplay() ?: "Incomplete"}", style = MaterialTheme.typography.bodyLarge)
                                        if (result.immediateDiscounts.minorUnits > 0) {
                                            Text(text = "Discounts: ${result.immediateDiscounts.formatDisplay()}", color = MaterialTheme.colorScheme.primary)
                                        }
                                        if (result.futureRewards.minorUnits > 0) {
                                            Text(text = "Cashback/Rewards: ${result.futureRewards.formatDisplay()}", color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Text(text = "Status: ${result.status}", style = MaterialTheme.typography.labelSmall)
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedButton(
                                                onClick = { snapshotToConfirm = snapshot },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Confirm Purchase", style = MaterialTheme.typography.labelSmall)
                                            }

                                            Button(
                                                onClick = {
                                                    val exportJson = viewModel.generateMoneyLensExportJsonFromSnapshot(snapshot)
                                                    moneyLensExportJsonForDialog = Pair("Snapshot ${snapshot.id.take(8)}", exportJson)
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
                        }
                    }
                }

                SavedTab.PURCHASES -> {
                    if (purchases.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No confirmed purchases recorded yet.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                FilledTonalButton(
                                    onClick = {
                                        csvToExportSaf = viewModel.generatePurchasesCsvExport()
                                        csvFileLauncher.launch("deallens_purchases_${System.currentTimeMillis()}.csv")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Export All Purchases as CSV (MoneyLens Statement)")
                                }
                            }
                            items(purchases) { purchase ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = purchase.sellerOrPlatform,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Badge {
                                                Text("Paid ${purchase.payNowAmount.formatDisplay()}")
                                            }
                                        }
                                        Text(text = "Date: ${purchase.timestamp}", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.height(6.dp))

                                        if (purchase.items.isNotEmpty()) {
                                            Text("Items:", style = MaterialTheme.typography.labelMedium)
                                            purchase.items.forEach { item ->
                                                Text("- ${item.name} (x${item.quantity}) • ${item.unitPrice.formatDisplay()}", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }

                                        if (purchase.immediateDiscountsTotal.minorUnits > 0) {
                                            Text("Saved: ${purchase.immediateDiscountsTotal.formatDisplay()}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (purchase.futureCashback.minorUnits > 0) {
                                            Text("Cashback: ${purchase.futureCashback.formatDisplay()}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                                        }
                                        purchase.receiptNote?.let { note ->
                                            if (note.isNotBlank()) {
                                                Text("Note: $note", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                val json = viewModel.generateMoneyLensExportJson(purchase)
                                                moneyLensExportJsonForDialog = Pair(purchase.sellerOrPlatform, json)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Export to MoneyLens")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SavedTab.PRICE_HISTORY -> {
                    if (products.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No recorded price observations yet.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(products) { product ->
                                ProductHistoryItem(
                                    product = product,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }

                SavedTab.WISHLIST -> {
                    if (wishlist.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Wishlist is empty. Tap + to set a target price.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(wishlist) { item ->
                                WishlistItemCard(
                                    product = item,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddWishlistDialog) {
        AddWishlistDialog(
            onDismiss = { showAddWishlistDialog = false },
            onConfirm = { name, amountMinor, priority ->
                viewModel.addWishlistItem(name, amountMinor, CurrencyCode.MYR, priority)
                showAddWishlistDialog = false
            }
        )
    }

    snapshotToConfirm?.let { snapshot ->
        ConfirmPurchaseDialog(
            snapshot = snapshot,
            onDismiss = { snapshotToConfirm = null },
            onConfirm = { platform, note ->
                viewModel.confirmPurchaseFromSnapshot(snapshot, platform, note)
                snapshotToConfirm = null
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
fun ConfirmPurchaseDialog(
    snapshot: SavedSnapshot,
    onDismiss: () -> Unit,
    onConfirm: (platform: String, note: String) -> Unit
) {
    var platform by remember { mutableStateOf("Shopee") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Purchase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Amount: ${snapshot.result.amountDue?.formatDisplay() ?: "Incomplete"}",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = platform,
                    onValueChange = { platform = it },
                    label = { Text("Platform / Store") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Receipt Note (optional)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(platform.ifBlank { "Online Checkout" }, note)
                }
            ) {
                Text("Confirm & Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MoneyLensExportSheetDialog(
    title: String,
    json: String,
    onDismiss: () -> Unit,
    onShareSheet: () -> Unit,
    onSaveSaf: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export to MoneyLens") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose an offline export method for personal finance handoff:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        text = json.take(200) + if (json.length > 200) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onShareSheet) {
                    Text("Share Sheet")
                }
                FilledTonalButton(onClick = onSaveSaf) {
                    Text("Save JSON")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ProductHistoryItem(
    product: Product,
    viewModel: SavedViewModel
) {
    val historyState by viewModel.getProductHistory(product.id).collectAsState(initial = null)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium
                )
                historyState?.lowestRecordedPrice?.let { lowest ->
                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text("Lowest saved: ${lowest.formatDisplay()}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            val observations = historyState?.observations ?: emptyList()
            if (observations.isEmpty()) {
                Text(
                    text = "No recorded price points yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Recorded price history (oldest to newest):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                observations.forEach { obs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = obs.observedAt.toString().take(10),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = obs.price.formatDisplay(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WishlistItemCard(
    product: Product,
    viewModel: SavedViewModel
) {
    val historyState by viewModel.getProductHistory(product.id).collectAsState(initial = null)
    val latestPrice = historyState?.observations?.lastOrNull()?.price
    val targetPrice = product.targetPrice

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = product.name, style = MaterialTheme.typography.titleMedium)
                    product.priority?.let {
                        Text("Priority: ${it.name}", style = MaterialTheme.typography.labelSmall)
                    }
                }
                targetPrice?.let {
                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text("Target: ${it.formatDisplay()}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (latestPrice != null && targetPrice != null) {
                if (latestPrice <= targetPrice) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Target Met or Beaten! Current: ${latestPrice.formatDisplay()}",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Text(
                        text = "Current: ${latestPrice.formatDisplay()} (Above target by ${(latestPrice - targetPrice).formatDisplay()})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Text(
                    text = "No prices observed yet for this target.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AddWishlistDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, targetAmountMinor: Long, priority: Priority) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.HIGH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Wishlist Target") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Target Price (RM)") },
                    singleLine = true
                )
                Text("Priority:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.values().forEach { prio ->
                        FilterChip(
                            selected = selectedPriority == prio,
                            onClick = { selectedPriority = prio },
                            label = { Text(prio.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minor = (priceText.toDoubleOrNull() ?: 0.0) * 100
                    if (name.isNotBlank() && minor > 0) {
                        onConfirm(name, minor.toLong(), selectedPriority)
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
