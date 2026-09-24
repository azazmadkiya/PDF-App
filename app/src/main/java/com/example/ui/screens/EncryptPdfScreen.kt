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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PdfViewModel
import com.example.utils.PdfUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptPdfScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: Encrypt, 1: Decrypt

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("No file selected") }
    var pageCount by remember { mutableStateOf(0) }

    var userPassword by remember { mutableStateOf("") }
    var ownerPassword by remember { mutableStateOf("") }
    var decryptPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

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
                if (selectedTab == 0) {
                    statusMessage = "Encrypting PDF with password..."
                    val success = PdfUtils.encryptPdf(
                        context = context,
                        sourceUri = selectedUri!!,
                        ownerPassword = ownerPassword.ifBlank { "owner123" },
                        userPassword = userPassword,
                        outputUri = outputUri
                    )
                    isProcessing = false
                    if (success) {
                        statusMessage = "Successfully encrypted PDF!"
                        viewModel.addHistory("Encrypted PDF ($fileName)", outputUri.toString(), "Encrypt")
                    } else {
                        statusMessage = "Failed to encrypt PDF. Check parameters."
                    }
                } else {
                    statusMessage = "Decrypting PDF..."
                    val success = PdfUtils.decryptPdf(
                        context = context,
                        sourceUri = selectedUri!!,
                        password = decryptPassword,
                        outputUri = outputUri
                    )
                    isProcessing = false
                    if (success) {
                        statusMessage = "Successfully decrypted PDF!"
                        viewModel.addHistory("Decrypted PDF ($fileName)", outputUri.toString(), "Decrypt")
                    } else {
                        statusMessage = "Failed to decrypt PDF. Incorrect password or invalid file."
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Security & Passwords") },
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        selectedUri = null
                        statusMessage = null
                    },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    text = { Text("Encrypt PDF") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        selectedUri = null
                        statusMessage = null
                    },
                    icon = { Icon(Icons.Default.LockOpen, contentDescription = null) },
                    text = { Text("Decrypt PDF") }
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (selectedTab == 0) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (selectedTab == 0) "Secure Your Documents" else "Unlock Protected Documents",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (selectedTab == 0)
                            "Add password protection to prevent unauthorized access, copying, or modification of your sensitive PDF files."
                        else
                            "Remove password protection from encrypted PDF files using the correct password.",
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
                        Text(
                            if (selectedTab == 0) "Encryption Password Settings" else "Decryption Password",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        if (selectedTab == 0) {
                            OutlinedTextField(
                                value = userPassword,
                                onValueChange = { userPassword = it },
                                label = { Text("User Password (Required to open)") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = ownerPassword,
                                onValueChange = { ownerPassword = it },
                                label = { Text("Owner Password (Admin / Permissions)") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = decryptPassword,
                                onValueChange = { decryptPassword = it },
                                label = { Text("Enter Document Password") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = passwordVisible,
                                onCheckedChange = { passwordVisible = it }
                            )
                            Text("Show Password", fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                val defaultName = if (selectedTab == 0) "encrypted_$fileName" else "decrypted_$fileName"
                                saveFileLauncher.launch(defaultName)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing && (if (selectedTab == 0) userPassword.isNotBlank() else decryptPassword.isNotBlank())
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (selectedTab == 0) "Encrypting..." else "Decrypting...")
                            } else {
                                Icon(if (selectedTab == 0) Icons.Default.Security else Icons.Default.LockOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (selectedTab == 0) "Encrypt & Save PDF" else "Decrypt & Save PDF")
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
