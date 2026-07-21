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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.isyarat.pintar.ui.theme.*
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
            var showSplash by remember { mutableStateOf(true) }

            IsyaratPintarTheme {
                if (showSplash) {
                    SplashScreen(onTimeout = { showSplash = false })
                } else {
                    MainApp()
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
            .background(HH_Background),
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
                    modifier = Modifier.size(180.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Isyarat Pintar",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HH_Headline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE) }
    
    // Inisialisasi tema saat pertama kali dijalankan
    LaunchedEffect(Unit) {
        val themeIndex = sharedPreferences.getInt("theme_index", 0)
        updateThemeColors(
            ThemeColors.getAll().getOrElse(themeIndex) { ThemeColors.Palette3 }
        )
    }

    var currentScreen by remember { mutableStateOf("beranda") }
    var selectedLevel by remember { mutableStateOf<Level?>(null) }
    val levelsState = remember { mutableStateListOf<Level>().apply { addAll(IsyaratData.levels) } }
    val historyState = remember { mutableStateListOf<HistoryRecord>() }

    var restartKey by remember { mutableStateOf(0) }

    key(restartKey) {
        Scaffold(
            containerColor = HH_Background,
            topBar = {
                if (currentScreen != "scanner") {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.icon_app),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "ISYARAT PINTAR",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = HH_Headline,
                                    letterSpacing = 2.sp
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = HH_Background
                        ),
                        navigationIcon = {
                            if (currentScreen != "beranda") {
                                IconButton(onClick = { 
                                    if (currentScreen == "tentang") currentScreen = "pengaturan"
                                    else currentScreen = "beranda" 
                                }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = HH_Headline)
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (currentScreen != "scanner" && currentScreen != "kuis") {
                    val navBarColor = if (HH_Background.luminance() < 0.5f) HH_Background else HH_Headline
                    NavigationBar(
                        containerColor = navBarColor,
                        modifier = Modifier.border(3.dp, HH_Stroke, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == "beranda",
                            onClick = { currentScreen = "beranda" },
                            label = { Text("Beranda", fontWeight = FontWeight.Bold, color = Color.White) },
                            icon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color.White) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White,
                                indicatorColor = HH_Secondary
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == "riwayat",
                            onClick = { currentScreen = "riwayat" },
                            label = { Text("Riwayat", fontWeight = FontWeight.Bold, color = Color.White) },
                            icon = { Icon(Icons.Default.History, contentDescription = null, tint = Color.White) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White,
                                indicatorColor = HH_Secondary
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == "pengaturan",
                            onClick = { currentScreen = "pengaturan" },
                            label = { Text("Pengaturan", fontWeight = FontWeight.Bold, color = Color.White) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White,
                                indicatorColor = HH_Secondary
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                if (currentScreen == "beranda") {
                    ExtendedFloatingActionButton(
                        onClick = { currentScreen = "scanner" },
                        containerColor = HH_Button,
                        contentColor = HH_ButtonText,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.border(3.dp, HH_Stroke, RoundedCornerShape(16.dp)),
                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = "AR Scanner") },
                        text = { Text("Scan Text", fontWeight = FontWeight.ExtraBold) }
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
                                    // Buka level selanjutnya jika skor lebih dari 0
                                    if (index + 1 < levelsState.size && score > 0) {
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
                        onDeleteRecord = { record ->
                            historyState.remove(record)
                            val levelIndex = levelsState.indexOfFirst { it.id == record.levelId }
                            if (levelIndex != -1) {
                                val lastRecord = historyState.filter { it.levelId == record.levelId }.lastOrNull()
                                val newScore = lastRecord?.score ?: 0
                                
                                levelsState[levelIndex] = levelsState[levelIndex].copy(
                                    score = newScore,
                                    isCompleted = lastRecord != null
                                )
                                
                                // Re-evaluasi status kunci level selanjutnya
                                if (levelIndex + 1 < levelsState.size) {
                                    if (newScore == 0) {
                                        levelsState[levelIndex + 1] = levelsState[levelIndex + 1].copy(isUnlocked = false)
                                    } else {
                                        levelsState[levelIndex + 1] = levelsState[levelIndex + 1].copy(isUnlocked = true)
                                    }
                                }
                            }
                        }
                    )
                    "pengaturan" -> PengaturanScreen({ currentScreen = "tentang" }) {
                        restartKey++
                    }
                    "tentang" -> TentangKamiScreen()
                }
            }
        }
    }
}

@Composable
fun BerandaScreen(levels: List<Level>, onLevelClick: (Level) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Halo Sobat Isyarat", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = HH_Headline)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Daftar Level", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HH_Headline)
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            .border(3.dp, HH_Stroke, RoundedCornerShape(20.dp))
            .clickable(enabled = level.isUnlocked) { onClick(level) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (level.isUnlocked) HH_Secondary else HH_Background.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        .size(60.dp)
                        .background(HH_Background, RoundedCornerShape(12.dp))
                        .border(2.dp, HH_Stroke, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            if (level.isUnlocked) HH_Button.copy(alpha = 0.2f)
                            else HH_Stroke.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .border(2.dp, HH_Stroke, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (level.isUnlocked) Icons.Default.PlayArrow else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (level.isUnlocked) HH_Headline else HH_Stroke.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(level.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = HH_Headline)
                if (level.isCompleted) Text("Skor: ${level.score}", fontSize = 14.sp, color = HH_Headline, fontWeight = FontWeight.Bold)
                else if (!level.isUnlocked) Text("Terkunci", fontSize = 14.sp, color = HH_Headline.copy(alpha = 0.6f))
                else Text("Ayo Mulai!", fontSize = 14.sp, color = HH_Button, fontWeight = FontWeight.Bold)
            }
            if (level.isCompleted) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HH_Button, modifier = Modifier.size(28.dp))
            }
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

    val shuffledOptions = remember(currentQuestionIndex, level.id) {
        currentQuestion.options.shuffled()
    }

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
            containerColor = HH_Background,
            titleContentColor = HH_Headline,
            textContentColor = HH_Headline,
            modifier = Modifier.border(4.dp, HH_Stroke, RoundedCornerShape(28.dp)),
            title = {
                    Text(
                        if (isPerfect) "Luar Biasa!" else "Coba Lagi",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPerfect) HH_Button else HH_Tertiary
                    )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val imageName = if (isPerfect) "sign_luarbiasa" else "sign_cobalagi"
                    val resId = context.resources.getIdentifier(imageName, "drawable", context.packageName)
                    if (resId != 0) {
                        val infiniteTransition = rememberInfiniteTransition()
                        
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.95f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        
                        val emojiOffset by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = -12f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 16.dp)) {
                            if (isPerfect) {
                                Text("🎉", fontSize = 56.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = (-40).dp, y = (emojiOffset - 30).dp))
                                Text("🎊", fontSize = 56.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (emojiOffset - 30).dp))
                            } else {
                                Text("😢", fontSize = 56.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = (-40).dp, y = (emojiOffset - 30).dp))
                                Text("☹️", fontSize = 56.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (emojiOffset - 30).dp))
                            }
                            
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(180.dp)
                                    .scale(scale)
                                    .border(3.dp, HH_Stroke, RoundedCornerShape(20.dp))
                                    .background(Color.White, RoundedCornerShape(20.dp))
                                    .padding(8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Kamu telah menyelesaikan level ini!", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Text("Nilai Kamu: $score", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = HH_Button)
                    if (!isPerfect) {
                        Text(
                            "Dapatkan nilai 100 untuk membuka level berikutnya!",
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HH_Headline.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = { 
                Button(
                    onClick = { onFinished(score) },
                    colors = ButtonDefaults.buttonColors(containerColor = HH_Button, contentColor = HH_ButtonText),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(2.dp, HH_Stroke, RoundedCornerShape(12.dp))
                ) { 
                    Text("Ke Beranda", fontWeight = FontWeight.ExtraBold) 
                } 
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(HH_Background).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = HH_Headline)
            }
            Text("Pertanyaan ${currentQuestionIndex + 1}/${level.questions.size}", 
                modifier = Modifier.weight(1f), 
                textAlign = TextAlign.Center, 
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = HH_Headline
            )
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        LinearProgressIndicator(
            progress = (currentQuestionIndex + 1).toFloat() / level.questions.size,
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .padding(vertical = 4.dp)
                .border(2.dp, HH_Stroke, RoundedCornerShape(8.dp)),
            color = HH_Button,
            trackColor = HH_Secondary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, HH_Stroke, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = HH_Secondary),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                currentQuestion.text,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HH_Headline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(32.dp)
            )
        }
        
        Text("Pilih Gambar yang Benar:", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HH_Headline, modifier = Modifier.padding(top = 24.dp))
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f)) {
            items(shuffledOptions.size) { index ->
                val option = shuffledOptions[index]
                val resId = context.resources.getIdentifier(option, "drawable", context.packageName)
                
                val translationX = if (shakingIndex == index) shakeAnim.value else 0f

                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .aspectRatio(1f)
                        .offset(x = translationX.dp)
                        .border(
                            width = 3.dp,
                            color = when {
                                shakingIndex == index && isShakingCorrect -> HH_Button
                                shakingIndex == index && !isShakingCorrect -> HH_Tertiary
                                else -> HH_Stroke
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            if (shakingIndex != null) return@clickable
                            
                            val isCorrect = option == currentQuestion.correctAnswerImage
                            shakingIndex = index
                            isShakingCorrect = isCorrect
                            
                            if (!isCorrect) {
                                hasMistakeInCurrentLevel = true
                                score = (score - 5).coerceAtLeast(0) 
                            } else {
                                score += (100 / level.questions.size)
                            }
                            
                            scope.launch {
                                delay(if (isCorrect) 150 else 800)
                                shakingIndex = null
                                
                                if (currentQuestionIndex < level.questions.size - 1) {
                                    currentQuestionIndex++
                                } else {
                                    if (!hasMistakeInCurrentLevel && isCorrect) {
                                        score = 100
                                    } else {
                                        score = score.coerceAtMost(95)
                                    }
                                    showFinalDialog = true
                                }
                            }
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            shakingIndex == index && isShakingCorrect -> HH_Secondary
                            shakingIndex == index && !isShakingCorrect -> HH_Tertiary.copy(alpha = 0.5f)
                            else -> HH_Background
                        }
                    )
                ) {
                    if (resId != 0) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(16.dp)
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
                                        val blocks = visionText.textBlocks
                                        
                                        val greetings = listOf(
                                            "senang bertemu dengan kamu", "sampai jumpa lagi",
                                            "assalamualaikum", "waalaikumsalam", "senang berkenalan",
                                            "sampai jumpa", "selamat pagi", "selamat siang", 
                                            "selamat sore", "selamat malam", "perkenalkan", 
                                            "salam kenal", "sama sama", "terima kasih", 
                                            "tidak suka", "tidak mau", "apa kabar", 
                                            "halo", "maaf", "mau", "nama", "suka", "tidak", "tolong", "ya"
                                        )
                                        val commonWords = listOf(
                                            "belajar", "memasak", "makan", "minum", "sayang", "bekerja", 
                                            "ayah", "ibu", "saya", "satu", "sayur", "dapur", "sarapan", 
                                            "pagi", "sedang", "ayam", "goreng", "kakak", "kamar", "di"
                                        )
                                        
                                        // Cari kata yang paling dominan/dekat tengah
                                        var bestMatch: String? = null
                                        
                                        // Urutkan kata pencarian dari yang terpanjang ke terpendek untuk akurasi lebih baik
                                        val sortedGreetings = greetings.sortedByDescending { it.length }
                                        val sortedCommonWords = commonWords.sortedByDescending { it.length }

                                        for (block in blocks) {
                                            val text = block.text.lowercase()
                                            for (keyword in sortedGreetings) {
                                                if ("\\b$keyword\\b".toRegex().containsMatchIn(text)) {
                                                    bestMatch = "greeting_" + keyword.replace(" ", "_")
                                                    break
                                                }
                                            }
                                            if (bestMatch != null) break
                                            
                                            for (keyword in sortedCommonWords) {
                                                if ("\\b$keyword\\b".toRegex().containsMatchIn(text)) {
                                                    bestMatch = keyword.replace(" ", "_")
                                                    break
                                                }
                                            }
                                            if (bestMatch != null) break
                                        }

                                        if (bestMatch != null) {
                                            detectedText = bestMatch
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
        if (detectedText.isEmpty()) {
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
                        val key = detectedText.replace("greeting_", "")
                        val name = when (key) {
                            "sampai_jumpa", "sampai_jumpa_lagi" -> "bahasa_isyarat_sampai_jumpa_lagi"
                            "senang_berkenalan", "senang_bertemu_dengan_kamu" -> "bahasa_isyarat_senang_bertemu_dengan_kamu"
                            "terima_kasih" -> "bahasa_isyarat_terimakasih"
                            else -> "bahasa_isyarat_$key"
                        }
                        context.resources.getIdentifier(name, "raw", context.packageName)
                    } else {
                        val scanResId = context.resources.getIdentifier("scan_$detectedText", "drawable", context.packageName)
                        if (scanResId != 0) scanResId
                        else context.resources.getIdentifier("sign_$detectedText", "drawable", context.packageName)
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
    onDeleteRecord: (HistoryRecord) -> Unit
) {
    var showConfirmAll1 by remember { mutableStateOf(false) }
    var showConfirmAll2 by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<HistoryRecord?>(null) }
    var selectedLevelFilter by remember { mutableStateOf<Int?>(null) }

    val levelColors = listOf(
        HH_Button,
        HH_Secondary,
        HH_Tertiary,
        HH_Headline,
        HH_Button
    )

    Column(modifier = Modifier.fillMaxSize().background(HH_Background).padding(16.dp)) {
        val displayHistory = if (selectedLevelFilter == null) history else history.filter { it.levelId == selectedLevelFilter }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Riwayat Nilai", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = HH_Headline)
            IconButton(onClick = { showConfirmAll1 = true }) { 
                Icon(Icons.Default.DeleteForever, null, tint = HH_Tertiary, modifier = Modifier.size(32.dp)) 
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Box List Filter Level
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Filter "Semua"
            Surface(
                onClick = { selectedLevelFilter = null },
                modifier = Modifier.weight(1.5f).height(48.dp).border(2.dp, HH_Stroke, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = if (selectedLevelFilter == null) HH_Button else HH_Secondary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Semua",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selectedLevelFilter == null) HH_ButtonText else HH_Headline
                    )
                }
            }

            // Filter per Level
            (1..5).forEach { levelId ->
                Surface(
                    onClick = { selectedLevelFilter = levelId },
                    modifier = Modifier.weight(1f).height(48.dp).border(2.dp, HH_Stroke, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedLevelFilter == levelId) HH_Button else HH_Secondary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "L$levelId",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedLevelFilter == levelId) HH_ButtonText else HH_Headline
                        )
                    }
                }
            }
        }
        
        // Grafik Kemajuan Anak (Barchart)
        Card(
            modifier = Modifier.fillMaxWidth().height(240.dp).border(3.dp, HH_Stroke, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = HH_Secondary),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val chartTitle = if (selectedLevelFilter == null) "Grafik Kemajuan" else "Grafik Level $selectedLevelFilter"
                Text(chartTitle, fontWeight = FontWeight.ExtraBold, color = HH_Headline, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (displayHistory.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada data nilai", color = HH_Headline.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                        val recentHistory = displayHistory.takeLast(5)
                        
                        val animationProgress = remember { Animatable(0f) }
                        LaunchedEffect(recentHistory) {
                            animationProgress.snapTo(0f)
                            animationProgress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
                        }

                        Canvas(modifier = Modifier.fillMaxSize().padding(start = 35.dp, end = 10.dp, bottom = 25.dp, top = 20.dp)) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            
                            // Draw horizontal grid lines (Scores: 0, 25, 50, 75, 100)
                            for (i in 0..4) {
                                val yGrid = canvasHeight - (i.toFloat() / 4f) * canvasHeight
                                drawLine(HH_Stroke.copy(alpha = 0.1f), Offset(0f, yGrid), Offset(canvasWidth, yGrid), strokeWidth = 2f)
                            }

                            if (recentHistory.isNotEmpty()) {
                                recentHistory.forEachIndexed { index, record ->
                                    val barWidth = 28.dp.toPx()
                                    val barMaxHeight = canvasHeight
                                    val barHeight = (record.score / 100f) * barMaxHeight
                                    
                                    val x = (index.toFloat() / (recentHistory.size.coerceAtLeast(1))) * canvasWidth + (canvasWidth / (recentHistory.size * 2))
                                    val y = canvasHeight - barHeight * animationProgress.value
                                    
                                    val color = HH_Button
                                    
                                    drawRoundRect(
                                        color = color,
                                        topLeft = Offset(x - barWidth/2, y),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight * animationProgress.value),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                                    )
                                    // Stroke for bar
                                    drawRoundRect(
                                        color = HH_Stroke,
                                        topLeft = Offset(x - barWidth/2, y),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight * animationProgress.value),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }
                        }
                        
                        // Labels Y (Scores)
                        Column(
                            modifier = Modifier.fillMaxHeight().padding(bottom = 25.dp, top = 20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("100", "75", "50", "25", "0").forEach { score ->
                                Text(score, fontSize = 10.sp, color = HH_Headline, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                            }
                        }
                        
                        // Labels X (Level Names)
                        Row(
                            modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(start = 35.dp, end = 10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            recentHistory.forEach { record ->
                                val levelName = levels.find { it.id == record.levelId }?.name?.split(":")?.firstOrNull() ?: "L${record.levelId}"
                                Text(
                                    text = levelName,
                                    fontSize = 11.sp,
                                    color = HH_Headline,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(displayHistory.reversed()) { record ->
                val name = levels.find { it.id == record.levelId }?.name ?: "Level ${record.levelId}"
                Card(
                    modifier = Modifier.fillMaxWidth().border(2.dp, HH_Stroke, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = HH_Secondary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    ListItem(
                        headlineText = { Text(name, fontWeight = FontWeight.ExtraBold, color = HH_Headline) },
                        supportingText = { Text("Skor: ${record.score}", fontWeight = FontWeight.Bold, color = HH_Headline.copy(alpha = 0.7f)) },
                        leadingContent = { 
                            Icon(Icons.Default.Star, contentDescription = null, tint = HH_Button, modifier = Modifier.size(32.dp)) 
                        },
                        trailingContent = {
                            IconButton(onClick = { recordToDelete = record }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Riwayat", tint = HH_Tertiary)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
    
    // Dialog Konfirmasi Hapus Spesifik Riwayat
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Hapus Riwayat Ini?") },
            text = { Text("Apakah kamu yakin ingin menghapus data nilai ini?") },
            confirmButton = {
                TextButton(onClick = { 
                    onDeleteRecord(recordToDelete!!)
                    recordToDelete = null 
                }) { Text("Hapus", color = HH_Tertiary) }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text("Batal") }
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
                }) { Text("HAPUS SEMUA", color = HH_Tertiary) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmAll2 = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun PengaturanScreen(onAboutClick: () -> Unit, onThemeChanged: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE) }

    Column(modifier = Modifier.fillMaxSize().background(HH_Background).padding(16.dp)) {
        Text("Pengaturan", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = HH_Headline)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Tampilan & Tema", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HH_Headline, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().border(3.dp, HH_Stroke, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HH_Secondary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = HH_Headline)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Ganti Tema Warna", fontWeight = FontWeight.Bold, color = HH_Headline)
                    }
                    Button(
                        onClick = {
                            val themes = ThemeColors.getAll()
                            val currentIndex = sharedPreferences.getInt("theme_index", 0)
                            val nextIndex = (currentIndex + 1) % themes.size
                            
                            sharedPreferences.edit().putInt("theme_index", nextIndex).apply()
                            updateThemeColors(themes[nextIndex])
                            onThemeChanged()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HH_Button),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(2.dp, HH_Stroke, RoundedCornerShape(12.dp))
                    ) {
                        Text("Ganti", color = HH_ButtonText)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Data & Aplikasi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HH_Headline, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().border(3.dp, HH_Stroke, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HH_Secondary)
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
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = HH_Headline)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Hapus Cache", fontWeight = FontWeight.Bold, color = HH_Headline)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = HH_Headline)
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = HH_Stroke.copy(alpha = 0.2f), thickness = 2.dp)

                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onAboutClick()
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = HH_Headline)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Tentang Aplikasi", fontWeight = FontWeight.Bold, color = HH_Headline)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = HH_Headline)
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = HH_Stroke.copy(alpha = 0.2f), thickness = 2.dp)

                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        Toast.makeText(context, "Versi 1.0.0 (Android 16 Optimized)", Toast.LENGTH_SHORT).show()
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = HH_Headline)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Versi Aplikasi", fontWeight = FontWeight.Bold, color = HH_Headline)
                    }
                    Text("1.0.0", fontWeight = FontWeight.ExtraBold, color = HH_Button)
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
            .background(HH_Background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.icon_app),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Isyarat Pintar",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = HH_Headline
        )
        Text("Versi 1.0.0", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HH_Button)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Platform edukasi interaktif belajar bahasa isyarat dengan teknologi AR untuk anak-anak.",
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = HH_Headline,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            ContactItem(
                icon = Icons.Default.Phone,
                label = "WhatsApp",
                value = "0813-9982-0510",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send/?phone=6281399820510"))
                    context.startActivity(intent)
                }
            )
            
            ContactItem(
                icon = Icons.Default.Code,
                label = "GitHub",
                value = "Isyarat Pintar",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/murfidnurhadi/Aplikasi-Isyarat-Pintar"))
                    context.startActivity(intent)
                }
            )
            
            ContactItem(
                icon = Icons.Default.Email,
                label = "Email",
                value = "gerkatinpusat@gmail.com",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:gerkatinpusat@gmail.com"))
                    context.startActivity(intent)
                }
            )
        }
        
        Text("© 2024 Isyarat Pintar Team", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HH_Headline.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun ContactItem(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(2.dp, HH_Stroke, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HH_Secondary)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = HH_Button, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HH_Headline.copy(alpha = 0.6f))
                Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = HH_Headline)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = HH_Headline)
        }
    }
}
