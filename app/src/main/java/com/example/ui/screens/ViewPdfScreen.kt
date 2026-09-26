package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PdfViewModel
import com.example.utils.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewPdfScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var currentPage by remember { mutableStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fileName by remember { mutableStateOf("Select a PDF") }
    var fileSizeStr by remember { mutableStateOf("") }
    var isGalleryView by remember { mutableStateOf(false) }

    // Zoom & Pan state
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val incomingUri by viewModel.incomingPdfUri.collectAsState()

    fun loadUri(uri: Uri) {
        selectedUri = uri
        val count = PdfUtils.getPageCount(context, uri)
        pageCount = count
        currentPage = 0
        fileName = uri.lastPathSegment ?: "Document.pdf"
        fileSizeStr = PdfUtils.getFileSize(context, uri)
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        if (count > 0) {
            currentBitmap = PdfUtils.renderPageToBitmap(context, uri, 0, 1000, 1414)
        }
        viewModel.addHistory(fileName, uri.toString(), "Viewed")
    }

    LaunchedEffect(incomingUri) {
        incomingUri?.let { uri ->
            loadUri(uri)
            viewModel.setIncomingPdfUri(null)
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            loadUri(uri)
        }
    }

    LaunchedEffect(selectedUri) {
        selectedUri?.let { uri ->
            fileSizeStr = PdfUtils.getFileSize(context, uri)
        }
    }

    LaunchedEffect(currentPage, selectedUri) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        selectedUri?.let { uri ->
            if (pageCount > 0 && !isGalleryView) {
                currentBitmap = PdfUtils.renderPageToBitmap(context, uri, currentPage, 1000, 1414)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(fileName, maxLines = 1, fontSize = 16.sp)
                        if (fileSizeStr.isNotEmpty()) {
                            Text("Size: $fileSizeStr • $pageCount pages", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedUri != null && pageCount > 0) {
                        IconButton(onClick = { isGalleryView = !isGalleryView }) {
                            Icon(
                                if (isGalleryView) Icons.Default.MenuBook else Icons.Default.GridView,
                                contentDescription = if (isGalleryView) "Switch to Reader" else "Switch to Gallery"
                            )
                        }
                    }
                    IconButton(onClick = { filePicker.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Open PDF")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (selectedUri == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No PDF Selected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Choose a PDF file from your device to view and inspect locally.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { filePicker.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select PDF File")
                    }
                }
            } else if (isGalleryView) {
                // Thumbnail Gallery View Component
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Page Thumbnails ($pageCount pages)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Button(onClick = { isGalleryView = false }) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back to Reader")
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pageCount) { pageIndex ->
                            PdfPageThumbnailCard(
                                context = context,
                                uri = selectedUri!!,
                                pageIndex = pageIndex,
                                isSelected = pageIndex == currentPage,
                                onClick = {
                                    currentPage = pageIndex
                                    isGalleryView = false
                                }
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    if (scale == 1f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentBitmap != null) {
                            Image(
                                bitmap = currentBitmap!!.asImageBitmap(),
                                contentDescription = "PDF Page",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                            )
                        } else {
                            CircularProgressIndicator()
                        }

                        // Zoom Controls Overlay
                        if (currentBitmap != null) {
                            Surface(
                                tonalElevation = 6.dp,
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            scale = (scale - 0.5f).coerceIn(1f, 5f)
                                            if (scale == 1f) {
                                                offsetX = 0f
                                                offsetY = 0f
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        text = "${(scale * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            scale = (scale + 0.5f).coerceIn(1f, 5f)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
                                    }
                                    if (scale > 1f) {
                                        IconButton(
                                            onClick = {
                                                scale = 1f
                                                offsetX = 0f
                                                offsetY = 0f
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Navigation Controls
                    Surface(
                        tonalElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { if (currentPage > 0) currentPage-- },
                                enabled = currentPage > 0
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = null)
                                Text("Previous")
                            }

                            TextButton(onClick = { isGalleryView = true }) {
                                Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Page ${currentPage + 1} / $pageCount")
                            }

                            Button(
                                onClick = { if (currentPage < pageCount - 1) currentPage++ },
                                enabled = currentPage < pageCount - 1
                            ) {
                                Text("Next")
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPageThumbnailCard(
    context: android.content.Context,
    uri: Uri,
    pageIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri, pageIndex) {
        withContext(Dispatchers.IO) {
            thumbnailBitmap = PdfUtils.renderPageToBitmap(context, uri, pageIndex, 300, 424)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = "Thumbnail Page ${pageIndex + 1}",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Page ${pageIndex + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
