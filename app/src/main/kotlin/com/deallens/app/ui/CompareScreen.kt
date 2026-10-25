package com.deallens.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Compare Deals") })
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select scenarios from Saved to compare side-by-side.")
        }
    }
}
