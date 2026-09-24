package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun SplitPdfScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("No file selected") }
    var totalPages by remember { mutableStateOf(0) }
    var startPageStr by remember { mutableStateOf("1") }
    var endPageStr by remember { mutableStateOf("1") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            fileName = uri.lastPathSegment ?: "Document.pdf"
            val count = PdfUtils.getPageCount(context, uri)
            totalPages = count
            startPageStr = "1"
            endPageStr = count.toString()
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { outputUri ->
        if (outputUri != null && selectedUri != null) {
            val start = startPageStr.toIntOrNull()?.minus(1) ?: 0
            val end = endPageStr.toIntOrNull()?.minus(1) ?: (totalPages - 1)
            coroutineScope.launch {
                isProcessing = true
                statusMessage = "Splitting PDF..."
                val success = PdfUtils.splitPdf(context, selectedUri!!, start, end, outputUri)
                isProcessing = false
                if (success) {
                    statusMessage = "Successfully split PDF!"
                    viewModel.addHistory("Split PDF ($startPageStr-$endPageStr)", outputUri.toString(), "Split")
                } else {
                    statusMessage = "Failed to split PDF."
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split PDF") },
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
                    Text("Extract Page Range", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Select a PDF file and specify the page range you wish to extract into a new document.",
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Total Pages: $totalPages", fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = startPageStr,
                                onValueChange = { startPageStr = it },
                                label = { Text("Start Page") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = endPageStr,
                                onValueChange = { endPageStr = it },
                                label = { Text("End Page") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = { saveFileLauncher.launch("split_document.pdf") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Processing...")
                            } else {
                                Icon(Icons.Default.CallSplit, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Split & Save PDF")
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
