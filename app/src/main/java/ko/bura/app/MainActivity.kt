package ko.bura.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import ko.bura.core.Card
import ko.bura.core.Dealer
import ko.bura.core.Suit

class MainActivity : Activity() {
    private val ink = Color.rgb(16, 45, 43)
    private val cream = Color.rgb(246, 238, 221)
    private val gold = Color.rgb(234, 205, 138)
    private val muted = Color.rgb(164, 191, 178)
    private lateinit var status: TextView
    private lateinit var ping: Button
    private lateinit var hand: LinearLayout
    private var probe: BluetoothProbe? = null
    private var pendingHost: Boolean? = null
    private var pendingGameRole: Boolean? = null
    private val adapter: BluetoothAdapter? get() = getSystemService(BluetoothManager::class.java)?.adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(18), dp(24), dp(32))
            setBackgroundColor(ink)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(page) }
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets
        }
        setContentView(scroll)
        page.addView(label("FEITO PARA ESTAR PERTO", 11, gold).apply { letterSpacing = 0.15f })
        page.addView(label("bura.ko", 52, cream).apply { typeface = Typeface.create("serif", Typeface.BOLD) })
        page.addView(label("Uma boa mão. Uma boa companhia.", 16, muted))
        gap(page, 24)
        page.addView(label("PRIMEIRA MESA · PROTÓTIPO 0.1", 11, gold))
        gap(page, 12)
        val table = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(20))
            background = surface(Color.rgb(24, 65, 57), Color.rgb(77, 104, 79), 18)
        }
        table.addView(label("O encontro começa aqui", 22, cream).apply { typeface = Typeface.create("serif", Typeface.BOLD) })
        gap(table, 8)
        table.addView(label("Prévia local das cartas • ainda não é uma partida", 13, muted))
        gap(table, 18)
        hand = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        table.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = true
            addView(hand)
        })
        gap(table, 12)
        table.addView(label("Deslize para ver a mão inteira.", 12, muted))
        page.addView(table)
        showHand()
        gap(page, 24)
        page.addView(label("Dois celulares, nenhuma internet", 21, cream).apply { setTypeface(typeface, Typeface.BOLD) })
        gap(page, 8)
        page.addView(label("Instale nos dois Androids e pareie-os nas configurações. Volte aqui: um recebe, o outro entra. Esta versão testa a conexão; a partida completa vem na próxima etapa.", 15, muted))
        gap(page, 16)
        page.addView(action("Jogar no mesmo celular", true) {
            startActivity(Intent(this, LocalGameActivity::class.java))
        })
        page.addView(action("Jogar via Bluetooth", false) { chooseGameRole() })
        page.addView(label("Partida local e partida Bluetooth usam o mesmo motor e as mesmas regras.", 12, muted))
        gap(page, 8)
        page.addView(action("1. Parear celulares", false) {
            try { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
            catch (_: android.content.ActivityNotFoundException) { status.text = "Abra Bluetooth nas configurações do celular." }
        })
        page.addView(action("2. Receber conexão", true) { choosePeer(true) })
        page.addView(action("2. Entrar na conexão", false) { choosePeer(false) })
        gap(page, 12)
        status = label("Aguardando sua escolha.", 14, gold).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            setPadding(0, dp(8), 0, dp(8))
        }
        page.addView(status)
        ping = action("Testar ida e volta", true) { probe?.ping() }.apply { isEnabled = false; alpha = 0.5f }
        page.addView(ping)
        page.addView(action("Encerrar conexão", false) {
            stopProbe()
            status.text = "Conexão encerrada. Você pode conectar novamente."
        })
        gap(page, 16)
        page.addView(action("Conhecer as regras propostas", false) {
            AlertDialog.Builder(this).setTitle("Buraco aberto · bura-open-v1")
                .setMessage("Duas pessoas, dois baralhos sem curingas impressos. Cada jogador recebe 11 cartas; há dois mortos de 11.\n\nSequências do mesmo naipe, mínimo de 3 cartas; sem trincas. O 2 pode ser natural ou substituir uma carta, com no máximo um substituto por sequência. Ás baixo ou alto, sem volta K–A–2.\n\nCanastra: 7 ou mais cartas. Limpa vale 200; suja, 100. Para bater: pegar o morto, ter canastra limpa e terminar com descarte.\n\nEsta versão apenas exibe cartas e testa Bluetooth. Regulamento completo e pendências estão no repositório.")
                .setPositiveButton("Entendi", null).show()
        })
        page.addView(label("SEM CONTA · SEM NUVEM · SEM PRESSA", 10, muted).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, 0)
        })
    }

    private fun choosePeer(host: Boolean) {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            pendingHost = host
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
            return
        }
        val bluetooth = adapter
        if (bluetooth == null) { status.text = "Este aparelho não oferece Bluetooth compatível."; return }
        try {
            if (!bluetooth.isEnabled) {
                status.text = "Ative Bluetooth nas configurações e tente novamente."
                return
            }
            val devices = bluetooth.bondedDevices.sortedBy { it.name ?: it.address }
            if (devices.isEmpty()) { status.text = "Primeiro pareie os dois celulares nas configurações."; return }
            val labels = devices.map { "${it.name ?: "Celular"}\n${it.address}" }.toTypedArray()
            AlertDialog.Builder(this).setTitle("Escolha o outro celular")
                .setItems(labels) { _, index ->
                    stopProbe()
                    probe = BluetoothProbe(bluetooth) { message, ready, latency ->
                        status.text = if (latency == null) message else "$message\nIda e volta: $latency ms"
                        ping.isEnabled = ready
                        ping.alpha = if (ready) 1f else 0.5f
                    }.also { it.open(devices[index], host) }
                }.setNegativeButton("Cancelar", null).show()
        } catch (_: SecurityException) {
            status.text = "Permita acesso a dispositivos próximos para conectar."
    }
    private fun chooseGameRole() {
        AlertDialog.Builder(this).setTitle("Qual celular cria a mesa?")
            .setItems(arrayOf("Criar mesa (anfitrião)", "Entrar na mesa")) { _, which -> chooseGamePeer(which == 0) }
            .setNegativeButton("Cancelar", null).show()
    }
    private fun chooseGamePeer(host: Boolean) {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            pendingGameRole = host; requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1); return
        }
        try {
            val bluetooth = adapter
            if (bluetooth == null || !bluetooth.isEnabled) { status.text = "Ative o Bluetooth nas configurações."; return }
            val peers = bluetooth.bondedDevices.sortedBy { it.name ?: it.address }
            if (peers.isEmpty()) { status.text = "Pareie os celulares primeiro."; return }
            AlertDialog.Builder(this).setTitle("Escolha o outro celular")
                .setItems(peers.map { "${it.name ?: "Celular"}\n${it.address}" }.toTypedArray()) { _, index ->
                    startActivity(Intent(this, BluetoothGameActivity::class.java).putExtra("host", host).putExtra("peer", peers[index].address))
                }.setNegativeButton("Cancelar", null).show()
        } catch (_: SecurityException) { status.text = "Conceda acesso a dispositivos próximos." }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val host = pendingHost
        val game = pendingGameRole
        pendingHost = null
        pendingGameRole = null
        if (requestCode == 1 && host != null) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) choosePeer(host)
            else status.text = "Permissão negada. Você pode concedê-la nas configurações do aplicativo."
        } else if (requestCode == 1 && game != null && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) chooseGamePeer(game)
        }
    }
    override fun onStop() {
        super.onStop()
        stopProbe()
        status.text = "Conexão pausada ao sair. Escolha receber ou entrar novamente."
    }
    private fun stopProbe() {
        probe?.close()
        probe = null
        if (::ping.isInitialized) { ping.isEnabled = false; ping.alpha = 0.5f }
    }
    private fun showHand() {
        val cards = Dealer.deal().hands[0].sortedWith(compareBy<Card> { it.suit.ordinal }.thenBy { it.rank })
        val cardScale = resources.configuration.fontScale.coerceAtLeast(1f)
        cards.forEach { card ->
            hand.addView(label(card.label, 24, if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) Color.rgb(160, 54, 63) else ink).apply {
                gravity = Gravity.CENTER
                typeface = Typeface.create("serif", Typeface.BOLD)
                background = surface(cream, Color.rgb(209, 195, 159), 8)
                elevation = dp(2).toFloat()
                contentDescription = "${when (card.rank) { 1 -> "Ás"; 11 -> "Valete"; 12 -> "Dama"; 13 -> "Rei"; else -> card.rank.toString() }} de ${when(card.suit) { Suit.CLUBS -> "paus"; Suit.DIAMONDS -> "ouros"; Suit.HEARTS -> "copas"; Suit.SPADES -> "espadas" }}"
                layoutParams = LinearLayout.LayoutParams(dp((62 * cardScale).toInt()), dp((88 * cardScale).toInt())).apply { marginEnd = dp(8); bottomMargin = dp(4) }
            })
        }
    }
    private fun label(value: String, size: Int, color: Int) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color)
        setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun action(value: String, primary: Boolean, clicked: () -> Unit) = Button(this).apply {
        text = value
        isAllCaps = false
        textSize = 15f
        setTextColor(if (primary) ink else cream)
        background = RippleDrawable(ColorStateList.valueOf(Color.argb(40, 255, 255, 255)),
            surface(if (primary) gold else Color.rgb(24, 60, 55), if (primary) gold else Color.rgb(68, 103, 91), 12), null)
        minHeight = dp(52)
        setPadding(dp(12), dp(12), dp(12), dp(12))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
        setOnClickListener { clicked() }
    }
    private fun surface(fill: Int, stroke: Int, radius: Int) = GradientDrawable().apply {
        setColor(fill); setStroke(dp(1), stroke); cornerRadius = dp(radius).toFloat()
    }
    private fun gap(parent: LinearLayout, height: Int) { parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height))) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
