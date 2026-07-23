package com.scanapp.scanner

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Observer
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import com.scanapp.scanner.data.ScanSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.WHITE

        setContent {
            ScanApp(onFinish = ::finish)
        }
    }
}

@Composable
fun ScanApp(onFinish: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val barcodeScanner = rememberBarcodeScanner()
    val historyViewModel: ScanHistoryViewModel = viewModel(
        factory = ScanHistoryViewModel.factory(context.applicationContext),
    )
    val history by historyViewModel.history.collectAsStateWithLifecycle()
    var hasCameraPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var scanResult by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    var isPickingImage by remember { mutableStateOf(false) }
    var isShowingHistory by rememberSaveable { mutableStateOf(false) }
    val isCameraResultAccepted = remember { AtomicBoolean(false) }
    var lastBackPressTime by remember { mutableStateOf(0L) }

    SideEffect {
        val window = (context as Activity).window
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = isShowingHistory
    }

    fun acceptScan(text: String, source: ScanSource, successNotice: String) {
        if (source == ScanSource.CAMERA && !isCameraResultAccepted.compareAndSet(false, true)) return
        torchEnabled = false
        scanResult = text
        notice = successNotice
        historyViewModel.recordScan(text, source)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        notice = if (granted) null else "需要相机权限才能实时扫码，可继续从相册选择图片识别"
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isPickingImage = true
        notice = "正在识别相册图片..."
        scope.launch {
            val text = scanImageFromGallery(context, barcodeScanner, uri)
            isPickingImage = false
            if (text.isNullOrBlank()) {
                notice = "图片中未识别到二维码或条码"
            } else {
                acceptScan(text, ScanSource.GALLERY, "已从相册识别")
            }
        }
    }

    BackHandler {
        when {
            scanResult != null -> {
                isCameraResultAccepted.set(false)
                scanResult = null
                notice = null
            }
            isShowingHistory -> {
                isShowingHistory = false
            }
            else -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 2000) {
                    onFinish()
                } else {
                    lastBackPressTime = currentTime
                    Toast.makeText(context, "再按一次退出应用", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    DisposableEffect(barcodeScanner) {
        onDispose { barcodeScanner.close() }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    MaterialTheme(colorScheme = scannerColorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            if (isShowingHistory) {
                HistoryScreen(
                    items = history,
                    onBack = { isShowingHistory = false },
                    onCopy = context::copyToClipboard,
                    onOpenBrowser = context::openInBrowser,
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (hasCameraPermission) {
                        CameraScannerPreview(
                            scanner = barcodeScanner,
                            isActive = scanResult == null,
                            torchEnabled = torchEnabled,
                            onTorchAvailabilityChanged = { hasFlash = it },
                            onTorchChanged = { torchEnabled = it },
                            onBarcodeFound = { text ->
                                if (scanResult == null) {
                                    acceptScan(text, ScanSource.CAMERA, "扫码成功")
                                }
                            },
                            onCameraError = { notice = it },
                        )
                    } else {
                        CameraPermissionBackdrop()
                    }

                    ScannerOverlay()

                    TopBar(
                        onFinish = onFinish,
                        onShowHistory = {
                            torchEnabled = false
                            isShowingHistory = true
                        },
                        onPickImage = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(WindowInsets.statusBars.asPaddingValues())
                            .padding(top = 72.dp)
                            .padding(WindowInsets.navigationBars.asPaddingValues())
                            .padding(bottom = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ScanHeader()

                        if (notice != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            NoticeText(text = notice.orEmpty())
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        ScanFrame()

                        Spacer(modifier = Modifier.height(28.dp))

                        TorchControl(
                            enabled = torchEnabled,
                            available = hasFlash && hasCameraPermission && scanResult == null,
                            onToggle = {
                                if (hasFlash && hasCameraPermission && scanResult == null) {
                                    torchEnabled = !torchEnabled
                                } else {
                                    notice = "当前设备或状态不支持打开手电筒"
                                }
                            },
                        )

                        Spacer(modifier = Modifier.weight(0.45f))
                    }

                    if (isPickingImage) {
                        LoadingHint()
                    }

                    scanResult?.let { result ->
                        ResultPanel(
                            result = result,
                            onCopy = { context.copyToClipboard(result) },
                            onOpenBrowser = { context.openInBrowser(result) },
                            onScanAgain = {
                                isCameraResultAccepted.set(false)
                                scanResult = null
                                notice = null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberBarcodeScanner(): BarcodeScanner {
    return remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build()
        BarcodeScanning.getClient(options)
    }
}

@Composable
private fun CameraScannerPreview(
    scanner: BarcodeScanner,
    isActive: Boolean,
    torchEnabled: Boolean,
    onTorchAvailabilityChanged: (Boolean) -> Unit,
    onTorchChanged: (Boolean) -> Unit,
    onBarcodeFound: (String) -> Unit,
    onCameraError: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val processing = remember { AtomicBoolean(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(camera, torchEnabled) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    DisposableEffect(previewView, lifecycleOwner, scanner, isActive) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var observer: Observer<Int>? = null
        var observedCamera: Camera? = null
        val disposed = AtomicBoolean(false)

        val listener = Runnable {
            try {
                provider = cameraProviderFuture.get()
                provider?.unbindAll()

                if (disposed.get() || !isActive) {
                    camera = null
                    return@Runnable
                }

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(analysisExecutor) { imageProxy ->
                            analyzeBarcode(
                                scanner = scanner,
                                imageProxy = imageProxy,
                                processing = processing,
                                onBarcodeFound = onBarcodeFound,
                            )
                        }
                    }

                val boundCamera = provider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
                camera = boundCamera
                observedCamera = boundCamera
                val hasFlash = boundCamera?.cameraInfo?.hasFlashUnit() == true
                onTorchAvailabilityChanged(hasFlash)
                observer = Observer { state -> onTorchChanged(state == TorchState.ON) }
                observer?.let { boundCamera?.cameraInfo?.torchState?.observe(lifecycleOwner, it) }
            } catch (_: Exception) {
                camera = null
                onTorchAvailabilityChanged(false)
                onCameraError("相机启动失败，请检查权限或稍后重试")
            }
        }

        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed.set(true)
            observer?.let { observedCamera?.cameraInfo?.torchState?.removeObserver(it) }
            provider?.unbindAll()
            processing.set(false)
            camera = null
        }
    }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class])
private fun analyzeBarcode(
    scanner: BarcodeScanner,
    imageProxy: ImageProxy,
    processing: AtomicBoolean,
    onBarcodeFound: (String) -> Unit,
) {
    if (!processing.compareAndSet(false, true)) {
        imageProxy.close()
        return
    }

    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        processing.set(false)
        imageProxy.close()
        return
    }

    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            barcodes.firstReadableText()?.let(onBarcodeFound)
        }
        .addOnCompleteListener {
            processing.set(false)
            imageProxy.close()
        }
}

private suspend fun scanImageFromGallery(
    context: Context,
    scanner: BarcodeScanner,
    uri: Uri,
): String? {
    return try {
        val image = InputImage.fromFilePath(context, uri)
        scanner.process(image).await().firstReadableText()
    } catch (_: Exception) {
        null
    }
}

private fun List<Barcode>.firstReadableText(): String? {
    return firstNotNullOfOrNull { barcode ->
        barcode.rawValue?.trim()?.takeIf { it.isNotEmpty() }
    }
}

@Composable
private fun CameraPermissionBackdrop() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF32343A), Color(0xFF191A1F), Color(0xFF07080B)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "▣",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "等待开启相机权限",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "也可以点击右上角从相册识别",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun ScannerOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f)),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.00f to Color.Black.copy(alpha = 0.42f),
                    0.28f to Color.White.copy(alpha = 0.05f),
                    0.53f to Color.Transparent,
                    0.80f to Color.Black.copy(alpha = 0.24f),
                    1.00f to Color.Black.copy(alpha = 0.48f),
                ),
            ),
    )
}

@Composable
private fun TopBar(
    onFinish: () -> Unit,
    onShowHistory: () -> Unit,
    onPickImage: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Hand-drawn style back button
        IconButton(
            onClick = onFinish,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .border(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp),
                ),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_back_hand_drawn),
                contentDescription = "返回",
                modifier = Modifier.size(24.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Hand-drawn history button
            IconButton(
                onClick = onShowHistory,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFF4CAF50).copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                    ),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_history_hand_drawn),
                    contentDescription = "历史",
                    modifier = Modifier.size(24.dp),
                )
            }
            // Hand-drawn gallery button
            IconButton(
                onClick = onPickImage,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFFFD93D).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                    ),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_gallery_hand_drawn),
                    contentDescription = "相册",
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun ScanHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "扫描二维码/条码",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "对准二维码/条码，即可自动扫描",
            color = Color.White.copy(alpha = 0.66f),
            fontSize = 18.sp,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
