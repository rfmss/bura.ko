package ko.bura.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import ko.bura.core.Frame
import ko.bura.core.Wire
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("MissingPermission")
class GameBluetoothLink(private val adapter: BluetoothAdapter, private val peer: BluetoothDevice, private val host: Boolean,
                        private val received: (Frame) -> Unit, private val connected: () -> Unit, private val failed: (String) -> Unit) {
    private val closed = AtomicBoolean(false)
    private val writer = ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(12))
    private var socket: BluetoothSocket? = null
    private var server: BluetoothServerSocket? = null
    fun start() {
        Thread({
            try {
                val channel: BluetoothSocket
                if (host) {
                    val listener = adapter.listenUsingRfcommWithServiceRecord("bura.ko", BluetoothProbe.SERVICE)
                    server = listener
                    while (!closed.get()) {
                        val candidate = listener.accept()
                        if (candidate.remoteDevice.address == peer.address) { channel = candidate; break }
                        candidate.close()
                    }
                    listener.close(); server = null
                } else {
                    channel = peer.createRfcommSocketToServiceRecord(BluetoothProbe.SERVICE)
                    socket = channel; channel.connect()
                }
                if (closed.get()) { channel.close(); return@Thread }
                socket = channel; connected()
                while (!closed.get()) received(Wire.read(channel.inputStream))
            } catch (error: Exception) { if (!closed.get()) failed(error.message ?: "Bluetooth desconectado") }
        }, "bura-game-link").start()
    }
    fun send(frame: Frame) {
        if (closed.get()) return
        try { writer.execute { try { if (!closed.get()) Wire.write(socket!!.outputStream, frame) } catch (error: Exception) { if (!closed.get()) failed(error.message ?: "Falha de envio") } } }
        catch (_: Exception) { failed("Fila de mensagens cheia") }
    }
    fun close() {
        if (!closed.compareAndSet(false, true)) return
        try { server?.close() } catch (_: IOException) { }
        try { socket?.close() } catch (_: IOException) { }
        writer.shutdownNow()
    }
}
