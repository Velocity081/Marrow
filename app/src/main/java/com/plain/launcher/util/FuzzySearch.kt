package com.plain.launcher.util

import com.plain.launcher.data.model.AppItem
import java.util.Locale

object FuzzySearch {

    data class MatchResult(
        val item: AppItem,
        val score: Int
    )

    fun filter(items: List<AppItem>, query: String): List<AppItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return items

        val q = trimmed.lowercase(Locale.getDefault())

        val results = mutableListOf<MatchResult>()
        for (item in items) {
            val scoreCustom = scoreMatch(item.displayLabel.lowercase(Locale.getDefault()), q)
            val scoreOriginal = if (!item.customLabel.isNullOrBlank()) {
                scoreMatch(item.originalLabel.lowercase(Locale.getDefault()), q)
            } else 0

            val bestScore = maxOf(scoreCustom, scoreOriginal)
            if (bestScore > 0) {
                results.add(MatchResult(item, bestScore))
            }
        }

        return results
            .sortedWith(compareByDescending<MatchResult> { it.score }
                .thenBy { it.item.displayLabel.lowercase(Locale.getDefault()) })
            .map { it.item }
    }

    fun scoreMatch(target: String, query: String): Int {
        if (query.isEmpty()) return 100
        if (target.isEmpty()) return 0

        // 1. Exact match
        if (target == query) return 1000

        // 2. Exact prefix match
        if (target.startsWith(query)) {
            return 800 - (target.length - query.length)
        }

        // 3. Word boundary prefix match (e.g. "gm" for "google maps")
        val words = target.split(" ", "-", "_", ".")
        for (word in words) {
            if (word.startsWith(query)) {
                return 600 - (word.length - query.length)
            }
        }

        // Word initials match (acronym)
        if (words.size > 1 && query.length <= words.size) {
            val initials = words.mapNotNull { it.firstOrNull() }.joinToString("")
            if (initials.startsWith(query)) {
                return 500
            }
        }

        // 4. Substring match
        val subIndex = target.indexOf(query)
        if (subIndex >= 0) {
            return 400 - subIndex
        }

        // 5. Fuzzy subsequence match
        var tIndex = 0
        var qIndex = 0
        var consecutiveBonus = 0
        var totalScore = 0

        while (tIndex < target.length && qIndex < query.length) {
            if (target[tIndex] == query[qIndex]) {
                totalScore += 20 + consecutiveBonus
                consecutiveBonus += 5
                qIndex++
            } else {
                consecutiveBonus = 0
            }
            tIndex++
        }

        return if (qIndex == query.length) {
            // Matched all query characters in sequence
            100 + totalScore - (target.length - query.length)
        } else {
            0
        }
    }
}
