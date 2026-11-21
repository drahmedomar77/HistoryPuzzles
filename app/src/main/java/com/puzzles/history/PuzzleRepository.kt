package com.puzzles.history

object PuzzleRepository {
    // أضف هنا أي ملف ألغاز آخر عندك بنفس الطريقة: + اسمالكائن.list
    val puzzles: List<Puzzle> =
        EgyptHistoryPuzzles.list +
            AncientCivilizationsPuzzles.list +
            HistoryOfSciencePuzzles.list
}