private fun ScanFrame() {
    val transition = rememberInfiniteTransition(label = "scan-line")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan-line-progress",
    )
    val frameHeight = 260.dp
    val scanTravel = frameHeight - 74.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp)
            .height(frameHeight),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 4.5.dp.toPx()
            val corner = 38.dp.toPx()
            val width = size.width
            val height = size.height
            val terracotta = Color(0xFFC4612F)
            val green = Color(0xFF4CAF50)

            // Hand-drawn background with slight irregularity
            drawRoundRect(
                color = Color.White.copy(alpha = 0.06f),
                size = Size(width, height),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            
            // Hand-drawn corners with terracotta color
            // Top-left
            drawLine(terracotta, Offset(0f, 0f), Offset(corner + 2.dp.toPx(), 0f), stroke, StrokeCap.Round)
            drawLine(terracotta, Offset(0f, 0f), Offset(0f, corner + 2.dp.toPx()), stroke, StrokeCap.Round)
            // Top-right
            drawLine(terracotta, Offset(width, 0f), Offset(width - corner - 2.dp.toPx(), 0f), stroke, StrokeCap.Round)
            drawLine(terracotta, Offset(width, 0f), Offset(width, corner + 2.dp.toPx()), stroke, StrokeCap.Round)
            // Bottom-left
            drawLine(terracotta, Offset(0f, height), Offset(corner + 2.dp.toPx(), height), stroke, StrokeCap.Round)
            drawLine(terracotta, Offset(0f, height), Offset(0f, height - corner - 2.dp.toPx()), stroke, StrokeCap.Round)
            // Bottom-right
            drawLine(terracotta, Offset(width, height), Offset(width - corner - 2.dp.toPx(), height), stroke, StrokeCap.Round)
            drawLine(terracotta, Offset(width, height), Offset(width, height - corner - 2.dp.toPx()), stroke, StrokeCap.Round)

            // Green scanning line
            val y = 16.dp.toPx() + progress * (height - 32.dp.toPx())
            drawLine(
                color = green,
                start = Offset(10.dp.toPx(), y),
                end = Offset(width - 10.dp.toPx(), y),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .offset(y = 16.dp + scanTravel * progress)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4CAF50).copy(alpha = 0.22f),
                            Color(0xFF4CAF50).copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun NoticeText(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.84f),
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
    )
}

