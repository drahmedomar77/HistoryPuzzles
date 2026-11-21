package com.puzzles.history

object PuzzleRepository {
    // المصدر الوحيد للألغاز هو PuzzleData (يجمع الأقسام الخمسة)
    val puzzles: List<Puzzle> = PuzzleData.puzzles
}
