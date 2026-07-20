package com.isyarat.pintar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.isyarat.pintar.ui.theme.IsyaratPintarTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Izin kamera dibutuhkan untuk fitur AR", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            var isDarkTheme by remember { mutableStateOf(false) } // Default Light for kids
            var showSplash by remember { mutableStateOf(true) }

            IsyaratPintarTheme(darkTheme = isDarkTheme) {
                if (showSplash) {
                    SplashScreen(onTimeout = { showSplash = false })
                } else {
                    MainApp(onToggleTheme = { isDarkTheme = !isDarkTheme }, isDarkTheme = isDarkTheme)
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(2000)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(1000))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.icon_app),
                    contentDescription = "Logo",
                    modifier = Modifier.size(150.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Isyarat Pintar",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(onToggleTheme: () -> Unit, isDarkTheme: Boolean) {
    var currentScreen by remember { mutableStateOf("beranda") }
    var selectedLevel by remember { mutableStateOf<Level?>(null) }
    val levelsState = remember { mutableStateListOf<Level>().apply { addAll(IsyaratData.levels) } }
    val historyState = remember { mutableStateListOf<HistoryRecord>() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == "beranda",
                    onClick = { currentScreen = "beranda" },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Beranda") }
                )
                NavigationBarItem(
                    selected = currentScreen == "riwayat",
                    onClick = { currentScreen = "riwayat" },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Riwayat") }
                )
                NavigationBarItem(
                    selected = currentScreen == "pengaturan",
                    onClick = { currentScreen = "pengaturan" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Pengaturan") }
                )
                NavigationBarItem(
                    selected = currentScreen == "tentang",
                    onClick = { currentScreen = "tentang" },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("Tentang") }
                )
            }
        },
        floatingActionButton = {
            if (currentScreen == "beranda") {
                ExtendedFloatingActionButton(
                    onClick = { currentScreen = "scanner" },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "AR Scanner") },
                    text = { Text("Scan QR") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                "beranda" -> BerandaScreen(levelsState) { level ->
                    selectedLevel = level
                    currentScreen = "kuis"
                }
                "kuis" -> selectedLevel?.let { level ->
                    QuizScreen(
                        level = level,
                        onFinished = { score ->
                            val index = levelsState.indexOfFirst { it.id == level.id }
                            if (index != -1) {
                                levelsState[index] = levelsState[index].copy(isCompleted = true, score = score)
                                historyState.add(HistoryRecord(levelId = level.id, score = score))
                                // Hanya buka level selanjutnya jika skor sempurna (100)
                                if (index + 1 < levelsState.size && score >= 100) {
                                    levelsState[index + 1] = levelsState[index + 1].copy(isUnlocked = true)
                                }
                            }
                            currentScreen = "beranda"
                        },
                        onBack = { currentScreen = "beranda" }
                    )
                }
                "scanner" -> ScannerScreen { currentScreen = "beranda" }
                "riwayat" -> RiwayatScreen(
                    history = historyState,
                    levels = levelsState,
                    onResetAll = {
                        historyState.clear()
                        val reset = levelsState.mapIndexed { i, level ->
                            level.copy(isUnlocked = i == 0, isCompleted = false, score = 0)
                        }
                        levelsState.clear()
                        levelsState.addAll(reset)
                    },
                    onDeleteLevel = { levelId ->
                        val index = levelsState.indexOfFirst { it.id == levelId }
                        if (index != -1) {
                            // Reset level terpilih dan kunci level setelahnya secara berurutan
                            for (i in index until levelsState.size) {
                                levelsState[i] = levelsState[i].copy(
                                    isUnlocked = i == index, // Level yang dihapus tetap terbuka untuk dicoba lagi
                                    isCompleted = false,
                                    score = 0
                                )
                            }
                            // Hapus riwayat untuk level ini dan semua level setelahnya
                            historyState.removeAll { it.levelId >= levelId }
                        }
                    }
                )
                "pengaturan" -> PengaturanScreen(isDarkTheme, onToggleTheme)
                "tentang" -> TentangKamiScreen()
            }
        }
    }
}

@Composable
fun BerandaScreen(levels: List<Level>, onLevelClick: (Level) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Halo, Teman Kecil! \uD83D\uDC4B", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Daftar Level", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            items(levels) { level ->
                LevelItem(level, onLevelClick)
            }
        }
    }
}

