package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PdfViewModel
import com.example.utils.PdfUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("No file selected") }

    // Watermark settings
    var isTextWatermark by remember { mutableStateOf(true) }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var watermarkImageUri by remember { mutableStateOf<Uri?>(null) }
    var opacity by remember { mutableStateOf(0.35f) } // 35% default
    var rotationAngle by remember { mutableStateOf(-45f) } // Diagonal default
    var includePageNumbers by remember { mutableStateOf(true) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            fileName = uri.lastPathSegment ?: "Document.pdf"
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            watermarkImageUri = uri
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { outputUri ->
        if (outputUri != null && selectedUri != null) {
            coroutineScope.launch {
                isProcessing = true
                statusMessage = "Applying watermark & page numbers..."
                val success = PdfUtils.addWatermarkAndPageNumbers(
                    context = context,
                    sourceUri = selectedUri!!,
                    isTextWatermark = isTextWatermark,
                    watermarkText = watermarkText,
                    watermarkImageUri = watermarkImageUri,
                    opacity = opacity,
                    rotationAngle = rotationAngle,
                    includePageNumbers = includePageNumbers,
                    outputUri = outputUri
                )
                isProcessing = false
                if (success) {
                    statusMessage = "Successfully watermarked PDF!"
                    viewModel.addHistory("Watermarked PDF", outputUri.toString(), "Watermark")
                } else {
                    statusMessage = "Failed to watermark PDF."
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watermark & Page Numbers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select PDF Document", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Stamp custom text or image watermarks with precise opacity and rotation settings.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { filePicker.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedUri == null) "Select PDF File" else fileName)
                    }
                }
            }

            if (selectedUri != null) {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Watermark Type", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = isTextWatermark,
                                onClick = { isTextWatermark = true },
                                label = { Text("Text Watermark") },
                                leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = !isTextWatermark,
                                onClick = { isTextWatermark = false },
                                label = { Text("Image Watermark") },
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (isTextWatermark) {
                            OutlinedTextField(
                                value = watermarkText,
                                onValueChange = { watermarkText = it },
                                label = { Text("Watermark Text") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { imagePicker.launch(arrayOf("image/*")) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (watermarkImageUri == null) "Select Watermark Image" else "Change Watermark Image")
                                }
                                if (watermarkImageUri != null) {
                                    Text(
                                        "Selected Image: ${watermarkImageUri!!.lastPathSegment ?: "Image"}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Opacity slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Opacity", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("${(opacity * 100).roundToInt()}%", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = opacity,
                                onValueChange = { opacity = it },
                                valueRange = 0.1f..1.0f
                            )
                        }

                        // Rotation slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Rotation Angle", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("${rotationAngle.roundToInt()}°", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = rotationAngle,
                                onValueChange = { rotationAngle = it },
                                valueRange = -180f..180f
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { rotationAngle = -45f },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(4.dp)
                                ) { Text("Diagonal (-45°)", fontSize = 11.sp) }
                                OutlinedButton(
                                    onClick = { rotationAngle = 0f },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(4.dp)
                                ) { Text("Horizontal (0°)", fontSize = 11.sp) }
                                OutlinedButton(
                                    onClick = { rotationAngle = 90f },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(4.dp)
                                ) { Text("Vertical (90°)", fontSize = 11.sp) }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Include Page Numbers Footer")
                            Switch(
                                checked = includePageNumbers,
                                onCheckedChange = { includePageNumbers = it }
                            )
                        }

                        Button(
                            onClick = { saveFileLauncher.launch("watermarked_document.pdf") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing && (!isTextWatermark || watermarkText.isNotBlank()) && (isTextWatermark || watermarkImageUri != null)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Processing...")
                            } else {
                                Icon(Icons.Default.WaterDrop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Apply & Save PDF")
                            }
                        }
                    }
                }
            }

            if (statusMessage != null) {
                Text(
                    statusMessage!!,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
