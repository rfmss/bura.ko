package ko.bura.core

/** Connection-only protocol. This deliberately cannot carry private game state. */
class ProbeSession(private val host: Boolean, nonce: String) {
    private val challenge = "${Wire.RULES}\n$nonce".toByteArray(Charsets.UTF_8)
    var ready = false
        private set
    var pendingPing: Int? = null
        private set
    var lastPong: Int? = null
        private set
    private var nextPing = 1
    private var lastIncomingPing = 0

    init { require(nonce.matches(Regex("[a-zA-Z0-9-]{1,64}"))) }
    fun opening(): List<Frame> = if (host) listOf(Frame(MessageType.HELLO, 0, challenge)) else emptyList()
    fun ping(): Frame {
        check(ready && pendingPing == null) { "Aguarde a conexão ou a resposta anterior" }
        check(nextPing < Int.MAX_VALUE) { "Reconecte para continuar" }
        pendingPing = nextPing++
        return Frame(MessageType.PING, pendingPing!!)
    }
    fun receive(frame: Frame): List<Frame> {
        if (!ready) {
            if (frame.sequence != 0) throw ProtocolException("Handshake inválido")
            if (host) {
                if (frame.type != MessageType.READY || !frame.payload.contentEquals(challenge))
                    throw ProtocolException("Confirmação incompatível")
                ready = true
                return emptyList()
            }
            val text = frame.payload.toString(Charsets.UTF_8)
            if (frame.type != MessageType.HELLO || !text.matches(Regex("${Wire.RULES}\n[a-zA-Z0-9-]{1,64}")))
                throw ProtocolException("Regras ou identificação incompatíveis")
            ready = true
            return listOf(Frame(MessageType.READY, 0, frame.payload.copyOf()))
        }
        if (frame.payload.isNotEmpty()) throw ProtocolException("Conteúdo inesperado")
        return when (frame.type) {
            MessageType.PING -> {
                if (frame.sequence != lastIncomingPing + 1 || frame.sequence <= 0)
                    throw ProtocolException("Sonda fora de ordem")
                lastIncomingPing = frame.sequence
                listOf(Frame(MessageType.PONG, frame.sequence))
            }
            MessageType.PONG -> {
                if (frame.sequence != pendingPing) throw ProtocolException("Resposta sem solicitação")
                lastPong = frame.sequence
                pendingPing = null
                emptyList()
            }
            else -> throw ProtocolException("Handshake repetido")
        }
    }
}
