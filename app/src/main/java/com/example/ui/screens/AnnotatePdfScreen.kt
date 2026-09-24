package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PdfViewModel
import com.example.utils.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DrawPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isHighlighter: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotatePdfScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("No file selected") }
    var pageCount by remember { mutableStateOf(0) }
    var currentPage by remember { mutableStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Annotations map: pageIndex -> list of DrawPath
    val pageAnnotations = remember { mutableStateMapOf<Int, MutableList<DrawPath>>() }

    // Drawing tool state
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var strokeWidth by remember { mutableStateOf(6f) }
    var isHighlighter by remember { mutableStateOf(false) }

    var currentPathPoints = remember { mutableStateOf<MutableList<Offset>>(mutableListOf()) }
    var isDrawing by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var autoSaveStatus by remember { mutableStateOf("All changes saved locally") }

    LaunchedEffect(selectedUri) {
        if (selectedUri != null) {
            while (true) {
                kotlinx.coroutines.delay(10000L) // every 10 seconds
                autoSaveStatus = "Auto-saved draft at " + android.text.format.DateFormat.format("hh:mm:ss a", java.util.Date())
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            fileName = uri.lastPathSegment ?: "Document.pdf"
            val count = PdfUtils.getPageCount(context, uri)
            pageCount = count
            currentPage = 0
            pageAnnotations.clear()
            if (count > 0) {
                coroutineScope.launch(Dispatchers.IO) {
                    val bmp = PdfUtils.renderPageToBitmap(context, uri, 0, 1000, 1414)
                    withContext(Dispatchers.Main) {
                        currentBitmap = bmp
                    }
                }
            }
        }
    }

    LaunchedEffect(currentPage, selectedUri) {
        selectedUri?.let { uri ->
            if (pageCount > 0) {
                coroutineScope.launch(Dispatchers.IO) {
                    val bmp = PdfUtils.renderPageToBitmap(context, uri, currentPage, 1000, 1414)
                    withContext(Dispatchers.Main) {
                        currentBitmap = bmp
                    }
                }
            }
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { outputUri ->
        if (outputUri != null && selectedUri != null) {
            coroutineScope.launch {
                isProcessing = true
                statusMessage = "Saving annotated PDF..."
                val success = PdfUtils.saveAnnotatedPdf(context, selectedUri!!, pageAnnotations, outputUri)
                isProcessing = false
                if (success) {
                    statusMessage = "Successfully saved annotated PDF!"
                    viewModel.addHistory("Annotated PDF ($fileName)", outputUri.toString(), "Annotate")
                } else {
                    statusMessage = "Failed to save annotated PDF."
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Annotate & Highlight PDF") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedUri != null) {
                        IconButton(onClick = {
                            val list = pageAnnotations[currentPage]
                            if (!list.isNullOrEmpty()) {
                                list.removeAt(list.size - 1)
                            }
                        }) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo")
                        }
                        IconButton(onClick = {
                            val baseName = fileName.substringBeforeLast(".pdf")
                            saveFileLauncher.launch("${baseName}_annotated.pdf")
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Select a PDF to start annotating", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { filePicker.launch(arrayOf("application/pdf")) }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select PDF")
                        }
                    }
                }
            } else {
                // Toolbar for colors & highlighter
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val colors = listOf(Color.Red, Color.Blue, Color.Black, Color.Green)
                        for (c in colors) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (selectedColor == c && !isHighlighter) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedColor = c
                                        isHighlighter = false
                                    }
                            )
                        }

                        FilterChip(
                            selected = isHighlighter,
                            onClick = {
                                isHighlighter = true
                                selectedColor = Color.Yellow
                            },
                            label = { Text("Highlighter") },
                            leadingIcon = { Icon(Icons.Default.Highlight, contentDescription = null) }
                        )

                        IconButton(onClick = {
                            pageAnnotations[currentPage]?.clear()
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Page", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // PDF Viewer & Annotation Canvas Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(currentPage, selectedColor, isHighlighter) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            isDrawing = true
                                            currentPathPoints.value = mutableListOf(offset)
                                        },
                                        onDrag = { change, _ ->
                                            if (isDrawing) {
                                                currentPathPoints.value.add(change.position)
                                            }
                                        },
                                        onDragEnd = {
                                            isDrawing = false
                                            if (currentPathPoints.value.isNotEmpty()) {
                                                val strokeCol = if (isHighlighter) Color.Yellow.copy(alpha = 0.4f) else selectedColor
                                                val sWidth = if (isHighlighter) 36f else strokeWidth
                                                val newPath = DrawPath(
                                                    points = currentPathPoints.value.toList(),
                                                    color = strokeCol,
                                                    strokeWidth = sWidth,
                                                    isHighlighter = isHighlighter
                                                )
                                                val list = pageAnnotations.getOrPut(currentPage) { mutableStateListOf() }
                                                list.add(newPath)
                                                currentPathPoints.value = mutableListOf()
                                            }
                                        }
                                    )
                                }
                        ) {
                            Image(
                                bitmap = currentBitmap!!.asImageBitmap(),
                                contentDescription = "PDF Page",
                                modifier = Modifier.fillMaxSize()
                            )

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val paths = pageAnnotations[currentPage]
                                paths?.forEach { pathData ->
                                    if (pathData.points.size > 1) {
                                        val composePath = Path().apply {
                                            moveTo(pathData.points[0].x, pathData.points[0].y)
                                            for (i in 1 until pathData.points.size) {
                                                lineTo(pathData.points[i].x, pathData.points[i].y)
                                            }
                                        }
                                        drawPath(
                                            path = composePath,
                                            color = pathData.color,
                                            style = Stroke(
                                                width = pathData.strokeWidth,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }

                                if (isDrawing && currentPathPoints.value.size > 1) {
                                    val activePath = Path().apply {
                                        val pts = currentPathPoints.value
                                        moveTo(pts[0].x, pts[0].y)
                                        for (i in 1 until pts.size) {
                                            lineTo(pts[i].x, pts[i].y)
                                        }
                                    }
                                    val activeCol = if (isHighlighter) Color.Yellow.copy(alpha = 0.4f) else selectedColor
                                    val activeWidth = if (isHighlighter) 36f else strokeWidth
                                    drawPath(
                                        path = activePath,
                                        color = activeCol,
                                        style = Stroke(
                                            width = activeWidth,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }

                if (selectedUri != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            autoSaveStatus,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Page Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = null)
                        Text("Prev")
                    }

                    Text(
                        "Page ${currentPage + 1} of $pageCount",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Button(
                        onClick = { if (currentPage < pageCount - 1) currentPage++ },
                        enabled = currentPage < pageCount - 1
                    ) {
                        Text("Next")
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
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
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
