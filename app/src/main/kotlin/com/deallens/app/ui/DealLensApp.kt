package com.deallens.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealLensApp(
    sharedImageUri: Uri? = null,
    sharedText: String? = null,
    onClearSharedContent: () -> Unit = {},
    viewModel: CalculatorViewModel = hiltViewModel(),
    savedViewModel: SavedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(sharedImageUri, sharedText) {
        if (sharedImageUri != null || !sharedText.isNullOrBlank()) {
            viewModel.selectTab(AppTab.CAPTURE)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DealLens",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Offline Shopping Decision Assistant",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.selectTab(AppTab.CAPTURE) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture Screenshot",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("OCR Scan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            DealLensBottomBar(
                currentTab = state.currentTab,
                onTabSelected = viewModel::selectTab
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (state.currentTab) {
                AppTab.CALCULATE -> {
                    CalculatorScreen(
                        state = state,
                        viewModel = viewModel
                    )
                }
                AppTab.COMPARE -> {
                    OptimizerScreen()
                }
                AppTab.WALLET -> {
                    WalletScreen(
                        onApplyToCalculator = { voucher ->
                            viewModel.applyWalletVoucher(voucher)
                        }
                    )
                }
                AppTab.SAVED -> {
                    SavedScreen()
                }
                AppTab.CAPTURE -> {
                    CaptureScreen(
                        initialImageUri = sharedImageUri,
                        initialSharedText = sharedText,
                        onClearInitialShare = onClearSharedContent,
                        onDraftConfirmed = { draft, selectedPrice, itemName ->
                            viewModel.applyOcrDraft(draft, selectedPrice, itemName)
                            viewModel.selectTab(AppTab.CALCULATE)
                        },
                        onSaveObservationDirect = { itemName, platform, price ->
                            savedViewModel.recordObservation(itemName, platform, price.minorUnits, price.currency)
                            viewModel.selectTab(AppTab.SAVED)
                            savedViewModel.selectTab(SavedTab.PRICE_HISTORY)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DealLensBottomBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    val primaryTabs = listOf(AppTab.CALCULATE, AppTab.COMPARE, AppTab.WALLET, AppTab.SAVED)
    NavigationBar {
        primaryTabs.forEach { tab ->
            val icon: ImageVector = when (tab) {
                AppTab.CALCULATE -> Icons.Default.Calculate
                AppTab.COMPARE -> Icons.AutoMirrored.Filled.CompareArrows
                AppTab.WALLET -> Icons.Default.AccountBalanceWallet
                AppTab.SAVED -> Icons.Default.Bookmark
                AppTab.CAPTURE -> Icons.Default.CameraAlt
            }

            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(imageVector = icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}
