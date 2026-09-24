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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoOrientPdfScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("No file selected") }
    var pageCount by remember { mutableStateOf(0) }
    var landscapeCount by remember { mutableStateOf(0) }
    var portraitCount by remember { mutableStateOf(0) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            fileName = uri.lastPathSegment ?: "Document.pdf"
            coroutineScope.launch(Dispatchers.IO) {
                val count = PdfUtils.getPageCount(context, uri)
                var lCount = 0
                var pCount = 0
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val renderer = android.graphics.pdf.PdfRenderer(pfd)
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        if (page.width > page.height) {
                            lCount++
                        } else {
                            pCount++
                        }
                        page.close()
                    }
                    renderer.close()
                    pfd.close()
                }
                withContext(Dispatchers.Main) {
                    pageCount = count
                    landscapeCount = lCount
                    portraitCount = pCount
                }
            }
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { outputUri ->
        if (outputUri != null && selectedUri != null) {
            coroutineScope.launch {
                isProcessing = true
                statusMessage = "Auto-orienting scanned pages..."
                val success = PdfUtils.autoOrientPdf(context, selectedUri!!, outputUri)
                isProcessing = false
                if (success) {
                    statusMessage = "Successfully auto-oriented PDF pages!"
                    viewModel.addHistory("Auto-Oriented PDF ($fileName)", outputUri.toString(), "AutoOrient")
                } else {
                    statusMessage = "Failed to auto-orient PDF."
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-Orient Scanned Pages") },
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Smart Orientation Correction", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Automatically detects sideways or landscape scanned pages and rotates them into the correct upright portrait position for effortless reading.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = { filePicker.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (selectedUri == null) "Select PDF Document" else fileName)
            }

            if (selectedUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Document Analysis", fontWeight = FontWeight.Bold)
                        Text("Total Pages: $pageCount", fontSize = 14.sp)
                        Text("Portrait Pages (Upright): $portraitCount", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Sideways/Landscape Pages: $landscapeCount", fontSize = 14.sp, color = if (landscapeCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                }

                Button(
                    onClick = {
                        val baseName = fileName.substringBeforeLast(".pdf")
                        saveFileLauncher.launch("${baseName}_auto_oriented.pdf")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing...")
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-Detect & Fix Orientations")
                    }
                }
            }

            statusMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
