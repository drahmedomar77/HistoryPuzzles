package com.puzzles.history

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ألوان الواجهة
val NavyTop = Color(0xFF1B1464)
val NavyBottom = Color(0xFF0B0A2A)
val Gold = Color(0xFFFFB300)
private val SoftWhite = Color(0xFFE8E9F7)
private val DangerRed = Color(0xFFFF6B6B)

/** رابط سياسة الخصوصية (صفحة privacy.html في مستودع المشروع عبر GitHub Pages) */
const val PRIVACY_URL = "https://drahmedomar77.github.io/HistoryPuzzles/privacy.html"

// =====================================================================
// عناصر مشتركة
// =====================================================================

@Composable
private fun ScreenTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 10.dp)
    )
}

@Composable
private fun StatChip(text: String, accent: Color) {
    Surface(
        color = accent.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.7f))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/** صف الإحصاءات: العملات + سلسلة الأيام + عدد الإجابات الصحيحة */
@Composable
private fun StatsRow(progress: Progress) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatChip("🪙 ${progress.coins} عملة", Gold)
        StatChip("🔥 ${progress.streak} يوم", Color(0xFFFF7043))
        StatChip("✔ ${progress.correctCount} صحيحة", Color(0xFF66BB6A))
    }
}

// =====================================================================
// الشاشة الرئيسية
// =====================================================================

private fun categoryEmoji(name: String): String = when {
    "الإسلامي" in name -> "🕌"
    "مصر" in name -> "🏺"
    "الغامضة" in name -> "🕵️"
    "الحضارات" in name -> "🏛️"
    "العلوم" in name -> "🔬"
    else -> "📜"
}

private val CARD_COLORS = listOf(
    Color(0xFF7E57C2) to Color(0xFF4A3A8A),
    Color(0xFF00897B) to Color(0xFF04505A),
    Color(0xFFEF6C00) to Color(0xFF8B4513),
    Color(0xFF1E88E5) to Color(0xFF123E7C),
    Color(0xFF43A047) to Color(0xFF1B5E20)
)

@Composable
private fun ModeCard(
    emoji: String,
    title: String,
    subtitle: String,
    from: Color,
    to: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(from, to)))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 30.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
            Text(text = "❮", color = Color.White, fontSize = 22.sp)
        }
    }
}

