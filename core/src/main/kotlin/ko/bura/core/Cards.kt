package ko.bura.core

import java.security.SecureRandom
import java.util.Collections
import java.util.Random

enum class Suit(val glyph: String) { CLUBS("♣"), DIAMONDS("♦"), HEARTS("♥"), SPADES("♠") }

/** Physical identity distinguishes the two copies of every card. No printed jokers. */
data class Card(val id: Int) {
    init { require(id in 0..103) { "Carta fora do baralho" } }
    val rank: Int get() = id % 13 + 1
    val suit: Suit get() = Suit.entries[(id % 52) / 13]
    val points: Int get() = when (rank) { 1 -> 15; 2 -> 20; in 3..7 -> 5; else -> 10 }
    val label: String get() = (when (rank) { 1 -> "A"; 11 -> "J"; 12 -> "Q"; 13 -> "K"; else -> rank.toString() }) + suit.glyph
}

data class Deal(val hands: List<List<Card>>, val deadPiles: List<List<Card>>, val stock: List<Card>, val discard: List<Card>)

object Dealer {
    /** Inject Random only in tests. Production must not reveal the random seed. */
    fun deal(random: Random = SecureRandom()): Deal {
        val deck = (0..103).map(::Card).toMutableList()
        Collections.shuffle(deck, random)
        val hands = List(2) { player -> (0..10).map { deck[it * 2 + player] } }
        return Deal(hands, listOf(deck.subList(22, 33).toList(), deck.subList(33, 44).toList()),
            deck.subList(45, 104).toList(), listOf(deck[44]))
    }
}

data class Meld(val cards: List<Card>, val clean: Boolean) {
    val canasta: Boolean get() = cards.size >= 7
    val bonus: Int get() = if (!canasta) 0 else if (clean) 200 else 100
}

/** bura-open-v1: same-suit runs, one wild two, ace low OR high, no wrapping. */
object MeldRules {
    fun evaluate(cards: List<Card>): Meld? {
        if (cards.size !in 3..13 || cards.map { it.id }.toSet().size != cards.size) return null
        // Prefer the natural role of a two so a clean run stays clean.
        val candidates = listOf<Int?>(null) + cards.indices.filter { cards[it].rank == 2 }
        for (wild in candidates) {
            val natural = cards.filterIndexed { index, _ -> index != wild }
            if (natural.map { it.suit }.toSet().size != 1) continue
            for (aceHigh in listOf(false, true)) {
                val ranks = natural.map { if (it.rank == 1 && aceHigh) 14 else it.rank }
                if (ranks.toSet().size != ranks.size) continue
                for (start in 1..(15 - cards.size)) {
                    val run = start until start + cards.size
                    if (ranks.all { it in run } && run.count { it !in ranks } == if (wild == null) 0 else 1) {
                        return Meld(cards.toList(), wild == null)
                    }
                }
            }
        }
        return null
    }
}
