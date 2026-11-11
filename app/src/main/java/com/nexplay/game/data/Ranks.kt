package com.nexplay.game.data

data class Rank(val nameKey: String, val emoji: String, val color: Long, val division: String)

/** Player rank (like battle-royale lobbies) derived from level. */
object Ranks {
    private val tiers = listOf(
        Triple(1, "rank_bronze", "🥉" to 0xFFCD7F32L),
        Triple(3, "rank_silver", "🥈" to 0xFFCFD8DCL),
        Triple(5, "rank_gold", "🥇" to 0xFFFFD54FL),
        Triple(8, "rank_platinum", "💠" to 0xFF4DD0E1L),
        Triple(11, "rank_diamond", "💎" to 0xFF9C7BFFL),
        Triple(15, "rank_hero", "👑" to 0xFFFF4081L),
        Triple(20, "rank_legend", "🔥" to 0xFFFF6D00L)
    )

    fun of(level: Int): Rank {
        var idx = 0
        for (i in tiers.indices) if (level >= tiers[i].first) idx = i
        val t = tiers[idx]
        val div = if (idx == tiers.lastIndex) "" else {
            val span = tiers[idx + 1].first - t.first
            when (3 - minOf(2, (level - t.first) * 3 / span)) { 3 -> "III"; 2 -> "II"; else -> "I" }
        }
        return Rank(t.second, t.third.first, t.third.second, div)
    }
}
