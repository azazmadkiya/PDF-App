package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PdfUtils {

    fun getPageCount(context: Context, uri: Uri): Int {
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (parcelFileDescriptor != null) {
                renderer = PdfRenderer(parcelFileDescriptor)
                renderer.pageCount
            } else 0
        } catch (e: Exception) {
            0
        } finally {
            try {
                renderer?.close()
                parcelFileDescriptor?.close()
            } catch (ignored: Exception) {}
        }
    }

    fun renderPageToBitmap(context: Context, uri: Uri, pageIndex: Int, width: Int, height: Int): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                if (pageIndex in 0 until renderer.pageCount) {
                    page = renderer.openPage(pageIndex)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                } else null
            } else null
        } catch (e: Exception) {
            null
        } finally {
            try {
                page?.close()
                renderer?.close()
                pfd?.close()
            } catch (ignored: Exception) {}
        }
    }

    fun mergePdfs(context: Context, sourceUris: List<Uri>, outputUri: Uri): Boolean {
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
            val merger = PDFMergerUtility()

            val tempFiles = mutableListOf<File>()
            for (uri in sourceUris) {
                val tempFile = File.createTempFile("merge_src_", ".pdf", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFiles.add(tempFile)
                merger.addSource(tempFile)
            }

            val outputTempFile = File.createTempFile("merge_out_", ".pdf", context.cacheDir)
            merger.destinationFileName = outputTempFile.absolutePath
            merger.mergeDocuments(null)

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                outputTempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            for (f in tempFiles) {
                try { f.delete() } catch (ignored: Exception) {}
            }
            try { outputTempFile.delete() } catch (ignored: Exception) {}

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun splitPdf(context: Context, sourceUri: Uri, startPage: Int, endPage: Int, outputUri: Uri): Boolean {
        val pdfDocument = PdfDocument()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val maxIdx = (renderer.pageCount - 1).coerceAtLeast(0)
                val s = startPage.coerceIn(0, maxIdx)
                val e = endPage.coerceIn(s, maxIdx)
                
                var newPageNum = 1
                for (i in s..e) {
                    val page = renderer.openPage(i)
                    val width = page.width
                    val height = page.height
                    
                    val pageInfo = PdfDocument.PageInfo.Builder(width, height, newPageNum).create()
                    val pdfPage = pdfDocument.startPage(pageInfo)
                    
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(pdfPage)
                    
                    bitmap.recycle()
                    page.close()
                    newPageNum++
                }
            }

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            renderer?.close()
            pfd?.close()
            pdfDocument.close()
        }
    }

    fun addWatermarkAndPageNumbers(
        context: Context,
        sourceUri: Uri,
        watermarkText: String,
        includePageNumbers: Boolean,
        outputUri: Uri
    ): Boolean {
        return addWatermarkAndPageNumbers(
            context = context,
            sourceUri = sourceUri,
            isTextWatermark = true,
            watermarkText = watermarkText,
            watermarkImageUri = null,
            opacity = 0.3f,
            rotationAngle = -45f,
            includePageNumbers = includePageNumbers,
            outputUri = outputUri
        )
    }

    fun addWatermarkAndPageNumbers(
        context: Context,
        sourceUri: Uri,
        isTextWatermark: Boolean,
        watermarkText: String,
        watermarkImageUri: Uri?,
        opacity: Float,
        rotationAngle: Float,
        includePageNumbers: Boolean,
        outputUri: Uri
    ): Boolean {
        val pdfDocument = PdfDocument()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var watermarkBitmap: Bitmap? = null

        try {
            if (!isTextWatermark && watermarkImageUri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(watermarkImageUri)
                    watermarkBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val totalPages = renderer.pageCount
                
                val alphaVal = (opacity * 255).toInt().coerceIn(0, 255)

                val paint = Paint().apply {
                    color = Color.argb(alphaVal, 80, 80, 80)
                    textSize = 56f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }

                val footerPaint = Paint().apply {
                    color = Color.parseColor("#88000000")
                    textSize = 14f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }

                for (i in 0 until totalPages) {
                    val page = renderer.openPage(i)
                    val width = page.width
                    val height = page.height
                    
                    val pageInfo = PdfDocument.PageInfo.Builder(width, height, i + 1).create()
                    val pdfPage = pdfDocument.startPage(pageInfo)
                    
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    val canvas = pdfPage.canvas
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    
                    // Draw Watermark
                    if (isTextWatermark && watermarkText.isNotBlank()) {
                        canvas.save()
                        canvas.rotate(rotationAngle, width / 2f, height / 2f)
                        canvas.drawText(watermarkText, width / 2f, height / 2f, paint)
                        canvas.restore()
                    } else if (!isTextWatermark && watermarkBitmap != null) {
                        val imgPaint = Paint().apply {
                            alpha = alphaVal
                            isAntiAlias = true
                        }
                        canvas.save()
                        canvas.rotate(rotationAngle, width / 2f, height / 2f)
                        val destWidth = (width * 0.4f).toInt()
                        val destHeight = (watermarkBitmap.height * (destWidth.toFloat() / watermarkBitmap.width)).toInt()
                        val left = (width - destWidth) / 2f
                        val top = (height - destHeight) / 2f
                        val destRect = android.graphics.RectF(left, top, left + destWidth, top + destHeight)
                        canvas.drawBitmap(watermarkBitmap, null, destRect, imgPaint)
                        canvas.restore()
                    }

                    // Draw Page Numbers
                    if (includePageNumbers) {
                        val pageStr = "Page ${i + 1} of $totalPages"
                        canvas.drawText(pageStr, width / 2f, height - 30f, footerPaint)
                    }

                    pdfDocument.finishPage(pdfPage)
                    bitmap.recycle()
                    page.close()
                }
            }

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                watermarkBitmap?.recycle()
            } catch (ignored: Exception) {}
            renderer?.close()
            pfd?.close()
            pdfDocument.close()
        }
    }

    fun convertImagesToPdf(context: Context, imageUris: List<Uri>, outputUri: Uri): Boolean {
        val pdfDocument = PdfDocument()
        try {
            for ((index, uri) in imageUris.withIndex()) {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val width = bitmap.width.coerceAtLeast(595) // standard A4-ish width minimum
                    val height = bitmap.height.coerceAtLeast(842)

                    val pageInfo = PdfDocument.PageInfo.Builder(width, height, index + 1).create()
                    val pdfPage = pdfDocument.startPage(pageInfo)
                    
                    // Scale bitmap to fit page
                    pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(pdfPage)
                    bitmap.recycle()
                }
            }

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            pdfDocument.close()
        }
    }

    fun compressPdf(context: Context, sourceUri: Uri, quality: Int, outputUri: Uri): Boolean {
        val pdfDocument = PdfDocument()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val totalPages = renderer.pageCount
                for (i in 0 until totalPages) {
                    val page = renderer.openPage(i)
                    val scaleFactor = when {
                        quality <= 50 -> 0.75f
                        quality <= 75 -> 0.9f
                        else -> 1.0f
                    }
                    val width = (page.width * scaleFactor).toInt().coerceAtLeast(200)
                    val height = (page.height * scaleFactor).toInt().coerceAtLeast(200)

                    val pageInfo = PdfDocument.PageInfo.Builder(width, height, i + 1).create()
                    val pdfPage = pdfDocument.startPage(pageInfo)

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                    val compressedBytes = stream.toByteArray()
                    val compressedBitmap = android.graphics.BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

                    if (compressedBitmap != null) {
                        pdfPage.canvas.drawBitmap(compressedBitmap, 0f, 0f, null)
                        compressedBitmap.recycle()
                    } else {
                        pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    }

                    pdfDocument.finishPage(pdfPage)
                    bitmap.recycle()
                    page.close()
                }
            }

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            renderer?.close()
            pfd?.close()
            pdfDocument.close()
        }
    }

    fun exportPdfPagesToImages(
        context: Context,
        sourceUri: Uri,
        outputTreeUri: Uri,
        format: Bitmap.CompressFormat,
        quality: Int,
        scaleMultiplier: Float
    ): Int {
        var exportedCount = 0
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val totalPages = renderer.pageCount
                val resolver = context.contentResolver
                val docName = sourceUri.lastPathSegment?.substringBeforeLast('.') ?: "document"
                val rootDoc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, outputTreeUri)

                for (i in 0 until totalPages) {
                    val page = renderer.openPage(i)
                    val width = (page.width * scaleMultiplier).toInt().coerceAtLeast(600)
                    val height = (page.height * scaleMultiplier).toInt().coerceAtLeast(800)

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val ext = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
                    val mimeType = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
                    val fileName = "${docName}_page_${i + 1}.$ext"

                    val targetFile = rootDoc?.createFile(mimeType, fileName)
                    if (targetFile != null) {
                        resolver.openOutputStream(targetFile.uri)?.use { outputStream ->
                            bitmap.compress(format, quality, outputStream)
                        }
                        exportedCount++
                    }

                    bitmap.recycle()
                    page.close()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            renderer?.close()
            pfd?.close()
        }
        return exportedCount
    }

    fun exportSinglePageToImage(
        context: Context,
        sourceUri: Uri,
        pageIndex: Int,
        outputUri: Uri,
        format: Bitmap.CompressFormat,
        quality: Int,
        scaleMultiplier: Float
    ): Boolean {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                if (pageIndex in 0 until renderer.pageCount) {
                    val page = renderer.openPage(pageIndex)
                    val width = (page.width * scaleMultiplier).toInt().coerceAtLeast(600)
                    val height = (page.height * scaleMultiplier).toInt().coerceAtLeast(800)

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                        bitmap.compress(format, quality, outputStream)
                    }

                    bitmap.recycle()
                    page.close()
                    true
                } else false
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    fun rotatePdf(
        context: Context,
        sourceUri: Uri,
        rotationAngle: Int, // 90, 180, 270
        specificPages: List<Int>?, // null or empty means all pages
        outputUri: Uri
    ): Boolean {
        val pdfDocument = PdfDocument()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val totalPages = renderer.pageCount
                
                for (i in 0 until totalPages) {
                    val page = renderer.openPage(i)
                    val origWidth = page.width
                    val origHeight = page.height
                    
                    val shouldRotate = specificPages == null || specificPages.isEmpty() || specificPages.contains(i)
                    val angle = if (shouldRotate) rotationAngle else 0
                    
                    val isSwap = angle == 90 || angle == 270 || angle == -90 || angle == -270
                    val newWidth = if (isSwap) origHeight else origWidth
                    val newHeight = if (isSwap) origWidth else origHeight
                    
                    val pageInfo = PdfDocument.PageInfo.Builder(newWidth, newHeight, i + 1).create()
                    val pdfPage = pdfDocument.startPage(pageInfo)
                    
                    val bitmap = Bitmap.createBitmap(origWidth, origHeight, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    val canvas = pdfPage.canvas
                    canvas.save()
                    canvas.translate(newWidth / 2f, newHeight / 2f)
                    canvas.rotate(angle.toFloat())
                    canvas.translate(-origWidth / 2f, -origHeight / 2f)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    canvas.restore()
                    
                    pdfDocument.finishPage(pdfPage)
                    bitmap.recycle()
                    page.close()
                }
            }

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            renderer?.close()
            pfd?.close()
            pdfDocument.close()
        }
    }

    fun reorderPdf(
        context: Context,
        sourceUri: Uri,
        newOrder: List<Int>,
        outputUri: Uri
    ): Boolean {
        val pdfDocument = PdfDocument()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                for ((newPageIndex, origIndex) in newOrder.withIndex()) {
                    if (origIndex in 0 until renderer.pageCount) {
                        val page = renderer.openPage(origIndex)
                        val width = page.width
                        val height = page.height

                        val pageInfo = PdfDocument.PageInfo.Builder(width, height, newPageIndex + 1).create()
                        val pdfPage = pdfDocument.startPage(pageInfo)

                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val canvas = pdfPage.canvas
                        canvas.drawBitmap(bitmap, 0f, 0f, null)

                        pdfDocument.finishPage(pdfPage)
                        bitmap.recycle()
                        page.close()
                    }
                }
            }

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            renderer?.close()
            pfd?.close()
            pdfDocument.close()
        }
    }

    fun encryptPdf(
        context: Context,
        sourceUri: Uri,
        ownerPassword: String,
        userPassword: String,
        outputUri: Uri
    ): Boolean {
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            val document = PDDocument.load(inputStream)
            inputStream?.close()

            val ap = AccessPermission().apply {
                setCanAssembleDocument(false)
                setCanExtractContent(false)
                setCanExtractForAccessibility(true)
                setCanModify(false)
                setCanModifyAnnotations(false)
                setCanPrint(true)
                setCanPrintDegraded(true)
            }

            val spp = StandardProtectionPolicy(ownerPassword, userPassword, ap).apply {
                encryptionKeyLength = 128
            }

            document.protect(spp)

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                document.save(outputStream)
            }
            document.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun decryptPdf(
        context: Context,
        sourceUri: Uri,
        password: String,
        outputUri: Uri
    ): Boolean {
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            val document = PDDocument.load(inputStream, password)
            inputStream?.close()

            document.isAllSecurityToBeRemoved = true

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                document.save(outputStream)
            }
            document.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    data class PdfMetadata(
        val title: String,
        val author: String,
        val subject: String,
        val creator: String,
        val producer: String,
        val creationDate: String
    )

    fun getPdfMetadata(context: Context, uri: Uri): PdfMetadata {
        return try {
            PDFBoxResourceLoader.init(context.applicationContext)
            val inputStream = context.contentResolver.openInputStream(uri)
            val document = PDDocument.load(inputStream)
            inputStream?.close()
            val info = document.documentInformation
            val title = info.title ?: ""
            val author = info.author ?: ""
            val subject = info.subject ?: ""
            val creator = info.creator ?: ""
            val producer = info.producer ?: ""
            val cal = info.creationDate
            val dateStr = if (cal != null) {
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(cal.time)
            } else ""
            document.close()
            PdfMetadata(title, author, subject, creator, producer, dateStr)
        } catch (e: Exception) {
            e.printStackTrace()
            PdfMetadata("", "", "", "", "", "")
        }
    }

    fun updatePdfMetadata(
        context: Context,
        sourceUri: Uri,
        title: String,
        author: String,
        subject: String,
        creator: String,
        outputUri: Uri
    ): Boolean {
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            val document = PDDocument.load(inputStream)
            inputStream?.close()

            val info = document.documentInformation
            info.title = title
            info.author = author
            info.subject = subject
            info.creator = creator

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                document.save(outputStream)
            }
            document.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun extractTextFromPdf(context: Context, uri: Uri): String {
        return try {
            PDFBoxResourceLoader.init(context.applicationContext)
            val inputStream = context.contentResolver.openInputStream(uri)
            val document = PDDocument.load(inputStream)
            inputStream?.close()
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            text ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun extractTextByPages(context: Context, uri: Uri): List<String> {
        val pagesText = mutableListOf<String>()
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
            val inputStream = context.contentResolver.openInputStream(uri)
            val document = PDDocument.load(inputStream)
            inputStream?.close()
            val stripper = PDFTextStripper()
            val totalPages = document.numberOfPages
            for (i in 1..totalPages) {
                stripper.startPage = i
                stripper.endPage = i
                val pageText = stripper.getText(document) ?: ""
                pagesText.add(pageText.trim())
            }
            document.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return pagesText
    }

    fun autoOrientPdf(context: Context, sourceUri: Uri, outputUri: Uri): Boolean {
        val pdfDocument = PdfDocument()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount
                
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    val origWidth = page.width
                    val origHeight = page.height
                    
                    var rotateNeeded = 0
                    if (origWidth > origHeight) {
                        rotateNeeded = 90
                    }

                    val targetWidth = if (rotateNeeded == 90 || rotateNeeded == 270) origHeight else origWidth
                    val targetHeight = if (rotateNeeded == 90 || rotateNeeded == 270) origWidth else origHeight

                    val pageInfo = PdfDocument.PageInfo.Builder(targetWidth, targetHeight, i + 1).create()
                    val pdfPage = pdfDocument.startPage(pageInfo)

                    val bitmap = Bitmap.createBitmap(origWidth, origHeight, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val canvas = pdfPage.canvas
                    if (rotateNeeded != 0) {
                        canvas.save()
                        canvas.translate(targetWidth / 2f, targetHeight / 2f)
                        canvas.rotate(rotateNeeded.toFloat())
                        canvas.translate(-origWidth / 2f, -origHeight / 2f)
                        canvas.drawBitmap(bitmap, 0f, 0f, null)
                        canvas.restore()
                    } else {
                        canvas.drawBitmap(bitmap, 0f, 0f, null)
                    }

                    pdfDocument.finishPage(pdfPage)
                    bitmap.recycle()
                    page.close()
                }
            }

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                pdfDocument.close()
                renderer?.close()
                pfd?.close()
            } catch (ignored: Exception) {}
        }
    }

    fun saveAnnotatedPdf(
        context: Context,
        sourceUri: Uri,
        annotations: Map<Int, List<com.example.ui.screens.DrawPath>>,
        outputUri: Uri
    ): Boolean {
        val pdfDocument = PdfDocument()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount
                
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    val width = page.width
                    val height = page.height

                    val pageInfo = PdfDocument.PageInfo.Builder(width, height, i + 1).create()
                    val pdfPage = pdfDocument.startPage(pageInfo)
                    val canvas = pdfPage.canvas

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val paths = annotations[i]
                    if (!paths.isNullOrEmpty()) {
                        val androidCanvas = android.graphics.Canvas(bitmap)
                        for (pathData in paths) {
                            if (pathData.points.size > 1) {
                                val paint = Paint().apply {
                                    isAntiAlias = true
                                    style = Paint.Style.STROKE
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                    color = android.graphics.Color.argb(
                                        (pathData.color.alpha * 255).toInt(),
                                        (pathData.color.red * 255).toInt(),
                                        (pathData.color.green * 255).toInt(),
                                        (pathData.color.blue * 255).toInt()
                                    )
                                    strokeWidth = pathData.strokeWidth * (width.toFloat() / 1000f)
                                }

                                val androidPath = android.graphics.Path()
                                val scaleX = width.toFloat() / 1000f
                                val scaleY = height.toFloat() / 1414f

                                androidPath.moveTo(pathData.points[0].x * scaleX, pathData.points[0].y * scaleY)
                                for (pIdx in 1 until pathData.points.size) {
                                    androidPath.lineTo(pathData.points[pIdx].x * scaleX, pathData.points[pIdx].y * scaleY)
                                }
                                androidCanvas.drawPath(androidPath, paint)
                            }
                        }
                    }

                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(pdfPage)
                    bitmap.recycle()
                    page.close()
                }
            }

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                pdfDocument.close()
                renderer?.close()
                pfd?.close()
            } catch (ignored: Exception) {}
        }
    }

    fun flattenPdf(context: Context, sourceUri: Uri, outputUri: Uri): Boolean {
        val pdfDocument = PdfDocument()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount
                
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    val width = page.width
                    val height = page.height

                    val pageInfo = PdfDocument.PageInfo.Builder(width, height, i + 1).create()
                    val pdfPage = pdfDocument.startPage(pageInfo)

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(pdfPage)
                    bitmap.recycle()
                    page.close()
                }
            }

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                pdfDocument.close()
                renderer?.close()
                pfd?.close()
            } catch (ignored: Exception) {}
        }
    }

    fun getFileSize(context: Context, uri: Uri): String {
        var sizeBytes = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex != -1) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
            if (sizeBytes == 0L) {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    sizeBytes = pfd.statSize
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (sizeBytes <= 0) return "Unknown size"
        
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(java.util.Locale.getDefault(), "%.2f MB", mb)
        } else {
            String.format(java.util.Locale.getDefault(), "%.1f KB", kb)
        }
    }
}
