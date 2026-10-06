package ko.bura.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/** Compact explicit snapshot for local persistence and Bluetooth projections. */
object GameCodec {
    private const val MAGIC = 0x424b31
    const val MAX_BYTES = 1024
    private fun out(block: DataOutputStream.() -> Unit) = ByteArrayOutputStream().also { DataOutputStream(it).use(block) }.toByteArray().also { require(it.size <= MAX_BYTES) }
    private fun <T> input(bytes: ByteArray, block: DataInputStream.() -> T): T = DataInputStream(ByteArrayInputStream(bytes)).use { it.block() }
    private fun DataOutputStream.cards(cards: List<Card>) { writeByte(cards.size); cards.forEach { writeByte(it.id) } }
    private fun DataInputStream.cards() = List(readUnsignedByte().also { require(it <= 104) }) { Card(readUnsignedByte()) }
    fun state(s: GameState): ByteArray = out {
        GameEngine.validate(s); writeInt(MAGIC); writeUTF(s.id); writeInt(s.revision)
        s.hands.forEach { cards(it) }; writeByte(s.dead.size); s.dead.forEach { cards(it) }; cards(s.stock); cards(s.discard)
        writeByte(s.melds[0].size); writeByte(s.melds[1].size); s.melds.flatten().forEach { cards(it) }
        s.tookDead.forEach(::writeBoolean); writeByte(s.turn); writeBoolean(s.drew); writeByte(s.phase.ordinal)
        s.totals.forEach(::writeInt); s.roundScores.forEach(::writeInt); writeInt(s.finisher); s.sequences.forEach(::writeInt); s.votes.forEach(::writeBoolean)
    }
    fun readState(bytes: ByteArray): GameState = input(bytes) {
        require(readInt() == MAGIC); val id = readUTF(); val revision = readInt(); val hands = List(2) { cards() }
        val dead = List(readUnsignedByte()) { cards() }; val stock = cards(); val discard = cards()
        val first = readUnsignedByte(); val second = readUnsignedByte(); val flat = List(first + second) { cards() }
        val took = List(2) { readBoolean() }; val turn = readUnsignedByte(); val drew = readBoolean()
        val phase = GamePhase.entries[readUnsignedByte()]; val totals = List(2) { readInt() }; val scores = List(2) { readInt() }
        val finisher = readInt(); val sequences = List(2) { readInt() }; val votes = List(2) { readBoolean() }
        GameState(id, revision, hands, dead, stock, discard, listOf(flat.take(first), flat.drop(first)), took, turn, drew, phase, totals, scores, finisher, sequences, votes).also(GameEngine::validate)
    }
    fun command(command: GameCommand): ByteArray = out { writeInt(MAGIC); writeInt(command.revision); writeInt(command.sequence); writeByte(command.action.ordinal); writeByte(command.meld); writeByte(command.cards.size); command.cards.forEach { writeByte(it) } }
    fun readCommand(bytes: ByteArray): GameCommand = input(bytes) { require(readInt() == MAGIC); val revision = readInt(); val sequence = readInt(); val action = GameAction.entries[readUnsignedByte()]; val meld = readByte().toInt(); val cards = List(readUnsignedByte()) { readUnsignedByte() }; GameCommand(revision, sequence, action, cards, meld) }
}

