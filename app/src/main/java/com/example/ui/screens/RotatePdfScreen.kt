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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotatePdfScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("No file selected") }
    var pageCount by remember { mutableStateOf(0) }

    // Rotation settings
    var rotationAngle by remember { mutableStateOf(90) } // 90, 180, 270
    var rotateAllPages by remember { mutableStateOf(true) }
    var targetPageInput by remember { mutableStateOf("1") }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            fileName = uri.lastPathSegment ?: "Document.pdf"
            pageCount = PdfUtils.getPageCount(context, uri)
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { outputUri ->
        if (outputUri != null && selectedUri != null) {
            coroutineScope.launch {
                isProcessing = true
                statusMessage = "Rotating PDF pages..."
                
                val specificPages = if (rotateAllPages) {
                    null
                } else {
                    val pIdx = (targetPageInput.toIntOrNull() ?: 1) - 1
                    listOf(pIdx.coerceIn(0, (pageCount - 1).coerceAtLeast(0)))
                }

                val success = PdfUtils.rotatePdf(
                    context = context,
                    sourceUri = selectedUri!!,
                    rotationAngle = rotationAngle,
                    specificPages = specificPages,
                    outputUri = outputUri
                )
                isProcessing = false
                if (success) {
                    statusMessage = "Successfully rotated PDF!"
                    viewModel.addHistory("Rotated PDF (${rotationAngle}°)", outputUri.toString(), "Rotate")
                } else {
                    statusMessage = "Failed to rotate PDF."
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rotate PDF Pages") },
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
                        "Rotate specific pages or the entire PDF document by 90, 180, or 270 degrees.",
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
                        Text("Rotation Angle", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = rotationAngle == 90,
                                onClick = { rotationAngle = 90 },
                                label = { Text("90° Clockwise") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = rotationAngle == 180,
                                onClick = { rotationAngle = 180 },
                                label = { Text("180°") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = rotationAngle == 270,
                                onClick = { rotationAngle = 270 },
                                label = { Text("270° (90° CCW)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Divider()

                        Text("Rotation Scope", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = rotateAllPages,
                                onClick = { rotateAllPages = true },
                                label = { Text("All Pages") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = !rotateAllPages,
                                onClick = { rotateAllPages = false },
                                label = { Text("Specific Page") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!rotateAllPages) {
                            OutlinedTextField(
                                value = targetPageInput,
                                onValueChange = { targetPageInput = it },
                                label = { Text("Page Number (1 to $pageCount)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Button(
                            onClick = { saveFileLauncher.launch("rotated_document.pdf") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Processing...")
                            } else {
                                Icon(Icons.Default.RotateRight, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rotate & Save PDF")
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
