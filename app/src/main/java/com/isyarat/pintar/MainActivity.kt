package com.isyarat.pintar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HH_Background),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = startAnimation,
            enter = fadeIn(animationSpec = tween(800)) + scaleIn(
                initialScale = 0.5f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            exit = fadeOut(animationSpec = tween(600))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Langsung tampilkan Image tanpa container Box agar tidak ada efek border
                Image(
                    painter = painterResource(id = R.drawable.icon_app),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(240.dp)
                        .padding(16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Isyarat Pintar",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HH_Headline,
                    letterSpacing = 2.sp
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
            ThemeColors.getAll().getOrElse(themeIndex) { ThemeColors.PaletteLight }
        )
    }

    var currentScreen by rememberSaveable { mutableStateOf("beranda") }
    var selectedLevel by rememberSaveable { mutableStateOf<Level?>(null) }
    val levelsState = rememberSaveable(key = "levels_v3", saver = listSaver<SnapshotStateList<Level>, Level>(
        save = { it.toList() },
        restore = { it.toMutableStateList() }
    )) { IsyaratData.levels.toMutableStateList() }

    val historyState = rememberSaveable(saver = listSaver<SnapshotStateList<HistoryRecord>, HistoryRecord>(
        save = { it.toList() },
        restore = { it.toMutableStateList() }
    )) { mutableStateListOf<HistoryRecord>() }

    var restartKey by rememberSaveable { mutableStateOf(0) }
    
    // Handler Tombol Back
    BackHandler(enabled = currentScreen != "beranda") {
        if (currentScreen == "riwayat" || currentScreen == "pengaturan" || currentScreen == "scanner" || currentScreen == "quiz") {
            currentScreen = "beranda"
        }
    }
    
    var fabVisible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(currentScreen) {
        if (currentScreen == "beranda") {
            delay(1000) // Selisih 1 detik
            fabVisible = true
        } else {
            fabVisible = false
        }
    }

    key(restartKey) {
        Box(modifier = Modifier.fillMaxSize().background(HH_Background)) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    if (currentScreen != "scanner" && currentScreen != "quiz") {
                        val configuration = LocalConfiguration.current
                        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                        if (!(currentScreen == "beranda" && isLandscape)) {
                            Column {
                                TopAppBar(
                                    title = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Image(
                                                painter = painterResource(id = R.drawable.icon_app),
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Isyarat Aplikasi", fontWeight = FontWeight.ExtraBold, color = HH_Headline)
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent,
                                        titleContentColor = HH_Headline
                                    )
                                )
                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 2.dp,
                                    color = HH_Headline.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                },
                bottomBar = {
                    if (currentScreen != "scanner" && currentScreen != "quiz") {
                        val configuration = LocalConfiguration.current
                        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                        NavigationBar(
                            containerColor = HH_Secondary,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .height(if (isLandscape) 64.dp else 80.dp)
                                .border(width = 2.dp, color = HH_Stroke, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == "beranda",
                                onClick = { currentScreen = "beranda" },
                                icon = { Icon(Icons.Rounded.Home, null) },
                                label = { Text("Beranda", fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 11.sp else 14.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HH_Headline,
                                    unselectedIconColor = HH_Headline.copy(alpha = 0.5f),
                                    selectedTextColor = HH_Headline,
                                    unselectedTextColor = HH_Headline.copy(alpha = 0.5f),
                                    indicatorColor = HH_NavIndicator
                                )
                            )
                            NavigationBarItem(
                                selected = currentScreen == "riwayat",
                                onClick = { currentScreen = "riwayat" },
                                icon = { Icon(Icons.Rounded.History, null) },
                                label = { Text("Riwayat", fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 11.sp else 14.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HH_Headline,
                                    unselectedIconColor = HH_Headline.copy(alpha = 0.5f),
                                    selectedTextColor = HH_Headline,
                                    unselectedTextColor = HH_Headline.copy(alpha = 0.5f),
                                    indicatorColor = HH_NavIndicator
                                )
                            )
                            NavigationBarItem(
                                selected = currentScreen == "pengaturan",
                                onClick = { currentScreen = "pengaturan" },
                                icon = { Icon(Icons.Rounded.Settings, null) },
                                label = { Text("Pengaturan", fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 11.sp else 14.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HH_Headline,
                                    unselectedIconColor = HH_Headline.copy(alpha = 0.5f),
                                    selectedTextColor = HH_Headline,
                                    unselectedTextColor = HH_Headline.copy(alpha = 0.5f),
                                    indicatorColor = HH_NavIndicator
                                )
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (currentScreen == "beranda") {
                        AnimatedVisibility(
                            visible = fabVisible,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(1000, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(1000)),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            FloatingActionButton(
                                onClick = { currentScreen = "scanner" },
                                containerColor = HH_Button,
                                contentColor = HH_ButtonText,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.border(2.dp, HH_Stroke, RoundedCornerShape(16.dp))
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCode,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Scanner AR", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    when (currentScreen) {
                        "beranda" -> BerandaScreen(levelsState) { level ->
                            selectedLevel = level
                            currentScreen = "quiz"
                        }
                        "quiz" -> {
                            selectedLevel?.let { level ->
                                QuizScreen(
                                    level = level,
                                    onFinished = { finalScore ->
                                        historyState.add(HistoryRecord(levelId = level.id, score = finalScore))
                                        
                                        val levelIndex = levelsState.indexOfFirst { it.id == level.id }
                                        if (levelIndex != -1) {
                                            levelsState[levelIndex] = levelsState[levelIndex].copy(
                                                score = finalScore,
                                                isCompleted = true
                                            )
                                            
                                            if (finalScore <= 25) {
                                                // Cascade lock and history clear
                                                for (i in (levelIndex + 1) until levelsState.size) {
                                                    val subLevelId = levelsState[i].id
                                                    levelsState[i] = levelsState[i].copy(
                                                        isUnlocked = false,
                                                        isCompleted = false,
                                                        score = 0
                                                    )
                                                    historyState.removeAll { it.levelId == subLevelId }
                                                }
                                            } else if (levelIndex + 1 < levelsState.size) {
                                                levelsState[levelIndex + 1] = levelsState[levelIndex + 1].copy(
                                                    isUnlocked = true
                                                )
                                            }
                                        }
                                        currentScreen = "beranda"
                                    },
                                    onBack = { currentScreen = "beranda" }
                                )
                            }
                        }
                        "scanner" -> ScannerScreen(onBack = { currentScreen = "beranda" })
                        "riwayat" -> RiwayatScreen(
                            history = historyState,
                            levels = levelsState,
                            onResetAll = {
                                historyState.clear()
                                levelsState.clear()
                                levelsState.addAll(IsyaratData.levels)
                            },
                            onDeleteRecord = { record ->
                                historyState.remove(record)
                                val levelIndex = levelsState.indexOfFirst { it.id == record.levelId }
                                if (levelIndex != -1) {
                                    val levelHistory = historyState.filter { it.levelId == record.levelId }
                                    val maxScore = levelHistory.maxOfOrNull { it.score } ?: 0
                                    
                                    levelsState[levelIndex] = levelsState[levelIndex].copy(
                                        score = maxScore,
                                        isCompleted = levelHistory.isNotEmpty()
                                    )
                                    
                                    if (maxScore <= 25) {
                                        // Cascade lock and history clear
                                        for (i in (levelIndex + 1) until levelsState.size) {
                                            val subLevelId = levelsState[i].id
                                            levelsState[i] = levelsState[i].copy(
                                                isUnlocked = false,
                                                isCompleted = false,
                                                score = 0
                                            )
                                            historyState.removeAll { it.levelId == subLevelId }
                                        }
                                    } else if (levelIndex + 1 < levelsState.size) {
                                        levelsState[levelIndex + 1] = levelsState[levelIndex + 1].copy(
                                            isUnlocked = true
                                        )
                                    }
                                }
                            }
                        )
                        "pengaturan" -> {
                            PengaturanScreen(onAboutClick = { currentScreen = "tentang" }) {
                                restartKey++
                            }
                        }
                        "tentang" -> TentangKamiScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun BerandaScreen(levels: List<Level>, onLevelClick: (Level) -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(1000, easing = LinearOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(1000))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(if (isLandscape) 8.dp else 16.dp)) {
            if (isLandscape) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.icon_app),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Isyarat Aplikasi", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = HH_Headline)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Halo, Ayo Pilih Permainanmu!", fontSize = 14.sp, color = HH_Headline.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Halo Teman-Teman!", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = HH_Headline)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Ayo Pilih Permainanmu!", fontSize = 18.sp, color = HH_Headline.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 12.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isLandscape) 2 else 1),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(levels) { level ->
                    LevelItem(level, onLevelClick)
                }
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
            .border(3.dp, HH_Stroke, RoundedCornerShape(24.dp))
            .clickable(enabled = level.isUnlocked) { onClick(level) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (level.isUnlocked) HH_Secondary else HH_Background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (level.isUnlocked) 8.dp else 0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Gambar Ikon Level berdasarkan level.icon
            val iconName = level.icon
            val resId = if (iconName.isNotEmpty()) {
                context.resources.getIdentifier(iconName, "drawable", context.packageName)
            } else 0

            if (resId != 0 && level.isUnlocked) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .background(HH_Background, RoundedCornerShape(12.dp))
                        .border(2.dp, HH_Stroke, RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            if (level.isUnlocked) HH_Button.copy(alpha = 0.2f)
                            else HH_Headline.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (level.isUnlocked) Icons.Rounded.PlayArrow else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = if (level.isUnlocked) HH_Headline else HH_Headline.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(level.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = HH_Headline)
                if (level.isCompleted) Text("Skor: ${level.score}", fontSize = 14.sp, color = HH_Headline.copy(alpha = 0.8f))
                else if (!level.isUnlocked) Text("Terkunci", fontSize = 14.sp, color = HH_Headline.copy(alpha = 0.6f))
                else Text("Ayo Mulai!", fontSize = 14.sp, color = HH_Button, fontWeight = FontWeight.Bold)
            }
            if (level.isCompleted) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = HH_Button, modifier = Modifier.size(28.dp))
            }
        }
    }
}
@Composable
fun QuizScreen(level: Level, onFinished: (Int) -> Unit, onBack: () -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var currentQuestionIndex by rememberSaveable { mutableStateOf(0) }
    var score by rememberSaveable { mutableStateOf(0) }
    var correctCount by rememberSaveable { mutableStateOf(0) }
    var showFinalDialog by rememberSaveable { mutableStateOf(false) }
    var shakingIndex by remember { mutableStateOf<Int?>(null) }
    var isShakingCorrect by remember { mutableStateOf(true) }
    var timeLeft by rememberSaveable { mutableStateOf(20) }
    var showHint by rememberSaveable { mutableStateOf(false) }
    var lastQuestionIndex by rememberSaveable { mutableStateOf(-1) }

    LaunchedEffect(currentQuestionIndex, level.id) {
        if (lastQuestionIndex != currentQuestionIndex) {
            timeLeft = 20
            showHint = false
            lastQuestionIndex = currentQuestionIndex
        }
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "hintPulse")
    val hintScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintScale"
    )
    
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
        val canUnlockNext = score > 25
        AlertDialog(
            onDismissRequest = { },
            containerColor = HH_Background,
            titleContentColor = HH_Headline,
            textContentColor = HH_Headline,
            modifier = Modifier.border(4.dp, HH_Stroke, RoundedCornerShape(28.dp)),
            title = {
                    Text(
                        if (isPerfect) "HEBAT SEKALI!" else if (canUnlockNext) "BAGUS!" else "YUK, COBA LAGI!",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HH_Button
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
                            } else if (!canUnlockNext) {
                                Text("😢", fontSize = 56.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = (-40).dp, y = (emojiOffset - 30).dp))
                                Text("☹️", fontSize = 56.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (emojiOffset - 30).dp))
                            }
                            
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(180.dp)
                                    .scale(scale)
                                    .border(3.dp, HH_Stroke, RoundedCornerShape(24.dp))
                                    .background(Color.White, RoundedCornerShape(24.dp))
                                    .padding(8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Kamu telah menyelesaikan level ini!", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Text("Nilai Kamu: $score", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = HH_Button)
                    if (!canUnlockNext && level.id < 5) {
                        Text(
                            "Dapatkan skor lebih dari 25 untuk membuka level berikutnya!",
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Sisi Kiri: Soal
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = HH_Headline)
                        }
                        Text("Pertanyaan ${currentQuestionIndex + 1}/${level.questions.size}", 
                            modifier = Modifier.weight(1f), 
                            textAlign = TextAlign.Center, 
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = HH_Headline
                        )
                        AnimatedVisibility(
                            visible = timeLeft == 0,
                            enter = fadeIn() + scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                            exit = fadeOut() + scaleOut()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = { showHint = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Lightbulb,
                                        contentDescription = "Hint",
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    "BANTUAN",
                                    color = Color(0xFFFF9800),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.offset(y = (-4).dp)
                                )
                            }
                        }
                        if (timeLeft > 0) {
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    }
                    
                    LinearProgressIndicator(
                        progress = (currentQuestionIndex + 1).toFloat() / level.questions.size,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .padding(vertical = 4.dp)
                            .border(2.dp, HH_Stroke, RoundedCornerShape(8.dp)),
                        color = HH_Button,
                        trackColor = HH_Secondary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(3.dp, HH_Stroke, RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = HH_Secondary),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text(
                                currentQuestion.text,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HH_Headline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Sisi Kanan: Pilihan Jawaban
                Column(modifier = Modifier.weight(1.2f)) {
                    Text("Pilih Gambar yang Benar:", fontSize = 14.sp, color = HH_Headline, modifier = Modifier.padding(bottom = 8.dp))
                    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
                        items(shuffledOptions.size) { index ->
                            val option = shuffledOptions[index]
                            val resId = context.resources.getIdentifier(option, "drawable", context.packageName)
                            val translationX = if (shakingIndex == index) shakeAnim.value else 0f
                            val isCorrectOption = currentQuestion.correctAnswerImages.contains(option)
                            val scale = if (showHint && isCorrectOption) hintScale else 1f

                            Card(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .aspectRatio(1.3f)
                                    .scale(scale)
                                    .offset(x = translationX.dp)
                                    .border(
                                        width = 4.dp,
                                        color = when {
                                            shakingIndex == index && isShakingCorrect -> Color.Green
                                            shakingIndex == index && !isShakingCorrect -> Color.Red
                                            showHint && isCorrectOption -> Color.Yellow
                                            else -> HH_Stroke
                                        },
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable {
                                        if (shakingIndex != null) return@clickable
                                        
                                    val isCorrect = currentQuestion.correctAnswerImages.contains(option)
                                        shakingIndex = index
                                        isShakingCorrect = isCorrect
                                        
                                        if (isCorrect) {
                                            correctCount++
                                            score = when (level.id) {
                                                3 -> when (correctCount) {
                                                    1 -> 5
                                                    2 -> 10
                                                    3 -> 30
                                                    4 -> 45
                                                    5 -> 70
                                                    6 -> 85
                                                    7 -> 100
                                                    else -> score
                                                }
                                                5 -> when (correctCount) {
                                                    1 -> 10
                                                    2 -> 20
                                                    3 -> 40
                                                    4 -> 60
                                                    5 -> 80
                                                    6 -> 100
                                                    else -> score
                                                }
                                                else -> {
                                                    val pointsPerQuestion = 100 / level.questions.size
                                                    correctCount * pointsPerQuestion
                                                }
                                            }
                                        }
                                        
                                        scope.launch {
                                            delay(if (isCorrect) 600 else 800)
                                            shakingIndex = null
                                            
                                            if (currentQuestionIndex < level.questions.size - 1) {
                                                currentQuestionIndex++
                                            } else {
                                                showFinalDialog = true
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(24.dp),
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
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = HH_Headline)
                    }
                    Text("Pertanyaan ${currentQuestionIndex + 1}/${level.questions.size}", 
                        modifier = Modifier.weight(1f), 
                        textAlign = TextAlign.Center, 
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = HH_Headline
                    )
                    AnimatedVisibility(
                        visible = timeLeft == 0,
                        enter = fadeIn() + scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { showHint = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Lightbulb,
                                    contentDescription = "Hint",
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                "BANTUAN",
                                color = Color(0xFFFF9800),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.offset(y = (-4).dp)
                            )
                        }
                    }
                    if (timeLeft > 0) {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
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
                
                Text("Pilih Gambar yang Benar:", fontSize = 16.sp, color = HH_Headline, modifier = Modifier.padding(top = 24.dp))
                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f)) {
                    items(shuffledOptions.size) { index ->
                        val option = shuffledOptions[index]
                        val resId = context.resources.getIdentifier(option, "drawable", context.packageName)
                        
                        val translationX = if (shakingIndex == index) shakeAnim.value else 0f
                        val isCorrectOption = currentQuestion.correctAnswerImages.contains(option)
                        val scale = if (showHint && isCorrectOption) hintScale else 1f

                        Card(
                            modifier = Modifier
                                .padding(8.dp)
                                .aspectRatio(1f)
                                .scale(scale)
                                .offset(x = translationX.dp)
                                .border(
                                    width = 4.dp,
                                    color = when {
                                        shakingIndex == index && isShakingCorrect -> Color.Green
                                        shakingIndex == index && !isShakingCorrect -> Color.Red
                                        showHint && isCorrectOption -> Color.Yellow
                                        else -> HH_Stroke
                                    },
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .clickable {
                                    if (shakingIndex != null) return@clickable
                                    
                                    val isCorrect = currentQuestion.correctAnswerImages.contains(option)
                                    shakingIndex = index
                                    isShakingCorrect = isCorrect
                                    
                                    if (isCorrect) {
                                        correctCount++
                                        score = when (level.id) {
                                            3 -> when (correctCount) {
                                                1 -> 5
                                                2 -> 10
                                                3 -> 30
                                                4 -> 45
                                                5 -> 70
                                                6 -> 85
                                                7 -> 100
                                                else -> score
                                            }
                                            5 -> when (correctCount) {
                                                1 -> 10
                                                2 -> 20
                                                3 -> 40
                                                4 -> 60
                                                5 -> 80
                                                6 -> 100
                                                else -> score
                                            }
                                            else -> {
                                                val pointsPerQuestion = 100 / level.questions.size
                                                correctCount * pointsPerQuestion
                                            }
                                        }
                                    }
                                    
                                    scope.launch {
                                        delay(if (isCorrect) 600 else 800)
                                        shakingIndex = null
                                        
                                        if (currentQuestionIndex < level.questions.size - 1) {
                                            currentQuestionIndex++
                                        } else {
                                            showFinalDialog = true
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(24.dp),
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
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerScreen(onBack: () -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var detectedText by rememberSaveable { mutableStateOf("") }
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
            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = HH_Background.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.border(4.dp, HH_Stroke, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("1️⃣ Arahkan ke Kata", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = HH_Headline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("2️⃣ Lihat Isyarat Muncul!", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = HH_Headline)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(width = if (isLandscape) 200.dp else 280.dp, height = if (isLandscape) 120.dp else 180.dp)
                        .border(4.dp, HH_Button, RoundedCornerShape(24.dp))
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
                                .size(if (isLandscape) 120.dp else 200.dp) // Ukuran sedikit lebih besar untuk video/gif
                        )
                    }
                }

                // Tambahkan tombol untuk reset deteksi
                Button(
                    onClick = { detectedText = "" },
                    modifier = Modifier.padding(top = 16.dp).border(2.dp, HH_Stroke, RoundedCornerShape(50)),
                    colors = ButtonDefaults.buttonColors(containerColor = HH_Button, contentColor = HH_ButtonText)
                ) {
                    Text("Coba Kata Lain")
                }
            }
        }
        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).align(Alignment.TopStart)) { 
            Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = Color.White) 
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var showConfirmAll1 by rememberSaveable { mutableStateOf(false) }
    var showConfirmAll2 by rememberSaveable { mutableStateOf(false) }
    var recordToDelete by rememberSaveable { mutableStateOf<HistoryRecord?>(null) }
    var selectedLevelFilter by rememberSaveable { mutableStateOf<Int?>(null) }

    val levelColors = listOf(
        HH_Button,
        HH_Headline,
        HH_Button,
        HH_Headline,
        HH_Button
    )

    Box(modifier = Modifier.fillMaxSize().padding(if (isLandscape) 8.dp else 16.dp).alpha(if (visible) 1f else 0f)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            val displayHistory = if (selectedLevelFilter == null) history else history.filter { it.levelId == selectedLevelFilter }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Riwayat Nilai", fontSize = if (isLandscape) 24.sp else 32.sp, fontWeight = FontWeight.ExtraBold, color = HH_Headline)
            IconButton(onClick = { showConfirmAll1 = true }) { 
                Icon(Icons.Rounded.DeleteForever, null, tint = HH_Button, modifier = Modifier.size(if (isLandscape) 24.dp else 32.dp)) 
            }
        }
        
        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))

        // Box List Filter Level
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Filter "Semua"
            Surface(
                onClick = { selectedLevelFilter = null },
                modifier = Modifier.weight(1.5f).height(if (isLandscape) 40.dp else 48.dp).border(2.dp, HH_Stroke, RoundedCornerShape(12.dp)),
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
                    modifier = Modifier.weight(1f).height(if (isLandscape) 40.dp else 48.dp).border(2.dp, HH_Stroke, RoundedCornerShape(12.dp)),
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
            modifier = Modifier.fillMaxWidth().height(if (isLandscape) 160.dp else 240.dp).border(3.dp, HH_Stroke, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = HH_Secondary),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val chartTitle = if (selectedLevelFilter == null) "Grafik Kemajuan" else "Grafik Level $selectedLevelFilter"
                Text(chartTitle, color = HH_Headline, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (displayHistory.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada data nilai", color = HH_Headline.copy(alpha = 0.5f))
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
                                    
                                    val color = levelColors.getOrElse(record.levelId - 1) { HH_Button }
                                    
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
                                Text(score, fontSize = 10.sp, color = HH_Headline, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
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

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            displayHistory.reversed().forEach { record ->
                val name = levels.find { it.id == record.levelId }?.name ?: "Level ${record.levelId}"
                Card(
                    modifier = Modifier.fillMaxWidth().border(2.dp, HH_Stroke, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = HH_Secondary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(name, fontWeight = FontWeight.ExtraBold, color = HH_Headline) },
                        supportingContent = { Text("Skor: ${record.score}", color = HH_Headline.copy(alpha = 0.7f)) },
                        leadingContent = { 
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = HH_Button, modifier = Modifier.size(32.dp)) 
                        },
                        trailingContent = {
                            IconButton(onClick = { recordToDelete = record }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus Riwayat", tint = HH_Button)
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
                }) { Text("Hapus", color = HH_Button) }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text("Batal", color = HH_Button) }
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
                }) { Text("Ya", color = HH_Button) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmAll1 = false }) { Text("Batal", color = HH_Button) }
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
                }) { Text("HAPUS SEMUA", color = HH_Button) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmAll2 = false }) { Text("Batal", color = HH_Button) }
            }
        )
    }
}
}

@Composable
fun PengaturanScreen(onAboutClick: () -> Unit, onThemeChanged: () -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(1000, easing = LinearOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(1000))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isLandscape) 8.dp else 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Pengaturan", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = HH_Headline)

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Tampilan & Tema", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HH_Headline, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().border(3.dp, HH_Stroke, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = HH_Secondary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (sharedPreferences.getInt("theme_index", 0) == 1) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, contentDescription = null, tint = HH_Headline)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Mode Gelap", fontWeight = FontWeight.Bold, color = HH_Headline)
                    }
                    val currentIndex = sharedPreferences.getInt("theme_index", 0)
                    Switch(
                        checked = currentIndex == 1,
                        onCheckedChange = { isDark ->
                            val newIndex = if (isDark) 1 else 0
                            sharedPreferences.edit().putInt("theme_index", newIndex).apply()
                            updateThemeColors(ThemeColors.getAll()[newIndex])
                            onThemeChanged()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HH_Button,
                            checkedTrackColor = HH_Headline.copy(alpha = 0.3f),
                            uncheckedThumbColor = HH_Headline,
                            uncheckedTrackColor = HH_Headline.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Data & Aplikasi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HH_Headline, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().border(3.dp, HH_Stroke, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
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
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = HH_Button)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Hapus Cache", fontWeight = FontWeight.Bold, color = HH_Button)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = HH_Headline)
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
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = HH_Headline)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Tentang Aplikasi", fontWeight = FontWeight.Bold, color = HH_Headline)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = HH_Headline)
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = HH_Stroke.copy(alpha = 0.2f), thickness = 2.dp)

                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        Toast.makeText(context, "Versi 1.0.0 (Terbaru)", Toast.LENGTH_SHORT).show()
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = HH_Headline)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Versi Aplikasi", fontWeight = FontWeight.Bold, color = HH_Headline)
                    }
                    Text("Terbaru", fontWeight = FontWeight.ExtraBold, color = HH_Button)
                }
            }
        }
    }
}
}

