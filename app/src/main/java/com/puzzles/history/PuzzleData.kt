package com.puzzles.history

data class Puzzle(
    val id: Int,
    val category: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val hint: String,
    val historicalFact: String
)

/** يضع الإجابة الصحيحة في الموضع [target] ويخلط بقية الخيارات عشوائياً. */
fun Puzzle.withCorrectAt(target: Int): Puzzle {
    val correct = options[correctIndex]
    val others = options.filterIndexed { i, _ -> i != correctIndex }.shuffled().toMutableList()
    val position = target.coerceIn(0, options.size - 1)
    others.add(position, correct)
    return copy(options = others, correctIndex = position)
}

/**
 * يوزّع موضع الإجابة الصحيحة بالتساوي: في كل 4 أسئلة متتالية تظهر الإجابة
 * في كل موضع مرة واحدة بترتيب عشوائي، فلا يمكن للاعب تخمين مكانها
 * ولا تتكرر في الموضع نفسه مرات كثيرة متتالية.
 */
private fun balanced(list: List<Puzzle>): List<Puzzle> {
    val result = ArrayList<Puzzle>(list.size)
    var slots = ArrayList<Int>()
    for (puzzle in list) {
        if (slots.isEmpty()) slots = ArrayList((0 until puzzle.options.size).shuffled())
        result.add(puzzle.withCorrectAt(slots.removeAt(slots.size - 1)))
    }
    return result
}

/**
 * يوزّع الأقسام بالتناوب (سؤال من كل قسم بالدور) بدل عرض قسم كامل ثم الذي بعده،
 * فيتنوّع المحتوى في وضع «الكل» ويقل الملل. الترتيب ثابت في كل تشغيل.
 */
private fun interleave(lists: List<List<Puzzle>>): List<Puzzle> {
    val result = ArrayList<Puzzle>()
    val longest = lists.maxOf { it.size }
    for (i in 0 until longest) {
        for (list in lists) {
            if (i < list.size) result.add(list[i])
        }
    }
    return result
}

object PuzzleData {
    // ترتيب الخيارات يُخلط مرة واحدة عند كل تشغيل للتطبيق، ويُوزَّع موضع الإجابة الصحيحة بتوازن داخل كل قسم
    val puzzles: List<Puzzle> = interleave(
        listOf(
            balanced(IslamicHistoryPuzzles.list),
            balanced(EgyptHistoryPuzzles.list),
            balanced(MysteriesPuzzles.list),
            balanced(AncientCivilizationsPuzzles.list),
            balanced(HistoryOfSciencePuzzles.list)
        )
    )

    // خاصية واحدة فقط: Kotlin تولّد لها getAllPuzzles() تلقائياً،
    // لذلك حُذفت الدالة اليدوية التي كانت تسبب تعارض الأسماء.
    val allPuzzles: List<Puzzle> get() = puzzles
}
