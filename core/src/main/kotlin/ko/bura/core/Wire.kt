package ko.bura.core

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ProtocolException(message: String) : IOException(message)
enum class MessageType(val code: Int) { HELLO(1), READY(2), PING(3), PONG(4) }
data class Frame(val type: MessageType, val sequence: Int, val payload: ByteArray = byteArrayOf())

/** Length-delimited binary frames; bounded allocation, no object deserialization. */
object Wire {
    const val VERSION = 1
    const val MAX_PAYLOAD = 512
    const val RULES = "bura-open-v1"
    fun write(output: OutputStream, frame: Frame) {
        require(frame.sequence >= 0 && frame.payload.size <= MAX_PAYLOAD)
        val data = DataOutputStream(output)
        data.writeShort(0x424b)
        data.writeByte(VERSION)
        data.writeByte(frame.type.code)
        data.writeInt(frame.sequence)
        data.writeShort(frame.payload.size)
        data.write(frame.payload)
        data.flush()
    }
    fun read(input: InputStream): Frame {
        val data = DataInputStream(input)
        if (data.readUnsignedShort() != 0x424b) throw ProtocolException("Assinatura incompatível")
        if (data.readUnsignedByte() != VERSION) throw ProtocolException("Versão incompatível")
        val code = data.readUnsignedByte()
        val type = MessageType.entries.firstOrNull { it.code == code } ?: throw ProtocolException("Mensagem desconhecida")
        val sequence = data.readInt()
        if (sequence < 0) throw ProtocolException("Sequência inválida")
        val length = data.readUnsignedShort()
        if (length > MAX_PAYLOAD) throw ProtocolException("Mensagem grande demais")
        val payload = ByteArray(length)
        data.readFully(payload)
        return Frame(type, sequence, payload)
    }
}
