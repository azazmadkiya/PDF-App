package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.PdfViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val pdfViewModel: PdfViewModel = viewModel()
            val isDarkMode by pdfViewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(onTimeout = { showSplash = false })
                } else {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = pdfViewModel,
                                onNavigate = { route -> navController.navigate(route) }
                            )
                        }
                        composable("view") {
                            ViewPdfScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("merge") {
                            MergePdfScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("split") {
                            SplitPdfScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("compress") {
                            CompressPdfScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("watermark") {
                            WatermarkScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("img2pdf") {
                            ImagesToPdfScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("pdf2img") {
                            PdfToImagesScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("rotate") {
                            RotatePdfScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("auto_orient") {
                            AutoOrientPdfScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("annotate") {
                            AnnotatePdfScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("flatten") {
                            FlattenPdfScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("reorder") {
                            ReorderPdfScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("encrypt") {
                            EncryptPdfScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("ocr") {
                            OcrPdfScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("metadata") {
                            MetadataPdfScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = pdfViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
