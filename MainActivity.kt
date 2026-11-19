package com.puzzles.history

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // تهيئة إعلانات AdMob وتحميل الإعلانات مسبقاً
        MobileAds.initialize(this) {}
        AdMobManager.loadInterstitial(this)
        AdMobManager.loadRewarded(this)

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    PuzzleGameScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleGameScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val puzzles = PuzzleRepository.puzzles

    var currentIndex by remember { mutableIntStateOf(0) }
    var coins by remember { mutableIntStateOf(50) } // رصيد البداية 50 عملة
    var showHint by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableIntStateOf(-1) }
    var isAnswered by remember { mutableStateOf(false) }
    var solvedCount by remember { mutableIntStateOf(0) }

    val currentPuzzle = puzzles[currentIndex]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ألغاز التاريخ (${currentIndex + 1}/${puzzles.size})",
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
            // إعلان بانر في أسفل الشاشة بعيداً عن أزرار الإجابة
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
            ) {
                BannerAdView()
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // تصنيف اللغز
                AssistChip(
                    onClick = {},
                    label = { Text(currentPuzzle.category, fontWeight = FontWeight.SemiBold) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // بطاقة نص اللغز
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = currentPuzzle.question,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF102A43),
                        lineHeight = 28.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // خيارات الإجابة
                currentPuzzle.options.forEachIndexed { index, optionText ->
                    val buttonColor = when {
                        !isAnswered -> Color(0xFFFFFFFF)
                        index == currentPuzzle.correctIndex -> Color(0xFFD4EDDA)
                        index == selectedOption -> Color(0xFFF8D7DA)
                        else -> Color(0xFFFFFFFF)
                    }

                    OutlinedButton(
                        onClick = {
                            if (!isAnswered) {
                                selectedOption = index
                                isAnswered = true
                                if (index == currentPuzzle.correctIndex) {
                                    coins += 20
                                    solvedCount++
                                    Toast.makeText(context, "إجابة صحيحة! +20 عملة", Toast.LENGTH_SHORT).show()
                                } else {
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
                                .padding(vertical = 6.dp)
                        )
                    }
                }

                // عرض التلميح إذا تم طلبه
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

            // أزرار التلميح والمكافآت والانتقال للتالي
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // زر استخدام التلميح مقابل 30 عملة
                    Button(
                        onClick = {
                            if (!showHint) {
                                if (coins >= 30) {
                                    coins -= 30
                                    showHint = true
                                } else {
                                    Toast.makeText(context, "رصيدك غير كافٍ (تحتاج 30 عملة)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Text("💡 تلميح (30 🪙)", fontSize = 14.sp)
                    }

                    // زر إعلان بمكافأة للحصول على 50 عملة مجاناً
                    Button(
                        onClick = {
                            activity?.let { act ->
                                AdMobManager.showRewarded(
                                    activity = act,
                                    onRewardEarned = {
                                        coins += 50
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

                if (isAnswered) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            // عرض إعلان بيني كل 3 ألغاز محلولة
                            if (solvedCount > 0 && solvedCount % 3 == 0 && activity != null) {
                                AdMobManager.showInterstitial(activity)
                            }
                            // الانتقال للغز التالي
                            currentIndex = (currentIndex + 1) % puzzles.size
                            selectedOption = -1
                            isAnswered = false
                            showHint = false
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
