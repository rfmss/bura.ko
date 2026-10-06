package ko.bura.core

import java.security.SecureRandom
import java.util.Collections
import java.util.Random

enum class GamePhase { PLAYING, ROUND_OVER, MATCH_OVER }
enum class GameAction { DRAW_STOCK, DRAW_DISCARD, MELD, EXTEND, DISCARD, READY }
data class GameCommand(val revision: Int, val sequence: Int, val action: GameAction, val cards: List<Int> = emptyList(), val meld: Int = -1)
class MoveRejected(message: String) : IllegalArgumentException(message)

data class GameState(
    val id: String,
    val revision: Int,
    val hands: List<List<Card>>,
    val dead: List<List<Card>>,
    val stock: List<Card>,
    val discard: List<Card>,
    val melds: List<List<List<Card>>>,
    val tookDead: List<Boolean>,
    val turn: Int,
    val drew: Boolean,
    val phase: GamePhase,
    val totals: List<Int>,
    val roundScores: List<Int>,
    val finisher: Int,
    val sequences: List<Int>,
    val votes: List<Boolean>
) {
    fun view(player: Int): GameView = GameView(id, revision, player, hands[player], hands[1 - player].size, dead.size,
        stock.size, discard, melds, tookDead, turn, drew, phase, totals, roundScores, finisher, sequences[player], votes)
}

data class GameView(
    val id: String, val revision: Int, val player: Int, val hand: List<Card>, val opponentCards: Int,
    val deadCount: Int, val stockCount: Int, val discard: List<Card>, val melds: List<List<List<Card>>>,
    val tookDead: List<Boolean>, val turn: Int, val drew: Boolean, val phase: GamePhase,
    val totals: List<Int>, val roundScores: List<Int>, val finisher: Int, val sequence: Int, val votes: List<Boolean>
) { val myTurn get() = phase == GamePhase.PLAYING && player == turn }

