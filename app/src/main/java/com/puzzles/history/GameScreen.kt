package com.puzzles.history

import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** مؤثرات صوتية قصيرة للإجابة الصحيحة والخاطئة (الملفات في res/raw) */
class SoundFx(context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val correctId = pool.load(context, R.raw.correct, 1)
    private val wrongId = pool.load(context, R.raw.wrong, 1)

    fun playCorrect() { pool.play(correctId, 1f, 1f, 1, 0, 1f) }
    fun playWrong() { pool.play(wrongId, 1f, 1f, 1, 0, 1f) }
    fun release() { pool.release() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleGameScreen(
    progress: Progress,
    category: String,
    isAdsReady: Boolean,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // زر الرجوع في الهاتف يعيد اللاعب للشاشة الرئيسية
    BackHandler(onBack = onExit)

    val sfx = remember { SoundFx(context) }
    DisposableEffect(Unit) { onDispose { sfx.release() } }
    val scrollState = rememberScrollState()
    val chipsScroll = rememberScrollState()

    // قائمة التصنيفات المتاحة
    val categories = remember { listOf(Progress.ALL) + progress.categories }

    var selectedCategory by remember { mutableStateOf(category) }

    // الأسئلة التي لم تُحَلّ بعد فقط: السؤال المحلول لا يظهر ثانيةً لا في «الكل» ولا في القسم.
    // القائمة تُحسب مرة عند الدخول/تغيير القسم حتى يبقى التنقل بالسابق/التالي مستقراً.
    // وإن حُلّ القسم كاملاً تُعرض كل أسئلته للمراجعة.
    val filteredPuzzles = remember(selectedCategory) {
        val inCategory = if (selectedCategory == Progress.ALL) {
            PuzzleRepository.puzzles
        } else {
            PuzzleRepository.puzzles.filter { it.category == selectedCategory }
        }
        val unsolved = inCategory.filter { it.id !in progress.correctIds }
        if (unsolved.isEmpty()) inCategory else unsolved
    }

    if (filteredPuzzles.isEmpty()) return

    var currentIndex by remember(selectedCategory) { mutableIntStateOf(0) }
    var furthestIndex by remember(selectedCategory) { mutableIntStateOf(0) }

    var showHint by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableIntStateOf(-1) }
    var isAnswered by remember { mutableStateOf(false) }
    var rewardGiven by remember { mutableStateOf(false) }

    val currentPuzzle = filteredPuzzles[currentIndex]

    LaunchedEffect(currentIndex, selectedCategory) { scrollState.scrollTo(0) }

    // يحرّك شريط الأقسام تلقائياً ليظهر القسم المختار (مثل تاريخ العلوم في آخر الشريط)
    LaunchedEffect(selectedCategory, chipsScroll.maxValue) {
        val idx = categories.indexOf(selectedCategory)
        if (idx >= 0 && categories.size > 1 && chipsScroll.maxValue > 0) {
            chipsScroll.animateScrollTo(chipsScroll.maxValue * idx / (categories.size - 1))
        }
    }

    // بعد الإجابة ننزل تلقائياً لآخر الصفحة لتظهر النتيجة والمعلومة التاريخية
    LaunchedEffect(isAnswered) {
        if (isAnswered) {
            kotlinx.coroutines.delay(150)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    fun resetQuestionState() {
        selectedOption = -1
        isAnswered = false
        showHint = false
        rewardGiven = false
    }

    fun goNext() {
        // إعلان بيني كل 3 أسئلة (لا نزعج اللاعب أكثر من ذلك)
        progress.questionsSinceAd++
        if (progress.questionsSinceAd % 3 == 0 && activity != null) {
            AdMobManager.showInterstitial(activity)
        }
        val next = (currentIndex + 1) % filteredPuzzles.size
        currentIndex = next
        if (next > furthestIndex) furthestIndex = next
        resetQuestionState()
    }

    fun goPrevious() {
        if (currentIndex > 0) {
            currentIndex -= 1
            resetQuestionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Text(text = "➡", fontSize = 22.sp, color = Color.White)
                    }
                },
                title = {
                    Text(
                        text = "ألغاز التاريخ (${currentIndex + 1}/${filteredPuzzles.size})",
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
                            text = "🪙 ${progress.coins} عملة",
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
            // أزرار السابق/التالي ثابتة فوق الإعلان مباشرة، فلا تختفي خلفه ولا تحتاج تمريراً
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { goPrevious() },
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF1E3A5F)
                        )
                    ) {
                        Text("➡ السابق", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    // "التالي" يظهر بعد الإجابة، أو عند الرجوع لسؤال سبق الوصول لما بعده
                    if (isAnswered || currentIndex < furthestIndex) {
                        Button(
                            onClick = { goNext() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F))
                        ) {
                            Text("التالي ⬅", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                if (isAdsReady) {
                    BannerAdView(
                        isAdsReady = isAdsReady,
                        adInitError = null
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // شريط اختيار الأقسام (التصنيفات)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(chipsScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { name ->
                        FilterChip(
                            selected = selectedCategory == name,
                            onClick = {
                                if (selectedCategory != name) {
                                    selectedCategory = name
                                    resetQuestionState()
                                    progress.selectCategory(name)
                                }
                            },
                            label = {
                                Text(
                                    text = name,
                                    fontWeight = if (selectedCategory == name) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // بطاقة السؤال
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "القسم: ${currentPuzzle.category}",
                            fontSize = 13.sp,
                            color = Color(0xFF486581),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentPuzzle.question,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF102A43),
                            lineHeight = 25.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                                if (index == currentPuzzle.correctIndex) {
                                    // المكافأة أول مرة فقط لكل سؤال
                                    rewardGiven = progress.recordCorrect(currentPuzzle.id)
                                    if (progress.soundOn) sfx.playCorrect()
                                } else {
                                    if (progress.soundOn) sfx.playWrong()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
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
                                .padding(vertical = 0.dp)
                        )
                    }
                }

                // عرض التلميح
                if (showHint) {
                    Spacer(modifier = Modifier.height(6.dp))
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

                // نتيجة الإجابة
                if (isAnswered) {
                    val wasCorrect = selectedOption == currentPuzzle.correctIndex
                    val resultText = when {
                        !wasCorrect -> "❌ إجابة خاطئة"
                        rewardGiven -> "✅ إجابة صحيحة! +${Progress.CORRECT_REWARD} عملة"
                        else -> "✅ إجابة صحيحة (سبق أن حللت هذا السؤال)"
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (wasCorrect) Color(0xFFD4EDDA) else Color(0xFFFFE0CC)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = resultText,
                            color = if (wasCorrect) Color(0xFF155724) else Color(0xFF8A3B12),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // عرض المعلومة التاريخية بعد الإجابة
                    Spacer(modifier = Modifier.height(6.dp))
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

            Spacer(modifier = Modifier.height(8.dp))

            // أزرار التلميح والمكافأة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (!showHint) {
                            if (progress.spendCoins(30)) {
                                showHint = true
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
                                    progress.addCoins(50)
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
        }
    }
}
