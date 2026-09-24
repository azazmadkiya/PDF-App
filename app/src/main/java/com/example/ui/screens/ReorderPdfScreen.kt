package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PdfViewModel
import com.example.utils.PdfUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderPdfScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("No file selected") }
    var pageCount by remember { mutableStateOf(0) }
    var pagesOrder by remember { mutableStateOf<List<Int>>(emptyList()) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            fileName = uri.lastPathSegment ?: "Document.pdf"
            val count = PdfUtils.getPageCount(context, uri)
            pageCount = count
            pagesOrder = (0 until count).toList()
            statusMessage = null
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { outputUri ->
        if (outputUri != null && selectedUri != null) {
            coroutineScope.launch {
                isProcessing = true
                statusMessage = "Saving reordered PDF..."
                val success = PdfUtils.reorderPdf(
                    context = context,
                    sourceUri = selectedUri!!,
                    newOrder = pagesOrder,
                    outputUri = outputUri
                )
                isProcessing = false
                if (success) {
                    statusMessage = "Successfully reordered and saved PDF!"
                    viewModel.addHistory("Reordered PDF ($pageCount pages)", outputUri.toString(), "Reorder")
                } else {
                    statusMessage = "Failed to reorder PDF."
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reorder PDF Pages") },
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
                        "Visually rearrange pages into your desired order before saving.",
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

            if (selectedUri != null && pageCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Page Order ($pageCount pages)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = { pagesOrder = (0 until pageCount).toList() }) {
                        Text("Reset Order")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(pagesOrder) { index, origPageIndex ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = "Drag Handle",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column {
                                        Text(
                                            "New Position: ${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            "Original Page: ${origPageIndex + 1}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutableList = pagesOrder.toMutableList()
                                                val item = mutableList.removeAt(index)
                                                mutableList.add(index - 1, item)
                                                pagesOrder = mutableList
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move Up"
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < pagesOrder.size - 1) {
                                                val mutableList = pagesOrder.toMutableList()
                                                val item = mutableList.removeAt(index)
                                                mutableList.add(index + 1, item)
                                                pagesOrder = mutableList
                                            }
                                        },
                                        enabled = index < pagesOrder.size - 1
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move Down"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { saveFileLauncher.launch("reordered_document.pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing...")
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Reordered PDF")
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
