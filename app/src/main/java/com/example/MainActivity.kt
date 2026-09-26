package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
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
    private var pendingUri: Uri? = null
    lateinit var pdfViewModel: PdfViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractUriFromIntent(intent)

        setContent {
            val viewModel: PdfViewModel = viewModel()
            pdfViewModel = viewModel
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val incomingUri by viewModel.incomingPdfUri.collectAsState()

            LaunchedEffect(Unit) {
                pendingUri?.let { uri ->
                    viewModel.setIncomingPdfUri(uri)
                    pendingUri = null
                }
            }

            MyApplicationTheme(darkTheme = isDarkMode) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(onTimeout = { showSplash = false })
                } else {
                    val navController = rememberNavController()

                    LaunchedEffect(incomingUri) {
                        if (incomingUri != null) {
                            navController.navigate("view") {
                                launchSingleTop = true
                            }
                        }
                    }

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigate = { route -> navController.navigate(route) }
                            )
                        }
                        composable("view") {
                            ViewPdfScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("merge") {
                            MergePdfScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("split") {
                            SplitPdfScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("compress") {
                            CompressPdfScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("watermark") {
                            WatermarkScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("img2pdf") {
                            ImagesToPdfScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("pdf2img") {
                            PdfToImagesScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("rotate") {
                            RotatePdfScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("auto_orient") {
                            AutoOrientPdfScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("annotate") {
                            AnnotatePdfScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("flatten") {
                            FlattenPdfScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("reorder") {
                            ReorderPdfScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("encrypt") {
                            EncryptPdfScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("ocr") {
                            OcrPdfScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("metadata") {
                            MetadataPdfScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigate = { route -> navController.navigate(route) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("privacy_policy") {
                            PrivacyPolicyScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("terms_of_service") {
                            TermsOfServiceScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractUriFromIntent(intent)
    }

    private fun extractUriFromIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Ignore
                }
                if (::pdfViewModel.isInitialized) {
                    pdfViewModel.setIncomingPdfUri(uri)
                } else {
                    pendingUri = uri
                }
            }
        }
    }
}