@Composable
fun LevelItem(level: Level, onClick: (Level) -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = level.isUnlocked) { onClick(level) },
        colors = CardDefaults.cardColors(
            containerColor = if (level.isUnlocked) MaterialTheme.colorScheme.surfaceVariant else Color.Gray.copy(alpha = 0.3f)
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Gambar Ikon Level berdasarkan soal pertama
            val firstQuestionImage = level.questions.firstOrNull()?.correctAnswerImage
            val resId = if (firstQuestionImage != null) {
                context.resources.getIdentifier(firstQuestionImage, "drawable", context.packageName)
            } else 0

            if (resId != 0 && level.isUnlocked) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            if (level.isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color.Gray.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        level.id.toString(),
                        fontWeight = FontWeight.Bold,
                        color = if (level.isUnlocked) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(level.name, fontWeight = FontWeight.Bold)
                if (level.isCompleted) Text("Selesai - Skor: ${level.score}", fontSize = 12.sp, color = Color(0xFF4CAF50))
                else if (!level.isUnlocked) Text("Terkunci", fontSize = 12.sp, color = Color.Gray)
                else Text("Siap Dimulai!", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Icon(
                imageVector = if (level.isUnlocked) Icons.Default.PlayArrow else Icons.Default.Lock,
                contentDescription = null,
                tint = if (level.isUnlocked) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }
    }
}

@Composable
fun QuizScreen(level: Level, onFinished: (Int) -> Unit, onBack: () -> Unit) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var showFinalDialog by remember { mutableStateOf(false) }
    var shakingIndex by remember { mutableStateOf<Int?>(null) }
    var isShakingCorrect by remember { mutableStateOf(true) }
    var hasMistakeInCurrentLevel by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentQuestion = level.questions[currentQuestionIndex]

    val shakeAnim = remember { Animatable(0f) }

    LaunchedEffect(shakingIndex) {
        if (shakingIndex != null && !isShakingCorrect) {
            val intensity = 1.5f // Getar sangat rendah
            val duration = 80
            repeat(3) {
                shakeAnim.animateTo(intensity, tween(duration, easing = LinearEasing))
                shakeAnim.animateTo(-intensity, tween(duration, easing = LinearEasing))
            }
            shakeAnim.animateTo(0f, tween(duration, easing = LinearEasing))
        } else {
            shakeAnim.snapTo(0f)
        }
    }

    if (showFinalDialog) {
        val isPerfect = score >= 100
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    if (isPerfect) "Luar Biasa!" else "Coba Lagi",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPerfect) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val imageName = if (isPerfect) "sign_luarbiasa" else "sign_cobalagi"
                    val resId = context.resources.getIdentifier(imageName, "drawable", context.packageName)
                    if (resId != 0) {
                        val infiniteTransition = rememberInfiniteTransition()
                        
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.98f,
                            targetValue = 1.02f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        
                        val emojiOffset by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = -8f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 16.dp)) {
                            if (isPerfect) {
                                // Efek Party Popper dengan Emoji yang melompat
                                Text("🎉", fontSize = 48.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = (-30).dp, y = (emojiOffset - 20).dp))
                                Text("🎊", fontSize = 48.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = 30.dp, y = (emojiOffset - 20).dp))
                                Text("✨", fontSize = 24.sp, modifier = Modifier.align(Alignment.BottomStart).offset(x = (-40).dp, y = (-emojiOffset).dp))
                                Text("⭐", fontSize = 24.sp, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 40.dp, y = (-emojiOffset).dp))
                            } else {
                                // Efek Sedih dengan Emoji yang melompat
                                Text("😢", fontSize = 48.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = (-30).dp, y = (emojiOffset - 20).dp))
                                Text("☹️", fontSize = 48.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = 30.dp, y = (emojiOffset - 20).dp))
                                Text("💔", fontSize = 24.sp, modifier = Modifier.align(Alignment.BottomStart).offset(x = (-40).dp, y = (-emojiOffset).dp))
                                Text("☁️", fontSize = 24.sp, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 40.dp, y = (-emojiOffset).dp))
                            }
                            
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(150.dp)
                                    .scale(scale)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Kamu telah menyelesaikan level ini!", textAlign = TextAlign.Center)
                    Text("Nilai Kamu: $score", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (!isPerfect) {
                        Text(
                            "Dapatkan nilai 100 untuk membuka level berikutnya!",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = { Button(onClick = { onFinished(score) }) { Text("Ke Beranda") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
            Text("Pertanyaan ${currentQuestionIndex + 1}/${level.questions.size}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        LinearProgressIndicator(
            progress = (currentQuestionIndex + 1).toFloat() / level.questions.size,
            modifier = Modifier.fillMaxWidth().height(12.dp).padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                currentQuestion.text,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
        }
        
        Text("Pilih Gambar Isyarat yang Benar:", fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp))
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f)) {
            items(currentQuestion.options.size) { index ->
                val option = currentQuestion.options[index]
                val resId = context.resources.getIdentifier(option, "drawable", context.packageName)
                
                val translationX = if (shakingIndex == index) shakeAnim.value else 0f

                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .aspectRatio(1f)
                        .offset(x = translationX.dp)
                        .clickable {
                            if (shakingIndex != null) return@clickable
                            
                            val isCorrect = option == currentQuestion.correctAnswerImage
                            shakingIndex = index
                            isShakingCorrect = isCorrect
                            
                            if (!isCorrect) {
                                hasMistakeInCurrentLevel = true
                                score = (score - 5).coerceAtLeast(0) // Nilai turun jika salah
                            } else {
                                score += (100 / level.questions.size)
                            }
                            
                            scope.launch {
                                // Jika benar, lanjut lebih cepat (tanpa getar)
                                // Jika salah, tunggu animasi getar selesai (800ms)
                                delay(if (isCorrect) 150 else 800)
                                shakingIndex = null
                                
                                if (currentQuestionIndex < level.questions.size - 1) {
                                    currentQuestionIndex++
                                } else {
                                    // Pastikan skor 100 hanya jika benar semua tanpa salah
                                    if (!hasMistakeInCurrentLevel && isCorrect) {
                                        score = 100
                                    } else {
                                        score = score.coerceAtMost(95)
                                    }
                                    showFinalDialog = true
                                }
                            }
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            shakingIndex == index && isShakingCorrect -> Color(0xFFC8E6C9) // Green tint
                            shakingIndex == index && !isShakingCorrect -> Color(0xFFFFCDD2) // Red tint
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    if (resId != 0) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var detectedText by remember { mutableStateOf("") }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    val infiniteTransition = rememberInfiniteTransition()
    val translateY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )

    // Perbaikan: Pastikan kamera dilepas saat meninggalkan layar
    DisposableEffect(Unit) {
        onDispose {
            cameraProviderFuture.get().unbindAll()
            recognizer.close()
            executor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                // Lepaskan binding lama sebelum membuat yang baru
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                recognizer.process(image)
                                    .addOnSuccessListener { visionText ->
                                        val greetings = listOf(
                                            "apa kabar", "assalamualaikum", "halo", "sampai jumpa", 
                                            "selamat pagi", "selamat siang", "selamat sore", "selamat malam",
                                            "waalaikumsalam", "senang berkenalan"
                                        )
                                        val commonWords = listOf(
                                            "belajar", "memasak", "makan", "minum", "sayang", "bekerja", 
                                            "ayah", "ibu", "saya", "satu", "sayur", "dapur", "sarapan", 
                                            "pagi tadi", "pagi", "sedang", "ayam", "goreng", "kakak", "kamar", "di", "tadi"
                                        )
                                        
                                        val fullText = visionText.text.lowercase()
                                        
                                        for (keyword in greetings) {
                                            if ("\\b$keyword\\b".toRegex().containsMatchIn(fullText)) {
                                                detectedText = "greeting_" + keyword.replace(" ", "_")
                                                return@addOnSuccessListener
                                            }
                                        }

                                        for (keyword in commonWords) {
                                            if ("\\b$keyword\\b".toRegex().containsMatchIn(fullText)) {
                                                detectedText = keyword.replace(" ", "_")
                                                return@addOnSuccessListener
                                            }
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                try {
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }, modifier = Modifier.fillMaxSize())

        // UI Overlay
        Box(
            modifier = Modifier
                .size(width = 280.dp, height = 180.dp)
                .align(Alignment.Center)
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Text(
                "Arahkan ke kata",
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                fontSize = 12.sp
            )
        }

        if (detectedText.isNotEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center).offset(y = (-40).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val displayText = detectedText.replace("greeting_", "").replace("_", " ")
                    Text(
                        text = "Terdeteksi: ${displayText.uppercase()}",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (detectedText.isNotEmpty()) {
                    val resId = if (detectedText.startsWith("greeting_")) {
                        val name = "bahasa_isyarat_" + detectedText.replace("greeting_", "")
                            .replace("sampai_jumpa", "sampai_jumpa_lagi")
                            .replace("senang_berkenalan", "senang_berkenalan_denganmu")
                        context.resources.getIdentifier(name, "raw", context.packageName)
                    } else {
                        context.resources.getIdentifier("sign_$detectedText", "drawable", context.packageName)
                    }
                    if (resId != 0) {
                        val imageLoader = ImageLoader.Builder(context)
                            .components {
                                if (android.os.Build.VERSION.SDK_INT >= 28) {
                                    add(ImageDecoderDecoder.Factory())
                                } else {
                                    add(GifDecoder.Factory())
                                }
                            }
                            .build()

                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context).data(data = resId).apply(block = fun ImageRequest.Builder.() {
                                    crossfade(true)
                                }).build(),
                                imageLoader = imageLoader
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .offset(y = translateY.dp)
                                .size(200.dp) // Ukuran sedikit lebih besar untuk video/gif
                        )
                    }
                }

                // Tambahkan tombol untuk reset deteksi
                Button(
                    onClick = { detectedText = "" },
                    modifier = Modifier.padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Scan Lagi")
                }
            }
        }
        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).align(Alignment.TopStart)) { 
            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) 
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(
    history: List<HistoryRecord>,
    levels: List<Level>,
    onResetAll: () -> Unit,
    onDeleteLevel: (Int) -> Unit
) {
    var showConfirmAll1 by remember { mutableStateOf(false) }
    var showConfirmAll2 by remember { mutableStateOf(false) }
    var levelToDelete by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Riwayat Nilai", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showConfirmAll1 = true }) { 
                Icon(Icons.Default.DeleteForever, null, tint = Color.Red) 
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Grafik Kemajuan Anak (Barchart)
        Card(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Grafik Kemajuan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                if (history.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada data nilai", color = Color.Gray)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                        val recentHistory = history.takeLast(5)
                        val primaryColor = MaterialTheme.colorScheme.primary
                        
                        Canvas(modifier = Modifier.fillMaxSize().padding(start = 65.dp, end = 25.dp, bottom = 20.dp, top = 10.dp)) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            
                            if (recentHistory.isNotEmpty()) {
                                val points = recentHistory.mapIndexed { index, record ->
                                    val x = (record.score / 100f) * canvasWidth
                                    val y = if (recentHistory.size > 1) {
                                        canvasHeight - (index.toFloat() / (recentHistory.size - 1)) * canvasHeight
                                    } else {
                                        canvasHeight / 2
                                    }
                                    Offset(x, y)
                                }

                                // Draw horizontal grid lines for each level
                                recentHistory.forEachIndexed { index, _ ->
                                    val y = if (recentHistory.size > 1) {
                                        canvasHeight - (index.toFloat() / (recentHistory.size - 1)) * canvasHeight
                                    } else {
                                        canvasHeight / 2
                                    }
                                    drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(0f, y), Offset(canvasWidth, y), strokeWidth = 1f)
                                }

                                // Draw vertical grid lines for scores
                                for (i in 0..4) {
                                    val xGrid = (i / 4f) * canvasWidth
                                    drawLine(Color.LightGray.copy(alpha = 0.5f), Offset(xGrid, 0f), Offset(xGrid, canvasHeight), strokeWidth = 1f)
                                }

                                // Draw the line
                                if (points.size > 1) {
                                    for (i in 0 until points.size - 1) {
                                        drawLine(
                                            color = primaryColor,
                                            start = points[i],
                                            end = points[i + 1],
                                            strokeWidth = 3.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }

                                // Draw points
                                points.forEach { point ->
                                    drawCircle(primaryColor, radius = 5.dp.toPx(), center = point)
                                    drawCircle(Color.White, radius = 2.dp.toPx(), center = point)
                                }
                            }
                        }
                        
                        // Labels Y (Level Name)
                        Column(
                            modifier = Modifier.fillMaxHeight().padding(bottom = 20.dp, top = 10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            recentHistory.reversed().forEach { record ->
                                val levelName = levels.find { it.id == record.levelId }?.name?.split(":")?.firstOrNull() ?: "Lvl ${record.levelId}"
                                Text(
                                    text = levelName,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(60.dp),
                                    maxLines = 1,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                        
                        // Labels X (Score values)
                        Row(
                            modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(start = 65.dp, end = 25.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0", fontSize = 10.sp, color = Color.Gray)
                            Text("50", fontSize = 10.sp, color = Color.Gray)
                            Text("100", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(history.reversed()) { record ->
                val name = levels.find { it.id == record.levelId }?.name ?: "Level ${record.levelId}"
                ListItem(
                    headlineText = { Text(name) },
                    supportingText = { Text("Skor: ${record.score}") },
                    leadingContent = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700)) },
                    trailingContent = {
                        IconButton(onClick = { levelToDelete = record.levelId }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Level", tint = Color.Gray)
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
    
    // Dialog Konfirmasi Hapus Spesifik Level
    if (levelToDelete != null) {
        AlertDialog(
            onDismissRequest = { levelToDelete = null },
            title = { Text("Reset Level ${levelToDelete}?") },
            text = { Text("Menghapus riwayat level ini akan mengunci level-level setelahnya. Lanjutkan?") },
            confirmButton = {
                TextButton(onClick = { 
                    onDeleteLevel(levelToDelete!!)
                    levelToDelete = null 
                }) { Text("Ya, Reset", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { levelToDelete = null }) { Text("Batal") }
            }
        )
    }
    
    if (showConfirmAll1) {
        AlertDialog(
            onDismissRequest = { showConfirmAll1 = false },
            title = { Text("Hapus Semua Riwayat?") },
            text = { Text("Apakah kamu yakin ingin menghapus semua riwayat nilai?") },
            confirmButton = {
                TextButton(onClick = { 
                    showConfirmAll1 = false
                    showConfirmAll2 = true 
                }) { Text("Ya") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmAll1 = false }) { Text("Batal") }
            }
        )
    }
    
    if (showConfirmAll2) {
        AlertDialog(
            onDismissRequest = { showConfirmAll2 = false },
            title = { Text("Hapus Permanen?") },
            text = { Text("Data yang dihapus tidak bisa dikembalikan!") },
            confirmButton = {
                TextButton(onClick = { 
                    onResetAll()
                    showConfirmAll2 = false 
                }) { Text("HAPUS SEMUA", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmAll2 = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun PengaturanScreen(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pengaturan", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Tampilan", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mode Gelap")
                    }
                    Switch(checked = isDarkTheme, onCheckedChange = { onToggleTheme() })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Data & Aplikasi", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        try {
                            context.cacheDir.deleteRecursively()
                            Toast.makeText(context, "Cache berhasil dihapus!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal menghapus cache", Toast.LENGTH_SHORT).show()
                        }
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bersihkan Cache", color = Color.Red)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        Toast.makeText(context, "Versi 1.0.0 (Android 16 Optimized)", Toast.LENGTH_SHORT).show()
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Versi Aplikasi")
                    }
                    Text("1.0.0", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun TentangKamiScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.icon_app),
            contentDescription = null,
            modifier = Modifier.size(120.dp).padding(16.dp)
        )
        Text(
            "Isyarat Pintar",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Versi 1.0.0",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Isyarat Pintar adalah platform edukasi interaktif yang dirancang khusus untuk membantu teman-teman tunarungu belajar bahasa isyarat dengan cara yang menyenangkan melalui teknologi AR (Augmented Reality).",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "Hubungi Pengembang",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        ContactItem(
            icon = Icons.Default.Phone,
            label = "WhatsApp",
            value = "091394784696",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/6291394784696"))
                context.startActivity(intent)
            }
        )
        
        ContactItem(
            icon = Icons.Default.AccountCircle,
            label = "Instagram",
            value = "@mur.fidznx",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/mur.fidznx"))
                context.startActivity(intent)
            }
        )
        
        ContactItem(
            icon = Icons.Default.Code,
            label = "GitHub",
            value = "mur.fidznx",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/murfidznx"))
                context.startActivity(intent)
            }
        )
        
        ContactItem(
            icon = Icons.Default.Email,
            label = "Email",
            value = "mur.fidznx@gmail.com",
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:mur.fidznx@gmail.com"))
                context.startActivity(intent)
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("© 2024 Isyarat Pintar Team", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ContactItem(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, fontSize = 12.sp, color = Color.Gray)
                Text(value, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
