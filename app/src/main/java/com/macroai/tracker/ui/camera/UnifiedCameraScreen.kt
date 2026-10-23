package com.macroai.tracker.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.macroai.tracker.ui.recipes.IngredientPortionSelectorDialog
import com.macroai.tracker.ui.camera.BarcodeScannerDialog
import com.macroai.tracker.ui.camera.SwapIngredientDialog
import java.util.concurrent.Executors

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedCameraScreen(
    onNavigateBack: () -> Unit,
    viewModel: CameraCaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isCameraActive = uiState.reviewDraft is ReviewDraftState.Idle

    // Keep reference to the last captured bitmap so the full-screen analyzing & review screens can display it
    var lastCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    BackHandler(enabled = !isCameraActive) {
        viewModel.dismissReviewDraft()
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
                lastCapturedBitmap = bitmap
                viewModel.onPhotoCaptured(bitmap)
            }
        }
    }

    var showManualBarcodeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(uiState.savedSuccessEvent) {
        if (uiState.savedSuccessEvent) {
            viewModel.consumeSavedSuccessEvent()
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.reviewDraft) {
                            is ReviewDraftState.Idle -> "Scan Barcode or AI Photo"
                            is ReviewDraftState.PhotoContextPrompt -> "Meal Context"
                            is ReviewDraftState.AnalyzingLoading -> "AI Nutrition Analysis"
                            is ReviewDraftState.BarcodeReview -> "Product Nutrition"
                            is ReviewDraftState.AiMealReview -> "Review AI Meal"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isCameraActive) {
                                onNavigateBack()
                            } else {
                                viewModel.dismissReviewDraft()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isCameraActive && uiState.captureMode == CameraCaptureMode.BARCODE_SCAN) {
                        IconButton(onClick = { showManualBarcodeDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Dialpad,
                                contentDescription = "Enter Barcode Manually"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = isCameraActive,
            transitionSpec = {
                fadeIn(tween(240)) togetherWith fadeOut(tween(200))
            },
            label = "camera_vs_analysis_screen",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { showCameraView ->
            if (showCameraView) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    if (hasCameraPermission) {
                        CameraPreviewWithAnalysis(
                            captureMode = uiState.captureMode,
                            imageCapture = imageCapture,
                            cameraExecutor = cameraExecutor,
                            lifecycleOwner = lifecycleOwner,
                            onBarcodeDetected = viewModel::onBarcodeDetected
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Camera access is required for live barcode scanning and AI meal photo analysis.",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                Text("Grant Camera Permission")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text("Or pick meal photo from Gallery", color = Color.White)
                            }
                        }
                    }

                    // Center Reticle Overlay in Barcode Mode
                    if (uiState.captureMode == CameraCaptureMode.BARCODE_SCAN && hasCameraPermission) {
                        Box(
                            modifier = Modifier
                                .size(width = 280.dp, height = 160.dp)
                                .align(Alignment.Center)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        )
                    }

                    // Bottom Control Deck
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.78f))
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            CameraCaptureMode.entries.forEachIndexed { idx, mode ->
                                SegmentedButton(
                                    selected = uiState.captureMode == mode,
                                    onClick = { viewModel.setCaptureMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = idx,
                                        count = CameraCaptureMode.entries.size
                                    ),
                                    icon = {
                                        Icon(
                                            imageVector = if (mode == CameraCaptureMode.AI_PHOTO) {
                                                Icons.Default.CameraAlt
                                            } else {
                                                Icons.Default.QrCodeScanner
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                ) {
                                    Text(mode.label, maxLines = 1, softWrap = false)
                                }
                            }
                        }

                        if (uiState.captureMode == CameraCaptureMode.AI_PHOTO) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                com.macroai.tracker.domain.model.AiModel.entries.forEachIndexed { idx, model ->
                                    SegmentedButton(
                                        selected = uiState.selectedAiModel == model,
                                        onClick = { viewModel.setSelectedAiModel(model) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = idx,
                                            count = com.macroai.tracker.domain.model.AiModel.entries.size
                                        )
                                    ) {
                                        Text(
                                            text = model.shortName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Pick from Gallery",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                FloatingActionButton(
                                    onClick = {
                                        imageCapture.takePicture(
                                            cameraExecutor,
                                            object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(image: ImageProxy) {
                                                    val bitmap: Bitmap = image.toBitmap()
                                                    image.close()
                                                    lastCapturedBitmap = bitmap
                                                    viewModel.onPhotoCaptured(bitmap)
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    // Ignored or surfaced via snackbar
                                                }
                                            }
                                        )
                                    },
                                    shape = CircleShape,
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Capture Meal Photo",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.size(48.dp))
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Point camera at EAN/UPC barcode",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                TextButton(onClick = { showManualBarcodeDialog = true }) {
                                    Text("Type Code", maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            } else {
                // Dedicated Full-Screen Context, AI Analysis, and Meal Review Screen (Camera is completely unbound & hidden!)
                MealAnalysisAndReviewScreen(
                    draft = uiState.reviewDraft,
                    capturedBitmap = (uiState.reviewDraft as? ReviewDraftState.PhotoContextPrompt)?.bitmap
                        ?: lastCapturedBitmap,
                    selectedMealType = uiState.selectedMealType,
                    onMealTypeSelected = viewModel::setMealType,
                    onContextPromptChanged = viewModel::updatePhotoContextPrompt,
                    onAnalyzePhotoClicked = viewModel::submitPhotoForGeminiAnalysis,
                    onBarcodeGramsChanged = viewModel::updateBarcodeGramsInput,
                    onAiMealNameChanged = viewModel::updateAiMealName,
                    onAiIngredientWeightChanged = viewModel::updateAiIngredientWeight,
                    onConfirmSave = viewModel::confirmAndSaveCurrentDraft,
                    onCancel = viewModel::dismissReviewDraft,
                    onStartSwapIngredient = viewModel::startSwapIngredient,
                    onRemoveAiIngredient = viewModel::removeAiIngredient,
                    onBarcodeServingCountChanged = viewModel::updateBarcodeServingCount,
                    onBarcodeToggleServingMode = viewModel::toggleBarcodeServingMode,
                    onBarcodeSelectServingPreset = viewModel::selectBarcodeServingPreset,
                    selectedAiModel = uiState.selectedAiModel,
                    onAiModelSelected = viewModel::setSelectedAiModel
                )
            }
        }
    }

    // AI Guessed Ingredient Swap Dialog
    if (uiState.swapIngredientIndex != null && uiState.swapIngredientOriginal != null) {
        SwapIngredientDialog(
            originalIngredient = uiState.swapIngredientOriginal!!,
            searchQuery = uiState.swapSearchQuery,
            onSearchQueryChanged = viewModel::updateSwapSearchQuery,
            searchResults = uiState.swapSearchResults,
            isSearching = uiState.isSearchingSwap,
            onSelectProduct = viewModel::selectProductForSwap,
            onOpenBarcodeScanner = { viewModel.setShowSwapBarcodeScanner(true) },
            onDismiss = viewModel::dismissSwap
        )
    }

    // Barcode Scanner Dialog for packet ingredient swapping
    if (uiState.showSwapBarcodeScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = viewModel::onSwapBarcodeDetected,
            onDismiss = { viewModel.setShowSwapBarcodeScanner(false) }
        )
    }

    // Serving Size & Portion Selector Dialog for Swapped Ingredient
    uiState.swapSelectedProduct?.let { product ->
        IngredientPortionSelectorDialog(
            product = product,
            onConfirm = { scaledIngredient ->
                viewModel.confirmSwapIngredient(product, scaledIngredient.weightGrams)
            },
            onDismiss = viewModel::dismissSwapProductSelection
        )
    }

    if (showManualBarcodeDialog) {
        var manualCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManualBarcodeDialog = false },
            title = { Text("Enter Barcode") },
            text = {
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    label = { Text("EAN-13 / UPC Barcode") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualCode.isNotBlank()) {
                            showManualBarcodeDialog = false
                            viewModel.onBarcodeDetected(manualCode.trim())
                        }
                    }
                ) {
                    Text("Lookup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualBarcodeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreviewWithAnalysis(
    captureMode: CameraCaptureMode,
    imageCapture: ImageCapture,
    cameraExecutor: java.util.concurrent.Executor,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            }
            barcodeScanner.close()
        }
    }

    LaunchedEffect(captureMode) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (captureMode != CameraCaptureMode.BARCODE_SCAN) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val firstCode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                                    if (firstCode != null) {
                                        onBarcodeDetected(firstCode)
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}
