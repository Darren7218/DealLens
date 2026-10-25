package com.deallens.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.deallens.core.domain.*
import com.deallens.core.domain.repository.VoucherProfile
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel = hiltViewModel(),
    onApplyToCalculator: ((VoucherProfile) -> Unit)? = null
) {
    val vouchers by viewModel.vouchers.collectAsState()
    val context = LocalContext.current

    var showEditorDialog by remember { mutableStateOf(false) }
    var editingVoucher by remember { mutableStateOf<VoucherProfile?>(null) }

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voucher Wallet (${vouchers.size})") },
                actions = {
                    TextButton(onClick = { viewModel.addCuratedPresets() }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Load Presets")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingVoucher = null
                    showEditorDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Voucher") },
                text = { Text("Add Voucher") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Expiry Alerts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Allow notifications to get offline alerts 48h before vouchers expire.", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(onClick = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }) {
                            Text("Enable")
                        }
                    }
                }
            }

            if (vouchers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Your Voucher Wallet is Empty", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap '+ Add Voucher' to enter a custom voucher or 'Load Presets' to add starter Shopee, Lazada, and PDD rules.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(vouchers, key = { it.id }) { voucher ->
                        VoucherCard(
                            voucher = voucher,
                            onEdit = {
                                editingVoucher = voucher
                                showEditorDialog = true
                            },
                            onDelete = { viewModel.deleteVoucher(voucher.id) },
                            onApply = onApplyToCalculator?.let { cb -> { cb(voucher) } }
                        )
                    }
                }
            }
        }
    }

    if (showEditorDialog) {
        VoucherEditorDialog(
            existing = editingVoucher,
            onDismiss = { showEditorDialog = false },
            onSave = { name, platform, type, valStr, currency, capStr, minSpendStr, basis, expiryHours ->
                viewModel.saveCustomVoucher(
                    existingProfile = editingVoucher,
                    name = name,
                    platform = platform,
                    type = type,
                    valueStr = valStr,
                    currency = currency,
                    capStr = capStr,
                    minSpendStr = minSpendStr,
                    thresholdBasis = basis,
                    expiryHours = expiryHours
                )
                showEditorDialog = false
            }
        )
    }
}

