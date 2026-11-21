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

object PuzzleData {
    val puzzles: List<Puzzle> = listOf(
        IslamicHistoryPuzzles.list,
        EgyptHistoryPuzzles.list,
        MysteriesPuzzles.list,
        AncientCivilizationsPuzzles.list,
        HistoryOfSciencePuzzles.list
    ).flatten()

    // خاصية واحدة فقط: Kotlin تولّد لها getAllPuzzles() تلقائياً،
    // لذلك حُذفت الدالة اليدوية التي كانت تسبب تعارض الأسماء.
    val allPuzzles: List<Puzzle> get() = puzzles
}
