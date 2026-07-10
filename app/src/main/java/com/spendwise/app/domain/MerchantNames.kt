package com.spendwise.app.domain

import java.util.Locale
import kotlin.math.min

object MerchantNames {
    fun clean(input: String): String =
        input.trim().replace(Regex("\\s+"), " ")

    fun canonicalize(input: String, existing: List<String>): String =
        canonicalizeOrNull(input, existing) ?: clean(input)

    fun canonicalizeOrNull(input: String, existing: List<String>): String? {
        val cleaned = clean(input)
        if (cleaned.isBlank()) return null

        val queryKey = matchKey(cleaned)
        val candidates = uniqueExisting(existing)
        candidates.firstOrNull { matchKey(it) == queryKey }?.let { return it }

        return candidates
            .asSequence()
            .mapNotNull { candidate ->
                val candidateKey = matchKey(candidate)
                val limit = typoLimit(queryKey.length)
                if (limit == 0) return@mapNotNull null
                val distance = boundedDistance(queryKey, candidateKey, limit)
                if (distance <= limit) candidate to distance else null
            }
            .sortedWith(compareBy<Pair<String, Int>> { it.second }.thenBy { it.first.length })
            .firstOrNull()
            ?.first
    }

    fun suggest(query: String, existing: List<String>, limit: Int = 6): List<String> {
        val cleaned = clean(query)
        val candidates = uniqueExisting(existing)
        if (cleaned.isBlank()) return candidates.take(limit)

        val queryKey = matchKey(cleaned)
        return candidates
            .asSequence()
            .mapIndexedNotNull { index, candidate ->
                val candidateKey = matchKey(candidate)
                val candidateLower = candidate.lowercase(Locale.ROOT)
                val score = when {
                    candidateKey.startsWith(queryKey) -> 0
                    candidateLower.contains(cleaned.lowercase(Locale.ROOT)) -> 1
                    candidateKey.contains(queryKey) -> 2
                    typoLimit(queryKey.length) > 0 &&
                        boundedDistance(queryKey, candidateKey, typoLimit(queryKey.length)) <= typoLimit(queryKey.length) -> 3
                    else -> null
                }
                score?.let { MerchantSuggestion(candidate, it, index) }
            }
            .sortedWith(compareBy<MerchantSuggestion> { it.score }.thenBy { it.index })
            .map { it.name }
            .take(limit)
            .toList()
    }

    private fun uniqueExisting(existing: List<String>): List<String> {
        val seen = linkedSetOf<String>()
        return existing.map(::clean)
            .filter { it.isNotBlank() }
            .filter { seen.add(matchKey(it)) }
    }

    private fun matchKey(value: String): String =
        clean(value)
            .lowercase(Locale.ROOT)
            .filter { it.isLetterOrDigit() }

    private fun typoLimit(length: Int): Int = when {
        length < 4 -> 0
        length <= 7 -> 1
        else -> 2
    }

    private fun boundedDistance(a: String, b: String, limit: Int): Int {
        if (a == b) return 0
        if (kotlin.math.abs(a.length - b.length) > limit) return limit + 1

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            var rowMin = current[0]
            for (j in 1..b.length) {
                val substitutionCost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = min(
                    min(current[j - 1] + 1, previous[j] + 1),
                    previous[j - 1] + substitutionCost
                )
                rowMin = min(rowMin, current[j])
            }
            if (rowMin > limit) return limit + 1
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }

    private data class MerchantSuggestion(
        val name: String,
        val score: Int,
        val index: Int
    )
}