@Composable
fun TentangKamiScreen() {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (visible) 1f else 0f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon_app),
                contentDescription = null,
                modifier = Modifier.size(if (isLandscape) 60.dp else 100.dp)
            )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Isyarat Pintar",
            fontSize = if (isLandscape) 20.sp else 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = HH_Headline
        )
        Text("Versi 1.0.0", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HH_Button)
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Platform edukasi interaktif belajar bahasa isyarat dengan teknologi AR untuk anak-anak.",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = HH_Headline,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column {
            ContactItem(
                icon = Icons.Rounded.Phone,
                label = "WhatsApp",
                value = "0813-9982-0510",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send/?phone=6281399820510"))
                    context.startActivity(intent)
                }
            )
            
            ContactItem(
                icon = Icons.Rounded.Code,
                label = "GitHub",
                value = "Isyarat Pintar",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/murfidnurhadi/Aplikasi-Isyarat-Pintar"))
                    context.startActivity(intent)
                }
            )
            
            ContactItem(
                icon = Icons.Rounded.Email,
                label = "Email",
                value = "gerkatinpusat@gmail.com",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:gerkatinpusat@gmail.com"))
                    context.startActivity(intent)
                }
            )
        }
        
        
        Text("© 2026 Isyarat Pintar Team", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HH_Headline.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
    }
}
}

@Composable
fun ContactItem(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(2.dp, HH_Stroke, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
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
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = HH_Headline)
        }
    }
}
