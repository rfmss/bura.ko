package ko.bura.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.FilterInputStream
import java.util.Random

private var checks = 0
private fun verify(name: String, body: () -> Unit) { body(); checks++; println("PASS $name") }
private inline fun <reified T : Throwable> rejects(body: () -> Unit) {
    try { body() } catch (error: Throwable) { check(error is T) { "Expected ${T::class}, got $error" }; return }
    error("Expected ${T::class}")
}
private fun c(rank: Int, suit: Int = 0, copy: Int = 0) = Card(copy * 52 + suit * 13 + rank - 1)
private fun bytes(frame: Frame) = ByteArrayOutputStream().also { Wire.write(it, frame) }.toByteArray()
private fun connected(): Pair<ProbeSession, ProbeSession> {
    val host = ProbeSession(true, "host-123")
    val guest = ProbeSession(false, "guest-456")
    host.receive(guest.receive(host.opening().single()).single())
    check(host.ready && guest.ready)
    return host to guest
}
fun main() {
    verify("physical cards are unique and values are bounded") {
        check((0..103).map(::Card).toSet().size == 104)
        check(c(1).points == 15 && c(2).points == 20 && c(7).points == 5 && c(13).points == 10)
        rejects<IllegalArgumentException> { Card(104) }
        rejects<IllegalArgumentException> { Card(-1) }
    }
    verify("1000 shuffled deals conserve all 104 cards") {
        repeat(1000) { seed ->
            val d = Dealer.deal(Random(seed.toLong()))
            check(d.hands.map { it.size } == listOf(11, 11))
            check(d.deadPiles.map { it.size } == listOf(11, 11))
            check(d.stock.size == 59 && d.discard.size == 1)
            val all = d.hands.flatten() + d.deadPiles.flatten() + d.stock + d.discard
            check(all.size == 104 && all.map { it.id }.toSet().size == 104)
        }
        check(Dealer.deal(Random(42)) == Dealer.deal(Random(42)))
    }
    verify("natural two preserves clean canasta") {
        val result = MeldRules.evaluate((1..7).map { c(it) })!!
        check(result.clean && result.canasta && result.bonus == 200)
    }
    verify("wild two fills exactly one gap") {
        val result = MeldRules.evaluate(listOf(c(4), c(5), c(2, 2), c(7), c(8), c(9), c(10)))!!
        check(!result.clean && result.bonus == 100)
        check(MeldRules.evaluate(listOf(c(5), c(2, 1), c(2, 2))) == null)
    }
    verify("ace may be low or high but may not wrap") {
        check(MeldRules.evaluate(listOf(c(12), c(13), c(1)))?.clean == true)
        check(MeldRules.evaluate(listOf(c(1), c(2), c(3)))?.clean == true)
        check(MeldRules.evaluate(listOf(c(13), c(1), c(3))) == null)
        check(MeldRules.evaluate(listOf(c(12), c(13), c(1), c(3))) == null)
    }
    verify("invalid sets, duplicate identities and duplicate natural ranks rejected") {
        check(MeldRules.evaluate(listOf(c(5), c(5, 1), c(5, 2))) == null)
        check(MeldRules.evaluate(listOf(c(5), c(5), c(6))) == null)
        check(MeldRules.evaluate(listOf(c(5), c(5, copy = 1), c(6))) == null)
        check(MeldRules.evaluate(listOf(c(3), c(4, 1), c(5))) == null)
        check(MeldRules.evaluate(listOf(c(3), c(4))) == null)
    }
    verify("all natural runs are accepted, even reversed") {
        for (suit in 0..3) for (size in 3..13) for (start in 1..(15 - size)) {
            val cards = (start until start + size).map { c(if (it == 14) 1 else it, suit) }
            check(MeldRules.evaluate(cards.reversed())?.clean == true)
        }
    }
    verify("natural two plus one foreign wild two is legal") {
        check(MeldRules.evaluate(listOf(c(1), c(2), c(2, 1)))?.clean == false)
    }
    verify("framing survives single-byte reads and multiple messages") {
        val data = bytes(Frame(MessageType.HELLO, 0, "hello".toByteArray())) + bytes(Frame(MessageType.PING, 1))
        val fragmented = object : FilterInputStream(ByteArrayInputStream(data)) {
            override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, minOf(len, 1))
        }
        check(Wire.read(fragmented).payload.toString(Charsets.UTF_8) == "hello")
        check(Wire.read(fragmented).sequence == 1)
        rejects<EOFException> { Wire.read(fragmented) }
    }
    verify("every truncated frame is rejected") {
        val data = bytes(Frame(MessageType.HELLO, 0, byteArrayOf(1, 2, 3)))
        for (length in data.indices) rejects<EOFException> { Wire.read(ByteArrayInputStream(data.copyOf(length))) }
    }
    verify("bad signature, version, type, sequence and oversize rejected before allocation") {
        val data = bytes(Frame(MessageType.HELLO, 0))
        for ((index, value) in listOf(0 to 0, 2 to 99, 3 to 99, 4 to 255, 8 to 127)) {
            val invalid = data.copyOf().also { it[index] = value.toByte() }
            rejects<ProtocolException> { Wire.read(ByteArrayInputStream(invalid)) }
        }
        rejects<IllegalArgumentException> { bytes(Frame(MessageType.PING, -1)) }
        rejects<IllegalArgumentException> { bytes(Frame(MessageType.HELLO, 0, ByteArray(513))) }
        check(Wire.read(ByteArrayInputStream(bytes(Frame(MessageType.HELLO, 0, ByteArray(512))))).payload.size == 512)
    }
    verify("handshake followed by 100 bidirectional probes") {
        val (host, guest) = connected()
        repeat(100) {
            host.receive(guest.receive(host.ping()).single())
            guest.receive(host.receive(guest.ping()).single())
            check(host.pendingPing == null && guest.pendingPing == null)
        }
    }
    verify("wrong rules and false handshake confirmation rejected") {
        rejects<ProtocolException> { ProbeSession(false, "guest").receive(Frame(MessageType.HELLO, 0, "other\nx".toByteArray())) }
        rejects<ProtocolException> { ProbeSession(true, "host").receive(Frame(MessageType.READY, 0, "wrong".toByteArray())) }
        rejects<ProtocolException> { ProbeSession(false, "guest").receive(Frame(MessageType.PING, 1)) }
    }
    verify("duplicate probes and unsolicited replies rejected") {
        val (host, guest) = connected()
        val frame = host.ping()
        guest.receive(frame)
        rejects<ProtocolException> { guest.receive(frame) }
        rejects<IllegalStateException> { host.ping() }
        rejects<ProtocolException> { guest.receive(Frame(MessageType.PONG, 1)) }
        rejects<ProtocolException> { guest.receive(Frame(MessageType.PING, 2, byteArrayOf(1))) }
    }
    verify("a new connection cannot reuse the previous host challenge") {
        val previous = ProbeSession(true, "old").opening().single()
        val staleReply = ProbeSession(false, "guest").receive(previous).single()
        rejects<ProtocolException> { ProbeSession(true, "new").receive(staleReply) }
        val (host, guest) = connected()
        host.receive(guest.receive(host.ping()).single())
        check(host.lastPong == 1)
    }
    println("$checks checks passed")
    gameChecks()
}

private fun gameChecks() {
    var state = GameEngine.create(Random(7))
    check(state.hands.all { it.size == 11 })
    val first = state.view(0)
    state = GameEngine.apply(state, 0, GameCommand(state.revision, 1, GameAction.DRAW_STOCK))
    check(state.hands[0].size == 12 && state.stock.size == 58)
    val replay = GameEngine.apply(state, 0, GameCommand(first.revision, 1, GameAction.DRAW_STOCK))
    check(replay == state)
    val card = state.hands[0].first().id
    state = GameEngine.apply(state, 0, GameCommand(state.revision, 2, GameAction.DISCARD, listOf(card)))
    check(state.turn == 1 && !state.drew)
    GameEngine.validate(state)
    println("PASS game engine transaction, replay and invariants")
}
