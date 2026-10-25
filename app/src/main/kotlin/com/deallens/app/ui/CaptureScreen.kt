package com.deallens.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deallens.core.domain.CurrencyCode
import com.deallens.core.domain.Money
import com.deallens.core.domain.parser.ParsedPrice
import com.deallens.core.domain.parser.ReviewDraft
import com.deallens.core.pricing.parser.OcrTextParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    initialImageUri: Uri? = null,
    initialSharedText: String? = null,
    onClearInitialShare: () -> Unit = {},
    onDraftConfirmed: (draft: ReviewDraft, selectedPrice: ParsedPrice?, itemName: String) -> Unit,
    onSaveObservationDirect: (itemName: String, platform: String, price: Money) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentDraft by remember { mutableStateOf<ReviewDraft?>(null) }
    var selectedCandidate by remember { mutableStateOf<ParsedPrice?>(null) }
    var itemName by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Handle incoming shared screenshot or text from Android Share Sheet
    LaunchedEffect(initialImageUri, initialSharedText) {
        if (initialImageUri != null) {
            isProcessing = true
            errorMessage = null
            try {
                val draft = processImage(context, initialImageUri)
                currentDraft = draft
                selectedCandidate = draft.candidatePrices.firstOrNull { !it.isProbableInstallment }
                    ?: draft.candidatePrices.firstOrNull()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to process shared screenshot"
            } finally {
                isProcessing = false
                onClearInitialShare()
            }
        } else if (!initialSharedText.isNullOrBlank()) {
            val lines = initialSharedText.lines().filter { it.isNotBlank() }
            val draft = OcrTextParser.parseScreenshot(lines)
            currentDraft = draft
            selectedCandidate = draft.candidatePrices.firstOrNull { !it.isProbableInstallment }
                ?: draft.candidatePrices.firstOrNull()
            // Extract a probable product name from the first non-URL line
            val firstTitleLine = lines.firstOrNull { !it.contains("http") && it.length > 3 }
            if (firstTitleLine != null && itemName.isBlank()) {
                itemName = firstTitleLine.take(50)
            }
            onClearInitialShare()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    val draft = processImage(context, uri)
                    currentDraft = draft
                    selectedCandidate = draft.candidatePrices.firstOrNull { !it.isProbableInstallment }
                        ?: draft.candidatePrices.firstOrNull()
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Failed to process image"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Select Screenshot") },
                text = { Text("Pick Screenshot") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (isProcessing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Running Offline Bundled ML Kit OCR...")
                    }
                }
            } else if (errorMessage != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (currentDraft != null) {
                val draft = currentDraft!!
                Text("Review Extracted Draft", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Nothing is auto-saved. Verify the price and platform before proceeding.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item / Listing Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = draft.platform ?: "",
                    onValueChange = { currentDraft = draft.copy(platform = it) },
                    label = { Text("Detected Platform (Shopee / Lazada / PDD / Samsung)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Tap to Select Confirmed Price:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(draft.candidatePrices) { price ->
                        val isSelected = selectedCandidate == price
                        OutlinedCard(
                            onClick = { selectedCandidate = price },
                            modifier = Modifier.fillMaxWidth(),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${price.currencyCode} ${String.format("%.2f", price.amountMinorUnits / 100.0)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Source: \"${price.sourceText}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (price.isProbableInstallment) {
                                        Text(
                                            text = "⚠️ Probable Installment Amount (/bln, x12)",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (price.isProbableOriginalPrice) {
                                        Text(
                                            text = "ℹ️ Probable RRP / Crossed-out Price",
                                            color = Color(0xFFE65100),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedCandidate = price }
                                )
                            }
                        }
                    }

                    if (draft.voucherTerms.isNotEmpty()) {
                        item {
                            Text("Detected Voucher Clues:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            draft.voucherTerms.forEach { term ->
                                Text("• $term", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (draft.warnings.isNotEmpty()) {
                        item {
                            draft.warnings.forEach { warning ->
                                Text(warning, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val chosen = selectedCandidate ?: draft.candidatePrices.firstOrNull()
                            if (chosen != null) {
                                val curr = if (chosen.currencyCode == "CNY") CurrencyCode.CNY else CurrencyCode.MYR
                                onSaveObservationDirect(
                                    itemName.ifBlank { "Captured Listing" },
                                    draft.platform ?: "Screenshot",
                                    Money(chosen.amountMinorUnits, curr)
                                )
                            }
                        },
                        enabled = selectedCandidate != null || draft.candidatePrices.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Observation")
                    }

                    Button(
                        onClick = {
                            onDraftConfirmed(draft, selectedCandidate, itemName)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Load Calculator")
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Offline Screenshot & Share Capture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap 'Pick Screenshot' below or share a listing screenshot/link directly from Shopee, Lazada, or PDD via the Android Share Sheet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private suspend fun processImage(context: Context, uri: Uri): ReviewDraft = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
    val image = InputImage.fromFilePath(context, uri)
    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    
    recognizer.process(image)
        .addOnSuccessListener { text ->
            val blocks = text.textBlocks.map { it.text }
            val draft = OcrTextParser.parseScreenshot(blocks)
            continuation.resume(draft, null)
        }
        .addOnFailureListener { e ->
            continuation.resumeWith(Result.failure(e))
        }
}
