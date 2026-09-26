package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
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
fun PrivacyPolicyScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Your Privacy is Fully Protected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Last updated: September 2026",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Text("1. Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "PDF App (\"we\", \"our\", or \"us\") is committed to protecting your privacy. This Privacy Policy explains how your information is handled when you use our Android application for viewing, editing, merging, splitting, compressing, and managing PDF documents.",
                fontSize = 14.sp
            )

            Text("2. 100% On-Device Processing & No Data Collection", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "• **No Server Uploads:** All PDF viewing, manipulation, conversion (PDF to Image, Image to PDF), OCR extraction, watermarking, encryption, and editing operations take place entirely locally on your device's processor and storage.\n" +
                "• **No Personal Data Collection:** We do not collect, store, transmit, or share any personal information, documents, text contents, or metadata with any external servers or third parties.\n" +
                "• **No Analytics/Tracking:** We do not track user behavior or embed third-party advertising tracking SDKs.",
                fontSize = 14.sp
            )

            Text("3. Device Permissions & File Access", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "• **Storage & Document Access:** The app accesses local PDF files or files opened via the system file manager / ACTION_VIEW intents solely to perform the requested PDF utility actions.\n" +
                "• **Permissions Usage:** We request only the minimum necessary permissions required to operate offline document utilities.",
                fontSize = 14.sp
            )

            Text("4. Local Database & History", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "The app stores recent activity history (file names and local file paths/URIs) in an encrypted local Room database on your device to make it easy for you to access your recent documents. You can clear this history at any time directly within the app settings.",
                fontSize = 14.sp
            )

            Text("5. Children's Privacy", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "Our application does not knowingly collect any information from children under the age of 13. Since no personal data is collected from any user, the app is completely safe for all age groups.",
                fontSize = 14.sp
            )

            Text("6. Changes to This Privacy Policy", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "We may update our Privacy Policy from time to time. Any changes will be reflected within this screen with an updated revision date.",
                fontSize = 14.sp
            )

            Text("7. Contact Us", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "If you have any questions or suggestions regarding this Privacy Policy, please contact us at:\nDeveloper: Azazmadkiya\nWebsite: https://azazmadkiya.morbi.store",
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