@Composable
fun HomeScreen(progress: Progress, onPlay: (String) -> Unit) {
    val allIndex = progress.index(Progress.ALL)
    val started = allIndex > 0 || progress.correctCount > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenTitle("🏛️ ألغاز التاريخ")
        StatsRow(progress)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "مستواك: ${progress.rankTitle()}",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        ModeCard(
            emoji = "🎯",
            title = if (started) "تابع اللعب" else "ابدأ اللعب",
            subtitle = "كل الأقسام • السؤال ${allIndex + 1} من ${progress.totalQuestions}",
            from = Color(0xFFC2185B),
            to = Color(0xFF7B1F52),
            onClick = { onPlay(Progress.ALL) }
        )

        progress.categories.forEachIndexed { i, category ->
            Spacer(modifier = Modifier.height(8.dp))
            val colors = CARD_COLORS[i % CARD_COLORS.size]
            ModeCard(
                emoji = categoryEmoji(category),
                title = category,
                subtitle = "صحيحة ${progress.correctIn(category)} من ${progress.totalIn(category)}",
                from = colors.first,
                to = colors.second,
                onClick = { onPlay(category) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// =====================================================================
// شاشة الإنجازات
// =====================================================================

@Composable
private fun AchievementItem(achievement: Achievement, current: Int) {
    val done = current >= achievement.goal
    val shown = minOf(current, achievement.goal)
    val fraction = shown.toFloat() / achievement.goal.toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (done) Gold else Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (done) Gold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (done) achievement.emoji else "🔒", fontSize = 26.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = achievement.description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = fraction,
                        color = Gold,
                        trackColor = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "$shown/${achievement.goal}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementsScreen(progress: Progress) {
    val unlocked = ACHIEVEMENTS.count { it.value(progress) >= it.goal }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenTitle("🏆 إنجازاتي")

        Card(
            colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$unlocked / ${ACHIEVEMENTS.size}",
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "إنجاز مفتوح", color = SoftWhite, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = unlocked.toFloat() / ACHIEVEMENTS.size.toFloat(),
                    color = Gold,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatChip("🔥 أفضل سلسلة ${progress.bestStreak} يوم", Color(0xFFFF7043))
                    StatChip("✔ ${progress.correctCount} صحيحة", Color(0xFF66BB6A))
                }
            }
        }

        ACHIEVEMENTS.forEach { achievement ->
            Spacer(modifier = Modifier.height(12.dp))
            AchievementItem(achievement, achievement.value(progress))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// =====================================================================
// شاشة الإعدادات
// =====================================================================

@Composable
private fun SettingRow(
    emoji: String,
    title: String,
    subtitle: String? = null,
    titleColor: Color = Color.White,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val base = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(Color.White.copy(alpha = 0.08f))
    val rowModifier = if (onClick != null) base.clickable(onClick = onClick) else base

    Row(
        modifier = rowModifier.padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 26.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(text = subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp)
            }
        }
        if (trailing != null) trailing()
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, "تعذّر فتح الرابط", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun SettingsScreen(progress: Progress) {
    val context = LocalContext.current
    var showAbout by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenTitle("⚙️ الإعدادات")
        StatsRow(progress)
        Spacer(modifier = Modifier.height(16.dp))

        SettingRow(
            emoji = "🔊",
            title = "المؤثرات الصوتية",
            subtitle = "أصوات الإجابة الصحيحة والخاطئة",
            trailing = {
                Switch(
                    checked = progress.soundOn,
                    onCheckedChange = { progress.setSound(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4A3000),
                        checkedTrackColor = Gold
                    )
                )
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        SettingRow(
            emoji = "📤",
            title = "شارك التطبيق مع أصدقائك",
            onClick = {
                val text = "جرّب تطبيق «ألغاز التاريخ» 🏛️\n" +
                    "${progress.totalQuestions} سؤالاً تاريخياً بين السهل والصعب!\n" +
                    "https://play.google.com/store/apps/details?id=${context.packageName}"
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(send, "مشاركة التطبيق"))
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        SettingRow(
            emoji = "ℹ️",
            title = "عن التطبيق والخصوصية",
            onClick = { showAbout = true }
        )
        Spacer(modifier = Modifier.height(12.dp))

        SettingRow(
            emoji = "🗑️",
            title = "إعادة ضبط التقدم",
            titleColor = DangerRed,
            onClick = { showReset = true }
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "للتسلية والتعلّم وليس مرجعاً تاريخياً أكاديمياً.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showAbout) {
        val version = remember {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
            } catch (e: Exception) {
                ""
            }
        }
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("ألغاز التاريخ") },
            text = {
                Text(
                    "${progress.totalQuestions} سؤالاً في ${progress.categories.size} أقسام " +
                        "تاريخية، من السهل إلى الصعب.\n\n" +
                        "التطبيق يعرض إعلانات من Google AdMob، ويحفظ تقدمك على جهازك فقط.\n\n" +
                        if (version.isNotEmpty()) "الإصدار $version" else ""
                )
            },
            confirmButton = {
                TextButton(onClick = { openUrl(context, PRIVACY_URL) }) {
                    Text("سياسة الخصوصية")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbout = false }) { Text("إغلاق") }
            }
        )
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("إعادة ضبط التقدم؟") },
            text = {
                Text(
                    "سيتم حذف العملات والأسئلة المحلولة والإنجازات وسلسلة الأيام، " +
                        "وتبدأ من جديد. لا يمكن التراجع عن هذا الإجراء."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    progress.resetAll()
                    showReset = false
                    Toast.makeText(context, "تمت إعادة ضبط التقدم", Toast.LENGTH_SHORT).show()
                }) {
                    Text("إعادة الضبط", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("إلغاء") }
            }
        )
    }
}
