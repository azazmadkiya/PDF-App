package com.example.ui.screens

import android.graphics.Bitmap
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
fun PdfToImagesScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("No file selected") }
    var pageCount by remember { mutableStateOf(0) }

    // Export settings
    var isPng by remember { mutableStateOf(false) } // Default JPEG high quality
    var quality by remember { mutableStateOf(90) }
    var scaleMultiplier by remember { mutableStateOf(2.0f) } // 2x high-res default
    var targetPage by remember { mutableStateOf("1") }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            fileName = uri.lastPathSegment ?: "Document.pdf"
            pageCount = PdfUtils.getPageCount(context, uri)
        }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri != null && selectedUri != null) {
            coroutineScope.launch {
                isProcessing = true
                statusMessage = "Exporting all pages to images..."
                val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val count = PdfUtils.exportPdfPagesToImages(
                    context = context,
                    sourceUri = selectedUri!!,
                    outputTreeUri = treeUri,
                    format = format,
                    quality = quality,
                    scaleMultiplier = scaleMultiplier
                )
                isProcessing = false
                if (count > 0) {
                    statusMessage = "Successfully exported $count pages as images!"
                    viewModel.addHistory("Exported $count pages to images", fileName, "Export")
                } else {
                    statusMessage = "Failed to export pages."
                }
            }
        }
    }

    val singlePageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(if (isPng) "image/png" else "image/jpeg")
    ) { outputUri ->
        if (outputUri != null && selectedUri != null) {
            coroutineScope.launch {
                isProcessing = true
                statusMessage = "Exporting page to image..."
                val pIdx = (targetPage.toIntOrNull() ?: 1) - 1
                val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val success = PdfUtils.exportSinglePageToImage(
                    context = context,
                    sourceUri = selectedUri!!,
                    pageIndex = pIdx,
                    outputUri = outputUri,
                    format = format,
                    quality = quality,
                    scaleMultiplier = scaleMultiplier
                )
                isProcessing = false
                if (success) {
                    statusMessage = "Successfully exported page as image!"
                    viewModel.addHistory("Exported page ${pIdx + 1} to image", outputUri.toString(), "Export")
                } else {
                    statusMessage = "Failed to export page."
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF to High-Res Images") },
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
                        "Convert PDF pages into high-resolution PNG or JPEG images locally.",
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
                        Text(if (selectedUri == null) "Select PDF File" else "$fileName ($pageCount pages)")
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
                        Text("Image Format", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !isPng,
                                onClick = { isPng = false },
                                label = { Text("JPEG (Compressed)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isPng,
                                onClick = { isPng = true },
                                label = { Text("PNG (Lossless)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!isPng) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("JPEG Quality", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("$quality%", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = quality.toFloat(),
                                    onValueChange = { quality = it.roundToInt() },
                                    valueRange = 50f..100f
                                )
                            }
                        }

                        // Resolution Scale Multiplier
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Resolution Scale", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("${scaleMultiplier}x (${(scaleMultiplier * 100).toInt()}% DPI)", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { scaleMultiplier = 1.0f },
                                    modifier = Modifier.weight(1f)
                                ) { Text("1x Standard") }
                                OutlinedButton(
                                    onClick = { scaleMultiplier = 2.0f },
                                    modifier = Modifier.weight(1f)
                                ) { Text("2x High-Res") }
                                OutlinedButton(
                                    onClick = { scaleMultiplier = 3.0f },
                                    modifier = Modifier.weight(1f)
                                ) { Text("3x Ultra-HD") }
                            }
                        }

                        Divider()

                        // Export All Pages Option
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Export All Pages", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Save all $pageCount pages as individual image files into a selected folder.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(
                                onClick = { folderPicker.launch(null) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isProcessing
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose Folder & Export All Pages")
                            }
                        }

                        Divider()

                        // Export Single Page Option
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Export Specific Page", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            OutlinedTextField(
                                value = targetPage,
                                onValueChange = { targetPage = it },
                                label = { Text("Page Number (1 to $pageCount)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val ext = if (isPng) "png" else "jpg"
                                    singlePageLauncher.launch("page_${targetPage}_image.$ext")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isProcessing && (targetPage.toIntOrNull() ?: 0) in 1..pageCount
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export Page as Image")
                            }
                        }

                        if (isProcessing) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Processing images...")
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
