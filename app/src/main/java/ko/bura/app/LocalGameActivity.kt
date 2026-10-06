package ko.bura.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import ko.bura.core.*
import android.util.Base64
import java.util.Random

/** Complete playable two-player pass-and-play mode. The Bluetooth transport uses the same engine next. */
class LocalGameActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("local-game", MODE_PRIVATE) }
    private var state: GameState = load() ?: GameEngine.create(Random())
    private var selected = linkedSetOf<Int>()
    private lateinit var page: LinearLayout
    private var passed = true
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }
    private fun load(): GameState? = try { prefs.getString("state", null)?.let { GameCodec.readState(Base64.decode(it, Base64.DEFAULT)) } } catch (_: Exception) { null }
    private fun save() { prefs.edit().putString("state", Base64.encodeToString(GameCodec.state(state), Base64.NO_WRAP)).apply() }
    private fun render() {
        page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 14, 18, 20); setBackgroundColor(Color.rgb(16, 45, 43)) }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(page) }; setContentView(scroll)
        if (passed) renderPass() else renderTable()
    }
    private fun renderPass() {
        page.gravity = Gravity.CENTER
        page.addView(text("bura.ko", 32, 0xFFF5EFDF.toInt(), true))
        page.addView(text("Passe o celular\nsem mostrar a mão.", 25, 0xFFF5EFDF.toInt(), true))
        page.addView(text("Jogador ${state.turn + 1}, toque quando estiver pronto.", 16, 0xFFB0C5B5.toInt(), false))
        page.addView(button("Abrir minha mão", true) { passed = false; render() })
        page.addView(button("Sair da partida", false) { finish() })
    }
    private fun renderTable() {
        page.addView(text("bura.ko   ·   RODADA 1", 20, 0xFFF5EFDF.toInt(), true))
        page.addView(text("Jogador 1: ${state.totals[0]}     Jogador 2: ${state.totals[1]}", 13, 0xFFE5C584.toInt(), false))
        page.addView(text("Sua vez: jogador ${state.turn + 1}   ·   ${if (state.drew) "construa e descarte" else "compre"}", 17, 0xFFF5EFDF.toInt(), true))
        page.addView(text("Mão adversária: ${state.hands[1 - state.turn].size} cartas   ·   Monte: ${state.stock.size}   ·   Descarte: ${state.discard.lastOrNull()?.label ?: "vazio"}", 12, 0xFFB0C5B5.toInt(), false))
        if (state.melds[state.turn].isNotEmpty()) {
            page.addView(text("Suas sequências", 13, 0xFFE5C584.toInt(), true))
            state.melds[state.turn].forEachIndexed { index, meld ->
                page.addView(button("${meld.joinToString("  ") { it.label }}${if (MeldRules.evaluate(meld)?.canasta == true) "  CANASTRA" else ""}", false) {
                    if (selected.isNotEmpty()) act(GameAction.EXTEND, selected.toList(), index)
                })
            }
        }
        val hand = state.hands[state.turn]
        page.addView(text("Sua mão · toque para selecionar", 13, 0xFFE5C584.toInt(), true))
        hand.chunked(4).forEach { row ->
            val line = LinearLayout(this); row.forEach { card ->
                val tile = button(card.label, card.id in selected) { if (!selected.remove(card.id)) selected.add(card.id); render() }
                line.addView(tile, LinearLayout.LayoutParams(0, 58, 1f))
            }; page.addView(line)
        }
        val actions = LinearLayout(this)
        actions.addView(button("Comprar monte", false, !state.drew && state.stock.isNotEmpty()) { act(GameAction.DRAW_STOCK) }, LinearLayout.LayoutParams(0, 58, 1f))
        actions.addView(button("Pegar descarte", false, !state.drew && state.discard.isNotEmpty()) { act(GameAction.DRAW_DISCARD) }, LinearLayout.LayoutParams(0, 58, 1f))
        page.addView(actions)
        page.addView(button("Baixar seleção (${selected.size})", true, state.drew && selected.size >= 3) { act(GameAction.MELD, selected.toList()) })
        page.addView(button("Descartar seleção", false, state.drew && selected.size == 1) { act(GameAction.DISCARD, selected.toList()) })
        page.addView(button("Salvar e sair", false) { save(); finish() })
    }
    private fun act(action: GameAction, cards: List<Int> = emptyList(), meld: Int = -1) {
        try {
            state = GameEngine.apply(state, state.turn, GameCommand(state.revision, state.sequences[state.turn] + 1, action, cards, meld))
            save(); selected.clear(); passed = true
            if (state.phase != GamePhase.PLAYING) {
                AlertDialog.Builder(this).setTitle("Rodada encerrada").setMessage("Jogador 1: ${state.roundScores[0]}\nJogador 2: ${state.roundScores[1]}").setPositiveButton("Continuar") { _, _ -> passed = true; render() }.show()
            } else { passed = true; render() }
        } catch (error: MoveRejected) { AlertDialog.Builder(this).setTitle("Jogada inválida").setMessage(error.message).setPositiveButton("Entendi", null).show() }
    }
    private fun text(value: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply { text = value; textSize = size.toFloat(); setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD; setPadding(0, 10, 0, 10) }
    private fun button(value: String, primary: Boolean, enabled: Boolean = true, action: () -> Unit) = Button(this).apply { text = value; isAllCaps = false; textSize = 14f; isEnabled = enabled; alpha = if (enabled) 1f else .4f; setTextColor(if (primary) Color.rgb(16,45,43) else 0xFFF5EFDF.toInt()); setOnClickListener { action() } }
}