@Composable
fun VoucherCard(
    voucher: VoucherProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onApply: (() -> Unit)? = null
) {
    val now = Instant.now()
    val expiry = voucher.expiresAt
    val isExpired = expiry != null && expiry.isBefore(now)
    val hoursRemaining = expiry?.let { Duration.between(now, it).toHours() }
    val isExpiringSoon = hoursRemaining != null && hoursRemaining in 0..48

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = voucher.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Platform: ${voucher.platform ?: "Generic"} • Rule v${voucher.currentVersion}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (expiry != null) {
                    val badgeColor = when {
                        isExpired -> MaterialTheme.colorScheme.error
                        isExpiringSoon -> Color(0xFFE65100)
                        else -> Color(0xFF2E7D32)
                    }
                    val badgeText = when {
                        isExpired -> "Expired"
                        isExpiringSoon -> "Expires in ${hoursRemaining}h"
                        else -> "Active (${hoursRemaining?.div(24)}d left)"
                    }
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            voucher.latestRule?.let { rule ->
                val valueSummary = when (rule.type) {
                    VoucherType.PERCENTAGE -> "${rule.value.toPlainString()}% OFF"
                    VoucherType.FIXED -> "${rule.minimumSpend?.currency?.name ?: "MYR"} ${rule.value.toPlainString()} OFF"
                    VoucherType.SHIPPING -> "Free Shipping (${rule.value.toPlainString()} cap)"
                    VoucherType.REWARD -> "${rule.value.toPlainString()} Cashback/Coins"
                }
                Text(
                    text = valueSummary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                val conditions = buildList {
                    rule.cap?.let { add("Cap: ${it.formatDisplay()}") }
                    rule.minimumSpend?.let { add("Min Spend: ${it.formatDisplay()}") }
                    add("Basis: ${rule.thresholdBasis.name.replace('_', ' ')}")
                }
                Text(
                    text = conditions.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onApply != null && !isExpired) {
                    Button(
                        onClick = onApply,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Use in Calculator")
                    }
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit (v${voucher.currentVersion + 1})")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun VoucherEditorDialog(
    existing: VoucherProfile?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        platform: String,
        type: VoucherType,
        valueStr: String,
        currency: CurrencyCode,
        capStr: String,
        minSpendStr: String,
        thresholdBasis: ThresholdBasis,
        expiryHours: Long?
    ) -> Unit
) {
    val rule = existing?.latestRule
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var platform by remember { mutableStateOf(existing?.platform ?: "Shopee") }
    var type by remember { mutableStateOf(rule?.type ?: VoucherType.PERCENTAGE) }
    var valueStr by remember { mutableStateOf(rule?.value?.toPlainString() ?: "15") }
    var currency by remember {
        mutableStateOf(
            rule?.cap?.currency ?: rule?.minimumSpend?.currency ?: CurrencyCode.MYR
        )
    }
    var capStr by remember {
        mutableStateOf(rule?.cap?.let { (it.minorUnits / 100.0).toString().removeSuffix(".0") } ?: "")
    }
    var minSpendStr by remember {
        mutableStateOf(rule?.minimumSpend?.let { (it.minorUnits / 100.0).toString().removeSuffix(".0") } ?: "")
    }
    var basis by remember { mutableStateOf(rule?.thresholdBasis ?: ThresholdBasis.ORIGINAL_SUBTOTAL) }
    var selectedExpiryHours by remember { mutableStateOf<Long?>(48L) }

    val platforms = listOf("Shopee", "Lazada", "PDD", "Samsung", "Generic")
    val expiryOptions = listOf(
        "No Change / None" to null,
        "24h" to 24L,
        "48h" to 48L,
        "7d" to 168L
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Create Wallet Voucher" else "Update Voucher (Saves v${existing.currentVersion + 1})")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Voucher Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Platform:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    platforms.take(4).forEach { p ->
                        FilterChip(
                            selected = platform.equals(p, ignoreCase = true),
                            onClick = {
                                platform = p
                                if (p == "PDD") currency = CurrencyCode.CNY else currency = CurrencyCode.MYR
                            },
                            label = { Text(p) }
                        )
                    }
                }

                Text("Type & Currency:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = type == VoucherType.PERCENTAGE,
                        onClick = { type = VoucherType.PERCENTAGE },
                        label = { Text("% Off") }
                    )
                    FilterChip(
                        selected = type == VoucherType.FIXED,
                        onClick = { type = VoucherType.FIXED },
                        label = { Text("Fixed") }
                    )
                    FilterChip(
                        selected = currency == CurrencyCode.MYR,
                        onClick = { currency = CurrencyCode.MYR },
                        label = { Text("MYR") }
                    )
                    FilterChip(
                        selected = currency == CurrencyCode.CNY,
                        onClick = { currency = CurrencyCode.CNY },
                        label = { Text("CNY") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = valueStr,
                        onValueChange = { valueStr = it },
                        label = { Text(if (type == VoucherType.PERCENTAGE) "Discount %" else "Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = capStr,
                        onValueChange = { capStr = it },
                        label = { Text("Cap (${currency.name})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        enabled = type == VoucherType.PERCENTAGE,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = minSpendStr,
                    onValueChange = { minSpendStr = it },
                    label = { Text("Minimum Spend (${currency.name})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Expiry Window:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    expiryOptions.forEach { (label, hrs) ->
                        FilterChip(
                            selected = selectedExpiryHours == hrs,
                            onClick = { selectedExpiryHours = hrs },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, platform, type, valueStr, currency, capStr, minSpendStr, basis, selectedExpiryHours)
                }
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