object GameEngine {
    fun create(random: Random = SecureRandom(), id: String = "mesa-${random.nextInt(1_000_000)}"): GameState = deal(id, 0, 1, listOf(0, 0), random)
    private fun deal(id: String, revision: Int, round: Int, totals: List<Int>, random: Random): GameState {
        val d = Dealer.deal(random)
        return GameState(id, revision, d.hands, d.deadPiles, d.stock, d.discard, listOf(emptyList(), emptyList()),
            listOf(false, false), (round - 1) % 2, false, GamePhase.PLAYING, totals, listOf(0, 0), -1, listOf(0, 0), listOf(false, false))
    }
    private fun need(ok: Boolean, text: String) { if (!ok) throw MoveRejected(text) }
    fun apply(state: GameState, player: Int, command: GameCommand, random: Random = SecureRandom()): GameState {
        need(player in 0..1, "Jogador inválido.")
        if (command.sequence <= state.sequences[player]) return state
        need(command.sequence == state.sequences[player] + 1, "Jogada fora de ordem.")
        need(command.revision == state.revision, "A mesa mudou; sincronizando novamente.")
        need(state.phase == GamePhase.PLAYING || command.action == GameAction.READY, "A rodada terminou.")
        if (command.action == GameAction.READY) {
            need(state.phase != GamePhase.PLAYING && !state.votes[player], "Você já confirmou.")
            val votes = state.votes.toMutableList(); votes[player] = true
            if (!votes.all { it }) return state.copy(revision = state.revision + 1, votes = votes, sequences = state.sequences.updated(player, command.sequence))
            val nextRound = if (state.phase == GamePhase.MATCH_OVER) 1 else 2
            return deal(state.id, state.revision + 1, nextRound, if (nextRound == 1) listOf(0, 0) else state.totals, random)
        }
        need(state.turn == player, "Aguarde sua vez.")
        val hands = state.hands.map { it.toMutableList() }
        val own = hands[player]
        val dead = state.dead.toMutableList()
        val took = state.tookDead.toMutableList()
        val melds = state.melds.map { it.map { cards -> cards.toMutableList() }.toMutableList() }
        var stock = state.stock
        var discard = state.discard
        var drew = state.drew
        var turn = state.turn
        var phase = state.phase
        var finisher = -1
        fun takeDead() { need(!took[player] && dead.isNotEmpty(), "Você já pegou o morto."); own.addAll(dead.removeAt(0)); took[player] = true }
        when (command.action) {
            GameAction.DRAW_STOCK -> { need(!drew && stock.isNotEmpty(), "Não é possível comprar agora."); own.add(stock.first()); stock = stock.drop(1); drew = true }
            GameAction.DRAW_DISCARD -> { need(!drew && discard.isNotEmpty(), "Não é possível comprar agora."); own.addAll(discard); discard = emptyList(); drew = true }
            GameAction.MELD, GameAction.EXTEND -> {
                need(drew && command.cards.size in 3..13, "Compre e selecione uma sequência válida.")
                val selected = command.cards.map { id -> own.firstOrNull { it.id == id } ?: throw MoveRejected("Carta não está na mão.") }
                val combined = if (command.action == GameAction.EXTEND) {
                    need(command.meld in melds[player].indices, "Sequência inválida."); melds[player][command.meld] + selected
                } else selected
                val valid = MeldRules.evaluate(combined) ?: throw MoveRejected("Sequência inválida.")
                need(!(own.size == selected.size && !took[player]), "Guarde uma carta para o descarte.")
                own.removeAll { it.id in command.cards }
                if (command.action == GameAction.EXTEND) melds[player][command.meld] = valid.cards.toMutableList() else melds[player].add(valid.cards.toMutableList())
                if (own.isEmpty()) takeDead()
            }
            GameAction.DISCARD -> {
                need(drew && command.cards.size == 1, "Compre e selecione uma carta.")
                val card = own.firstOrNull { it.id == command.cards[0] } ?: throw MoveRejected("Carta não está na mão.")
                if (own.size == 1 && took[player]) {
                    need(melds[player].any { MeldRules.evaluate(it)?.let { m -> m.clean && m.canasta } == true }, "É preciso ter uma canastra limpa para bater.")
                    finisher = player
                }
                own.remove(card); discard = discard + card
                if (own.isEmpty() && !took[player]) takeDead()
                phase = if (finisher >= 0 || stock.isEmpty()) if (state.totals.maxOrNull() ?: 0 >= 2000) GamePhase.MATCH_OVER else GamePhase.ROUND_OVER else GamePhase.PLAYING
                if (phase == GamePhase.PLAYING) { turn = 1 - player; drew = false }
            }
            GameAction.READY -> error("handled above")
        }
        val nextSeq = state.sequences.updated(player, command.sequence)
        var next = state.copy(revision = state.revision + 1, hands = hands.map { it.toList() }, dead = dead.toList(), stock = stock,
            discard = discard, melds = melds.map { it.map { c -> c.toList() } }, tookDead = took.toList(), turn = turn, drew = drew,
            phase = phase, finisher = finisher, sequences = nextSeq)
        if (phase != GamePhase.PLAYING) {
            val scores = List(2) { p -> score(next, p, finisher) }
            val totals = List(2) { p -> state.totals[p] + scores[p] }
            val match = totals.maxOrNull() ?: 0 >= 2000 && totals[0] != totals[1]
            next = next.copy(phase = if (match) GamePhase.MATCH_OVER else GamePhase.ROUND_OVER, totals = totals, roundScores = scores, votes = listOf(false, false))
        }
        validate(next); return next
    }
    fun score(s: GameState, p: Int, finisher: Int) = s.melds[p].sumOf { it.sumOf { card -> card.points } + (MeldRules.evaluate(it)?.bonus ?: 0) } - s.hands[p].sumOf { it.points } - (if (s.tookDead[p]) 0 else 100) + (if (finisher == p) 100 else 0)
    fun validate(s: GameState) {
        require(s.hands.size == 2 && s.dead.size in 0..2 && s.melds.size == 2 && s.sequences.size == 2)
        require(s.dead.all { it.size == 11 } && s.dead.size + s.tookDead.count { it } == 2)
        require(s.melds.flatten().all { MeldRules.evaluate(it) != null })
        val cards = s.hands.flatten() + s.dead.flatten() + s.stock + s.discard + s.melds.flatten().flatten()
        require(cards.size == 104 && cards.map { it.id }.toSet().size == 104) { "Baralho inconsistente" }
    }
    private fun <T> List<T>.updated(index: Int, value: T) = toMutableList().also { it[index] = value }
}

class GameHost(initial: GameState, private val persist: (GameState) -> Unit) {
    var state = initial; private set
    fun accept(player: Int, command: GameCommand): GameState { val next = GameEngine.apply(state, player, command); if (next !== state) { persist(next); state = next }; return state }
}