@Composable
private fun TorchControl(
    enabled: Boolean,
    available: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onToggle,
            enabled = available,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) 
                        Color(0xFFFFD93D).copy(alpha = 0.25f) 
                    else 
                        Color.White.copy(alpha = 0.15f)
                )
                .border(
                    width = 2.5.dp,
                    color = if (enabled) 
                        Color(0xFFFFD93D).copy(alpha = 0.6f) 
                    else 
                        Color.White.copy(alpha = 0.25f),
                    shape = CircleShape,
                ),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_flashlight_hand_drawn),
                contentDescription = if (enabled) "关闭手电筒" else "打开手电筒",
                modifier = Modifier.size(36.dp),
                alpha = if (available) 1f else 0.42f,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (enabled) "轻触关闭" else "轻触照亮",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun LoadingHint() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "正在识别...",
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 28.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun BoxScope.ResultPanel(
    result: String,
    onCopy: () -> Unit,
    onOpenBrowser: () -> Unit,
    onScanAgain: () -> Unit,
) {
    val canOpen = result.normalizedWebUrl() != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onScanAgain,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 18.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F4EF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
        Column(modifier = Modifier.padding(22.dp)) {
            // Hand-drawn style title with accent
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "识别结果",
                    color = Color(0xFF1F2421),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFF2E3D6))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "✓",
                        color = Color(0xFFC4612F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = result,
                color = Color(0xFF1F2421),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE7E1D7), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC4612F),
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                ) {
                    Text(
                        text = "复制",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
                Button(
                    onClick = onOpenBrowser,
                    enabled = canOpen,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE7E1D7),
                        disabledContentColor = Color(0xFF5C635D),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                ) {
                    Text(
                        text = "访问",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (canOpen) Color.White else Color(0xFF5C635D),
                    )
                }
            }
            TextButton(
                onClick = onScanAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text(
                    text = "继续扫描",
                    color = Color(0xFF1F2421),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
    }
}

private val scannerColorScheme = darkColorScheme(
    primary = Color(0xFF7AAEFF),
    secondary = Color(0xFFB8C7FF),
    background = Color.Black,
    surface = Color.Black,
)

private fun Context.hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}

private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("扫码结果", text))
    Toast.makeText(this, "内容已复制", Toast.LENGTH_SHORT).show()
}

private fun Context.openInBrowser(text: String) {
    val url = text.normalizedWebUrl()
    if (url == null) {
        Toast.makeText(this, "识别内容不是可访问链接", Toast.LENGTH_SHORT).show()
        return
    }
    val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    val chooser = Intent.createChooser(viewIntent, "选择应用打开").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        startActivity(chooser)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "没有可打开该链接的应用", Toast.LENGTH_SHORT).show()
    }
}

internal fun String.normalizedWebUrl(): String? {
    val value = trim()
    if (value.isBlank()) return null

    val parsed = Uri.parse(value)
    if (parsed.scheme.equals("http", ignoreCase = true) || parsed.scheme.equals("https", ignoreCase = true)) {
        return value
    }

    return if (Patterns.WEB_URL.matcher(value).matches()) {
        "https://$value"
    } else {
        null
    }
}
