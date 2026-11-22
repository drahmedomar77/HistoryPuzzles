package com.puzzles.history

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {

    private var isAdsInitialized by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            MobileAds.initialize(this) {
                isAdsInitialized = true
                AdMobManager.loadInterstitial(this)
                AdMobManager.loadRewarded(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    PuzzleGameScreen(isAdsReady = isAdsInitialized)
                }
            }
        }
    }
}

private const val ALL_CATEGORIES = "الكل"
private const val AD_EVERY_N_PUZZLES = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleGameScreen(isAdsReady: Boolean) {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { context.getSharedPreferences("history_puzzles_prefs", Context.MODE_PRIVATE) }

    // قائمة التصنيفات المتاحة
    val categories = remember {
        listOf(ALL_CATEGORIES) + PuzzleRepository.puzzles.map { it.category }.distinct()
    }

    var selectedCategory by remember {
        val saved = prefs.getString("selected_category", ALL_CATEGORIES) ?: ALL_CATEGORIES
        mutableStateOf(if (saved in categories) saved else ALL_CATEGORIES)
    }

    val filteredPuzzles = remember(selectedCategory) {
        if (selectedCategory == ALL_CATEGORIES) {
            PuzzleRepository.puzzles
        } else {
            PuzzleRepository.puzzles.filter { it.category == selectedCategory }
        }
    }

    var currentIndex by remember(selectedCategory) {
        val savedIdx = prefs.getInt("index_$selectedCategory", 0)
        mutableIntStateOf(if (savedIdx in filteredPuzzles.indices) savedIdx else 0)
    }

    var coins by remember { mutableIntStateOf(prefs.getInt("user_coins", 50)) }
    var showHint by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableIntStateOf(-1) }
    var isAnswered by remember { mutableStateOf(false) }
    var solvedCount by remember { mutableIntStateOf(prefs.getInt("solved_count", 0)) }

    // عدد الألغاز التي أُجيب عنها منذ آخر إعلان بيني (صحيحة كانت أو خاطئة)
    var puzzlesSinceAd by remember { mutableIntStateOf(0) }

    // حفظ التقدم والعملات تلقائياً
    fun saveProgress(newCoins: Int = coins, newIndex: Int = currentIndex, newSolved: Int = solvedCount) {
        prefs.edit()
            .putInt("user_coins", newCoins)
            .putInt("index_$selectedCategory", newIndex)
            .putInt("solved_count", newSolved)
            .putString("selected_category", selectedCategory)
            .apply()
    }

    // لا يوجد انهيار إذا كانت القائمة فارغة: نعرض رسالة بدل الوصول لعنصر غير موجود
    val currentPuzzle = filteredPuzzles.getOrNull(currentIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentPuzzle != null) {
                            "ألغاز التاريخ (${currentIndex + 1}/${filteredPuzzles.size})"
                        } else {
                            "ألغاز التاريخ"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    Surface(
                        color = Color(0xFFFFF3CD),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "🪙 $coins عملة",
                            color = Color(0xFF856404),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E3A5F),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (isAdsReady) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                ) {
                    BannerAdView()
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // شريط اختيار الأقسام (التصنيفات)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                if (selectedCategory != category) {
                                    selectedCategory = category
                                    selectedOption = -1
                                    isAnswered = false
                                    showHint = false
                                    prefs.edit().putString("selected_category", category).apply()
                                }
                            },
                            label = {
                                Text(
                                    text = category,
                                    fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (currentPuzzle == null) {
                    Text(
                        text = "لا توجد ألغاز في هذا القسم حالياً.",
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    // بطاقة السؤال
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "القسم: ${currentPuzzle.category}",
                                fontSize = 13.sp,
                                color = Color(0xFF486581),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentPuzzle.question,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF102A43),
                                lineHeight = 28.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // خيارات الإجابة
                    for (index in currentPuzzle.options.indices) {
                        val optionText = currentPuzzle.options[index]
                        val buttonColor = when {
                            !isAnswered -> Color(0xFFFFFFFF)
                            index == currentPuzzle.correctIndex -> Color(0xFFD4EDDA)
                            index == selectedOption -> Color(0xFFFFB07A)
                            else -> Color(0xFFFFFFFF)
                        }

                        OutlinedButton(
                            onClick = {
                                if (!isAnswered) {
                                    selectedOption = index
                                    isAnswered = true
                                    puzzlesSinceAd++
                                    // نحفظ الفهرس التالي فوراً حتى لا يمكن تكرار نفس اللغز لكسب العملات عند إعادة فتح التطبيق
                                    val nextIndex = (currentIndex + 1) % filteredPuzzles.size
                                    if (index == currentPuzzle.correctIndex) {
                                        coins += 20
                                        solvedCount++
                                        saveProgress(newCoins = coins, newIndex = nextIndex, newSolved = solvedCount)
                                        Toast.makeText(context, "إجابة صحيحة! +20 عملة", Toast.LENGTH_SHORT).show()
                                    } else {
                                        saveProgress(newIndex = nextIndex)
                                        Toast.makeText(context, "إجابة خاطئة!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = buttonColor)
                        ) {
                            Text(
                                text = optionText,
                                fontSize = 16.sp,
                                color = Color.Black,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    // عرض التلميح
                    if (showHint) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 تلميح: ${currentPuzzle.hint}",
                                color = Color(0xFF6D4C00),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // عرض المعلومة التاريخية بعد الإجابة
                    if (isAnswered) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📜 معلومة تاريخية: ${currentPuzzle.historicalFact}",
                                color = Color(0xFF0D47A1),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // أزرار التلميح والمكافأة والانتقال للتالي
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (currentPuzzle != null && !isAnswered && !showHint) {
                                if (coins >= 30) {
                                    coins -= 30
                                    showHint = true
                                    saveProgress(newCoins = coins)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "رصيدك غير كافٍ (تحتاج 30 عملة)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Text("💡 تلميح (30 🪙)", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            activity?.let { act ->
                                AdMobManager.showRewarded(
                                    activity = act,
                                    onRewardEarned = {
                                        coins += 50
                                        saveProgress(newCoins = coins)
                                        Toast.makeText(context, "تمت إضافة 50 عملة لرصيدك!", Toast.LENGTH_SHORT).show()
                                    },
                                    onNotReady = {
                                        Toast.makeText(context, "جاري تجهيز الإعلان، حاول بعد ثوانٍ", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("🎬 +50 🪙 مجاناً", fontSize = 14.sp)
                    }
                }

                if (isAnswered && currentPuzzle != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            // إعلان بيني كل 3 ألغاز مُجاب عنها فقط، ثم نعيد العدّاد
                            if (puzzlesSinceAd >= AD_EVERY_N_PUZZLES && activity != null) {
                                AdMobManager.showInterstitial(activity)
                                puzzlesSinceAd = 0
                            }
                            val nextIndex = (currentIndex + 1) % filteredPuzzles.size
                            currentIndex = nextIndex
                            selectedOption = -1
                            isAnswered = false
                            showHint = false
                            saveProgress(newIndex = nextIndex)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F))
                    ) {
                        Text("اللغز التالي ⬅", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
