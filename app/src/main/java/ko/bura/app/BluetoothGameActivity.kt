package ko.bura.app

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import ko.bura.core.*
import java.util.Random

/** One authoritative host; guest receives a filtered GameView and sends only commands. */
class BluetoothGameActivity : Activity() {
    private val adapter get() = getSystemService(BluetoothManager::class.java)?.adapter
    private var link: GameBluetoothLink? = null
    private var host = false
    private var ready = false
    private var selected = linkedSetOf<Int>()
    private var state: GameState? = null
    private var view: GameView? = null
    private var sequence = 0
    private lateinit var page: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); host = intent.getBooleanExtra("host", false)
        val address = intent.getStringExtra("peer") ?: return finish()
        val bluetooth = adapter ?: return finish()
        if (host) state = GameEngine.create(Random())
        link = GameBluetoothLink(bluetooth, bluetooth.getRemoteDevice(address), host, { frame -> runOnUiThread { receive(frame) } },
            { runOnUiThread { if (host) { link!!.send(Frame(MessageType.HELLO, 0, "bura-game-v1".toByteArray())); status("Aguardando a outra pessoa…") } else { link!!.send(Frame(MessageType.READY, 0, "bura-game-v1".toByteArray())); status("Mesa encontrada…") } } },
            { runOnUiThread { status("Conexão perdida. A rodada não foi encerrada; reconecte depois.") } })
        link!!.start(); render("Conectando aos celulares…")
    }
    private fun receive(frame: Frame) {
        try {
            when (frame.type) {
                MessageType.READY -> { require(host && frame.payload.contentEquals("bura-game-v1".toByteArray())); ready = true; sendState() }
                MessageType.HELLO -> { require(!host && frame.payload.contentEquals("bura-game-v1".toByteArray())); ready = true }
                MessageType.COMMAND -> { require(host && ready); val s = state!!; state = GameEngine.apply(s, 1, GameCodec.readCommand(frame.payload)); sendState() }
                MessageType.STATE -> { require(!host); view = GameCodec.readView(frame.payload); sequence = view!!.sequence; render("Mesa sincronizada") }
                MessageType.ERROR -> status(frame.payload.toString(Charsets.UTF_8))
                else -> Unit
            }
            if (host && ready) render("Sua vez: jogador ${state!!.turn + 1}")
        } catch (error: Exception) { status("Mensagem rejeitada: ${error.message ?: "protocolo incompatível"}") }
    }
    private fun sendState() { val s = state ?: return; link?.send(Frame(MessageType.STATE, s.revision, GameCodec.view(s.view(1)))); render("Sua vez: jogador ${s.turn + 1}") }
    private fun act(action: GameAction, cards: List<Int> = emptyList(), meld: Int = -1) {
        try {
            if (host) { state = GameEngine.apply(state!!, 0, GameCommand(state!!.revision, state!!.sequences[0] + 1, action, cards, meld)); sendState() }
            else { val v = view ?: return; link?.send(Frame(MessageType.COMMAND, v.revision, GameCodec.command(GameCommand(v.revision, v.sequence + 1, action, cards, meld)))); status("Confirmando jogada…") }
            selected.clear(); render("Jogada salva")
        } catch (error: MoveRejected) { AlertDialog.Builder(this).setTitle("Jogada inválida").setMessage(error.message).setPositiveButton("Entendi", null).show() }
    }
    private fun render(message: String) {
        val v = if (host) state?.view(0) else view
        page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 14, 18, 18); setBackgroundColor(Color.rgb(16,45,43)) }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(page) }; setContentView(scroll)
        page.addView(text("bura.ko · Bluetooth", 21, 0xFFF5EFDF.toInt(), true)); page.addView(text(message, 13, 0xFFE5C584.toInt(), false))
        if (v == null) { page.addView(text("A mesa será distribuída quando a outra pessoa aceitar.", 18, 0xFFB0C5B5.toInt(), false)); return }
        page.addView(text("Você: ${v.totals[v.player]}   ·   Companhia: ${v.totals[1-v.player]}   ·   Monte: ${v.stockCount}", 14, 0xFFE5C584.toInt(), false))
        page.addView(text("${if (v.myTurn) "Sua vez" else "Aguarde"}   ·   Descarte: ${v.discard.lastOrNull()?.label ?: "vazio"}", 18, 0xFFF5EFDF.toInt(), true))
        v.melds[v.player].forEachIndexed { index, meld -> page.addView(button(meld.joinToString("  ") { it.label }, false) { if (selected.isNotEmpty() && v.myTurn && v.drew) act(GameAction.EXTEND, selected.toList(), index) }) }
        val hand = LinearLayout(this); v.hand.forEach { card -> hand.addView(button(card.label, card.id in selected) { if (!selected.remove(card.id)) selected.add(card.id); render("Selecione cartas") }, LinearLayout.LayoutParams(0,58,1f)) }; page.addView(hand)
        val buy = LinearLayout(this); buy.addView(button("Comprar monte", false, v.myTurn && !v.drew && v.stockCount > 0) { act(GameAction.DRAW_STOCK) }, LinearLayout.LayoutParams(0,58,1f)); buy.addView(button("Pegar descarte", false, v.myTurn && !v.drew && v.discard.isNotEmpty()) { act(GameAction.DRAW_DISCARD) }, LinearLayout.LayoutParams(0,58,1f)); page.addView(buy)
        page.addView(button("Baixar ${selected.size}", true, v.myTurn && v.drew && selected.size >= 3) { act(GameAction.MELD, selected.toList()) })
        page.addView(button("Descartar", false, v.myTurn && v.drew && selected.size == 1) { act(GameAction.DISCARD, selected.toList()) })
    }
    private fun status(value: String) { if (::page.isInitialized) page.addView(text(value, 14, 0xFFE5C584.toInt(), false)) }
    private fun text(value: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply { text = value; textSize = size.toFloat(); setTextColor(color); if (bold) setTypeface(typeface, 1); setPadding(0, 10, 0, 10) }
    private fun button(value: String, primary: Boolean, enabled: Boolean = true, action: () -> Unit) = Button(this).apply { text = value; isAllCaps = false; isEnabled = enabled; alpha = if (enabled) 1f else .4f; setTextColor(if (primary) Color.rgb(16,45,43) else 0xFFF5EFDF.toInt()); setOnClickListener { action() } }
    override fun onDestroy() { link?.close(); super.onDestroy() }
}
