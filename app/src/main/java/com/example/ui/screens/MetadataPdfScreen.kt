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
fun MetadataPdfScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("No file selected") }

    var titleInput by remember { mutableStateOf("") }
    var authorInput by remember { mutableStateOf("") }
    var subjectInput by remember { mutableStateOf("") }
    var creatorInput by remember { mutableStateOf("") }
    var producerDisplay by remember { mutableStateOf("") }
    var creationDateDisplay by remember { mutableStateOf("") }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            fileName = uri.lastPathSegment ?: "Document.pdf"
            val metadata = PdfUtils.getPdfMetadata(context, uri)
            titleInput = metadata.title
            authorInput = metadata.author
            subjectInput = metadata.subject
            creatorInput = metadata.creator
            producerDisplay = metadata.producer
            creationDateDisplay = metadata.creationDate
            statusMessage = "Metadata loaded successfully"
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { outputUri ->
        if (outputUri != null && selectedUri != null) {
            coroutineScope.launch {
                isProcessing = true
                statusMessage = "Updating PDF metadata..."
                val success = PdfUtils.updatePdfMetadata(
                    context = context,
                    sourceUri = selectedUri!!,
                    title = titleInput,
                    author = authorInput,
                    subject = subjectInput,
                    creator = creatorInput,
                    outputUri = outputUri
                )
                isProcessing = false
                if (success) {
                    statusMessage = "Metadata updated successfully!"
                    viewModel.addHistory("Edited Metadata ($fileName)", outputUri.toString(), "Metadata")
                } else {
                    statusMessage = "Failed to update metadata."
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Document Metadata") },
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View & Edit Document Properties", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Inspect and update document properties like Title, Author, Subject, and Creator fields stored inside the PDF.",
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Metadata Fields", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = authorInput,
                            onValueChange = { authorInput = it },
                            label = { Text("Author") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = subjectInput,
                            onValueChange = { subjectInput = it },
                            label = { Text("Subject") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = creatorInput,
                            onValueChange = { creatorInput = it },
                            label = { Text("Creator") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (producerDisplay.isNotBlank()) {
                            OutlinedTextField(
                                value = producerDisplay,
                                onValueChange = {},
                                enabled = false,
                                label = { Text("Producer (Read-Only)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (creationDateDisplay.isNotBlank()) {
                            OutlinedTextField(
                                value = creationDateDisplay,
                                onValueChange = {},
                                enabled = false,
                                label = { Text("Creation Date (Read-Only)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Button(
                            onClick = { saveFileLauncher.launch("metadata_$fileName") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving...")
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Updated PDF")
                            }
                        }
                    }
                }
            }

            if (statusMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            statusMessage!!,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
