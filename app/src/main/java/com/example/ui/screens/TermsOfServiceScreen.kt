package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PdfViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms of Service") },
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "End User License Agreement",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "Effective date: September 2026",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Text("1. Acceptance of Terms", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "By downloading, installing, or using PDF App, you agree to be bound by these Terms of Service. If you do not agree to these terms, please do not use the application.",
                fontSize = 14.sp
            )

            Text("2. Use License", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "PDF App is provided as a local, offline productivity utility to view, merge, split, compress, annotate, watermark, convert, and manage PDF documents on your personal Android device. You agree to use the app only for lawful purposes and in accordance with applicable local laws.",
                fontSize = 14.sp
            )

            Text("3. Intellectual Property", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "All app source code, user interface designs, logos, and graphics are the intellectual property of Azazmadkiya. You retain 100% ownership of any documents or files you process using the app.",
                fontSize = 14.sp
            )

            Text("4. Disclaimer of Warranties", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "The application is provided \"as is\" without warranty of any kind, either express or implied. While we strive for high reliability in document rendering and processing, we do not guarantee that the app will be error-free or uninterrupted. You are advised to keep backups of important documents.",
                fontSize = 14.sp
            )

            Text("5. Limitation of Liability", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "In no event shall the developer be liable for any direct, indirect, incidental, or consequential damages arising out of or in connection with the use or inability to use the application or processed documents.",
                fontSize = 14.sp
            )

            Text("6. Contact & Support", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "For support, feedback, or inquiries, please visit:\nDeveloper: Azazmadkiya\nWebsite: https://azazmadkiya.morbi.store",
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
