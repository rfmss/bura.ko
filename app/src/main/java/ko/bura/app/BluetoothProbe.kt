package ko.bura.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import ko.bura.core.ProbeSession
import ko.bura.core.Wire
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** One trusted paired peer. All socket I/O is off the UI thread. No background service. */
@SuppressLint("MissingPermission") // Activity grants CONNECT before opening this object.
class BluetoothProbe(private val adapter: BluetoothAdapter, private val listener: (String, Boolean, Long?) -> Unit) {
    companion object { val SERVICE: UUID = UUID.fromString("a4ad13bd-2c5d-4969-8740-eac061c146cb") }
    private val main = Handler(Looper.getMainLooper())
    private val generation = AtomicInteger()
    private val writer = ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(8))
    private val timer = Executors.newSingleThreadScheduledExecutor()
    private val lock = Any()
    private var socket: BluetoothSocket? = null
    private var server: BluetoothServerSocket? = null
    private var session: ProbeSession? = null
    private var timeout: ScheduledFuture<*>? = null
    private var timeoutEpoch = 0L
    private var pingStarted = 0L

    fun open(peer: BluetoothDevice, host: Boolean) {
        val token: Int
        synchronized(lock) {
            closeLocked()
            token = generation.get()
        }
        report(token, if (host) "Aguardando o celular escolhido…" else "Conectando…", false)
        armTimeout(token, 45, "Tempo esgotado. Tente conectar novamente.")
        Thread({
            try {
                val connected: BluetoothSocket
                if (host) {
                    val listening = adapter.listenUsingRfcommWithServiceRecord("bura.ko", SERVICE)
                    synchronized(lock) {
                        if (generation.get() != token) { listening.close(); return@Thread }
                        server = listening
                    }
                    connected = listening.accept()
                    listening.close()
                    synchronized(lock) { if (generation.get() == token) server = null }
                    if (connected.remoteDevice.address != peer.address) {
                        connected.close()
                        throw IOException("Outro aparelho tentou entrar. Escolha o mesmo par nos dois celulares.")
                    }
                } else {
                    connected = peer.createRfcommSocketToServiceRecord(SERVICE)
                    synchronized(lock) {
                        if (generation.get() != token) { connected.close(); return@Thread }
                        socket = connected // timeout can cancel the blocking connect
                    }
                    connected.connect()
                }
                val protocol = ProbeSession(host, UUID.randomUUID().toString())
                synchronized(lock) {
                    if (generation.get() != token) { connected.close(); return@Thread }
                    socket = connected
                    session = protocol
                }
                armTimeout(token, 10, "O outro aplicativo não confirmou a conexão.")
                enqueue(token) { protocol.opening().forEach { Wire.write(connected.outputStream, it) } }
                while (generation.get() == token) {
                    val frame = Wire.read(connected.inputStream)
                    synchronized(lock) {
                        if (generation.get() != token) return@Thread
                        val wasReady = protocol.ready
                        val oldPong = protocol.lastPong
                        val replies = protocol.receive(frame)
                        enqueue(token) { replies.forEach { Wire.write(connected.outputStream, it) } }
                        if (!wasReady && protocol.ready) {
                            cancelTimeoutLocked()
                            report(token, "Conexão confirmada • pronta para testar", true)
                        } else if (oldPong != protocol.lastPong) {
                            cancelTimeoutLocked()
                            val elapsed = SystemClock.elapsedRealtime() - pingStarted
                            report(token, "Resposta recebida • Bluetooth offline", true, elapsed)
                        }
                    }
                }
            } catch (error: Exception) {
                fail(token, error.message ?: "Conexão interrompida")
            }
        }, "burako-bluetooth").start()
    }

    fun ping() {
        synchronized(lock) {
            val protocol = session ?: return
            val connected = socket ?: return
            if (!protocol.ready || protocol.pendingPing != null) return
            val token = generation.get()
            val frame = protocol.ping()
            pingStarted = SystemClock.elapsedRealtime()
            report(token, "Aguardando resposta…", false)
            armTimeout(token, 8, "Sem resposta. Reconecte os celulares.")
            enqueue(token) { Wire.write(connected.outputStream, frame) }
        }
    }

    private fun enqueue(token: Int, action: () -> Unit) {
        try {
            writer.execute {
                if (generation.get() == token) {
                    try { action() } catch (error: Exception) { fail(token, error.message ?: "Falha de envio") }
                }
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
            fail(token, "Fila de envio cheia. Reconecte.")
        }
    }
    private fun armTimeout(token: Int, seconds: Long, message: String) {
        synchronized(lock) {
            if (generation.get() != token) return
            cancelTimeoutLocked()
            val epoch = timeoutEpoch
            timeout = timer.schedule({
                synchronized(lock) {
                    if (timeoutEpoch == epoch) fail(token, message)
                }
            }, seconds, TimeUnit.SECONDS)
        }
    }
    private fun cancelTimeoutLocked() {
        timeoutEpoch++
        timeout?.cancel(false)
        timeout = null
    }
    private fun report(token: Int, message: String, ready: Boolean, latency: Long? = null) {
        main.post { if (generation.get() == token) listener(message, ready, latency) }
    }
    private fun fail(token: Int, message: String) {
        synchronized(lock) {
            if (generation.get() != token) return
            closeLocked()
            report(generation.get(), message, false)
        }
    }
    private fun closeLocked() {
        generation.incrementAndGet()
        cancelTimeoutLocked()
        try { server?.close() } catch (_: IOException) { }
        try { socket?.close() } catch (_: IOException) { }
        server = null
        socket = null
        session = null
        writer.queue.clear()
    }
    fun close() {
        synchronized(lock) { closeLocked() }
        writer.shutdownNow()
        timer.shutdownNow()
        main.removeCallbacksAndMessages(null)
    }
}
