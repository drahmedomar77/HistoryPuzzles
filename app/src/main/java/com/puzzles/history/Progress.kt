package com.puzzles.history

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.TimeZone

/**
 * مصدر واحد لكل بيانات تقدم اللاعب (العملات، الأسئلة المحلولة، سلسلة الأيام، الصوت...).
 * الحقول التي تُقرأ في الشاشات قابلة للملاحظة، فتتحدث الواجهة تلقائياً عند تغيّرها.
 */
class Progress(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------- بيانات الأقسام (تُحسب مرة واحدة) ----------
    val categories: List<String> = PuzzleRepository.puzzles.map { it.category }.distinct()
    val totalQuestions: Int = PuzzleRepository.puzzles.size
    private val categoryOfId: Map<Int, String> =
        PuzzleRepository.puzzles.associate { it.id to it.category }
    private val totalByCategory: Map<String, Int> =
        PuzzleRepository.puzzles.groupingBy { it.category }.eachCount()

    // ---------- الحالة القابلة للملاحظة ----------
    var coins by mutableIntStateOf(prefs.getInt(KEY_COINS, START_COINS))
        private set

    var totalEarned by mutableIntStateOf(prefs.getInt(KEY_EARNED, 0))
        private set

    var streak by mutableIntStateOf(1)
        private set

    var bestStreak by mutableIntStateOf(prefs.getInt(KEY_BEST_STREAK, 1))
        private set

    var soundOn by mutableStateOf(prefs.getBoolean(KEY_SOUND, true))
        private set

    var correctIds by mutableStateOf(loadCorrectIds())
        private set

    var selectedCategory by mutableStateOf(prefs.getString(KEY_CATEGORY, ALL) ?: ALL)
        private set

    /** عدّاد غير محفوظ لتنظيم ظهور الإعلان البيني */
    var questionsSinceAd: Int = 0

    init {
        touchStreak()
    }

    // ---------- إحصاءات ----------
    /** عدد الأسئلة التي أُجيبت إجابة صحيحة (بدون تكرار) */
    val correctCount: Int
        get() = correctIds.size

    fun totalIn(category: String): Int = totalByCategory[category] ?: 0

    fun correctIn(category: String): Int = correctIds.count { categoryOfId[it] == category }

    fun maxCategoryCorrect(): Int = categories.maxOfOrNull { correctIn(it) } ?: 0

    fun minCategoryCorrect(): Int = categories.minOfOrNull { correctIn(it) } ?: 0

    fun rankTitle(): String {
        val n = correctCount
        return when {
            n < 10 -> "مبتدئ 🌱"
            n < 50 -> "طالب تاريخ 📘"
            n < 150 -> "باحث 🔎"
            n < 300 -> "مؤرخ 📜"
            n < 450 -> "خبير 🏛️"
            else -> "عبقري التاريخ 👑"
        }
    }

    // ---------- العملات ----------
    fun addCoins(amount: Int) {
        coins += amount
        if (amount > 0) totalEarned += amount
        prefs.edit().putInt(KEY_COINS, coins).putInt(KEY_EARNED, totalEarned).apply()
    }

    fun spendCoins(amount: Int): Boolean {
        if (coins < amount) return false
        coins -= amount
        prefs.edit().putInt(KEY_COINS, coins).apply()
        return true
    }

    /**
     * يسجّل إجابة صحيحة. المكافأة (20 عملة) تُمنح أول مرة فقط لكل سؤال،
     * حتى لا يمكن تكرار الإجابة عن السؤال نفسه للحصول على عملات.
     * @return true إذا كانت أول إجابة صحيحة عن هذا السؤال
     */
    fun recordCorrect(puzzleId: Int): Boolean {
        if (puzzleId in correctIds) return false
        val updated = correctIds + puzzleId
        correctIds = updated
        prefs.edit()
            .putStringSet(KEY_CORRECT_IDS, updated.map { it.toString() }.toSet())
            .apply()
        addCoins(CORRECT_REWARD)
        return true
    }

    // ---------- التقدم داخل الأقسام ----------
    fun index(category: String): Int = prefs.getInt("index_$category", 0)

    fun setIndex(category: String, value: Int) {
        prefs.edit().putInt("index_$category", value).apply()
    }

    fun selectCategory(category: String) {
        selectedCategory = category
        prefs.edit().putString(KEY_CATEGORY, category).apply()
    }

    // ---------- الصوت ----------
    fun setSound(enabled: Boolean) {
        soundOn = enabled
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    // ---------- إعادة الضبط ----------
    /** يمسح كل التقدم ويُبقي إعداد الصوت كما هو */
    fun resetAll() {
        val sound = soundOn
        prefs.edit().clear().putBoolean(KEY_SOUND, sound).apply()
        coins = START_COINS
        totalEarned = 0
        bestStreak = 1
        correctIds = emptySet()
        selectedCategory = ALL
        questionsSinceAd = 0
        touchStreak()
    }

    // ---------- داخلي ----------
    private fun loadCorrectIds(): Set<Int> =
        prefs.getStringSet(KEY_CORRECT_IDS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()

    private fun today(): Long {
        val now = System.currentTimeMillis()
        return (now + TimeZone.getDefault().getOffset(now)) / DAY_MS
    }

    /** يحدّث سلسلة الأيام المتتالية عند فتح التطبيق */
    private fun touchStreak() {
        val today = today()
        val last = prefs.getLong(KEY_LAST_DAY, -1L)
        val saved = prefs.getInt(KEY_STREAK, 0)
        val current = when {
            last == today -> if (saved > 0) saved else 1
            last == today - 1 -> saved + 1
            else -> 1
        }
        streak = current
        if (current > bestStreak) bestStreak = current
        prefs.edit()
            .putLong(KEY_LAST_DAY, today)
            .putInt(KEY_STREAK, current)
            .putInt(KEY_BEST_STREAK, bestStreak)
            .apply()
    }

    companion object {
        const val ALL = "الكل"
        const val CORRECT_REWARD = 20
        private const val START_COINS = 50
        private const val DAY_MS = 86_400_000L

        private const val PREFS_NAME = "history_puzzles_prefs"
        private const val KEY_COINS = "user_coins"
        private const val KEY_EARNED = "coins_earned_total"
        private const val KEY_STREAK = "streak"
        private const val KEY_BEST_STREAK = "best_streak"
        private const val KEY_LAST_DAY = "last_day"
        private const val KEY_SOUND = "sound_on"
        private const val KEY_CATEGORY = "selected_category"
        private const val KEY_CORRECT_IDS = "correct_ids"
    }
}

/** إنجاز: له هدف رقمي، وقيمة حالية تُحسب من تقدم اللاعب */
class Achievement(
    val emoji: String,
    val title: String,
    val description: String,
    val goal: Int,
    val value: (Progress) -> Int
)

val ACHIEVEMENTS: List<Achievement> = listOf(
    Achievement("🎯", "الخطوة الأولى", "أجب إجابة صحيحة واحدة", 1) { it.correctCount },
    Achievement("📘", "طالب مجتهد", "أجب 25 إجابة صحيحة", 25) { it.correctCount },
    Achievement("📜", "مؤرخ", "أجب 100 إجابة صحيحة", 100) { it.correctCount },
    Achievement("🏛️", "خبير التاريخ", "أجب 250 إجابة صحيحة", 250) { it.correctCount },
    Achievement("👑", "موسوعة متحركة", "أجب 500 إجابة صحيحة", 500) { it.correctCount },
    Achievement("🔥", "متحمّس دائم", "العب 3 أيام متتالية", 3) { it.bestStreak },
    Achievement("📅", "أسبوع كامل", "العب 7 أيام متتالية", 7) { it.bestStreak },
    Achievement("🪙", "ثروة عملات", "اكسب 500 عملة", 500) { it.totalEarned },
    Achievement("🎓", "متخصص", "أجب 50 سؤالاً صحيحاً في قسم واحد", 50) { it.maxCategoryCorrect() },
    Achievement("🧭", "رحّالة الأقسام", "أجب 10 أسئلة صحيحة على الأقل في كل قسم", 10) { it.minCategoryCorrect() }
)
