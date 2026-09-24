package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PdfHistoryEntity
import com.example.ui.PdfViewModel
import java.text.SimpleDateFormat
import java.util.*

data class PdfTool(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PdfViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val history by viewModel.historyList.collectAsState()

    val tools = listOf(
        PdfTool("View & Read", "Open & view any PDF locally", Icons.Default.Visibility, "view"),
        PdfTool("Merge PDFs", "Combine multiple PDFs", Icons.Default.CallMerge, "merge"),
        PdfTool("Split PDF", "Extract page ranges", Icons.Default.CallSplit, "split"),
        PdfTool("Compress PDF", "Reduce file size & optimize", Icons.Default.Compress, "compress"),
        PdfTool("Watermark", "Add text & page numbers", Icons.Default.WaterDrop, "watermark"),
        PdfTool("Images to PDF", "Convert JPG/PNG to PDF", Icons.Default.Image, "img2pdf"),
        PdfTool("PDF to Images", "Export pages as high-res PNG/JPEG", Icons.Default.PhotoLibrary, "pdf2img"),
        PdfTool("Rotate PDF", "Rotate pages by 90, 180, or 270 degrees", Icons.Default.RotateRight, "rotate"),
        PdfTool("Auto-Orient", "Detect & auto-rotate sideways scanned pages", Icons.Default.AutoFixHigh, "auto_orient"),
        PdfTool("Annotate PDF", "Free-hand drawing & text highlighting", Icons.Default.Edit, "annotate"),
        PdfTool("Flatten PDF", "Flatten forms & annotations into static layer", Icons.Default.Layers, "flatten"),
        PdfTool("Reorder Pages", "Visually reorder or swap PDF pages", Icons.Default.SwapVert, "reorder"),
        PdfTool("Encrypt PDF", "Password protect PDF documents", Icons.Default.Lock, "encrypt"),
        PdfTool("OCR & Text", "Extract searchable text & export .txt", Icons.Default.DocumentScanner, "ocr"),
        PdfTool("Edit Metadata", "View & edit title, author, & properties", Icons.Default.EditNote, "metadata"),
        PdfTool("Settings", "Privacy info & history", Icons.Default.Settings, "settings")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("PDF App", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("100% On-Device & Private", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                },
                actions = {
                    val isDarkMode by viewModel.isDarkMode.collectAsState()
                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    IconButton(onClick = { onNavigate("settings") }) {
                        Icon(Icons.Default.Info, contentDescription = "Privacy Info")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Privacy Banner Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Zero Server Uploads",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "All document processing happens locally on your device. Your files never leave your phone.",
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "PDF Tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item {
                // Tools Grid
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (chunk in tools.chunked(2)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (tool in chunk) {
                                ToolCard(
                                    tool = tool,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigate(tool.route) }
                                )
                            }
                            if (chunk.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text("Clear")
                        }
                    }
                }
            }

            if (history.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No recent documents processed yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(history) { item ->
                    HistoryItemCard(
                        item = item,
                        onOpen = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(item.uriString), "application/pdf")
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback or view inside app
                                onNavigate("view")
                            }
                        },
                        onDelete = { viewModel.deleteHistory(item) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ToolCard(
    tool: PdfTool,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    tool.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                tool.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                tool.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
fun HistoryItemCard(
    item: PdfHistoryEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(item.timestamp) {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${item.actionType} • $dateStr",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onOpen) {
                Icon(Icons.Default.Visibility, contentDescription = "Open")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
