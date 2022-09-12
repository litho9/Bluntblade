package bluntblade

import ch.qos.logback.classic.Logger
import com.google.protobuf.GeneratedMessageV3
import bluntblade.game.ClimateType
import bluntblade.game.PacketOpcodes
import bluntblade.interaction.PlayerLocationInfo
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.DefaultEventLoop
import kcp.highway.ChannelConfig
import kcp.highway.KcpListener
import kcp.highway.KcpServer
import kcp.highway.Ukcp
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

val address = InetSocketAddress(22102)

val channelConfig = with(ChannelConfig()) {
    nodelay(true, 40, 2, true)
    mtu = 1400
    sndwnd = 256
    rcvwnd = 256
    timeoutMillis = (30 * 1000).toLong() // 30s
    isUseConvChannel = true
    isAckNoDelay = false
    this
}

object GameServerListener : KcpListener {
    private val logicThread = DefaultEventLoop()
    private val sessions = ConcurrentHashMap<Ukcp, GameSessionKcp>()

    override fun onConnected(ukcp: Ukcp) {
        sessions[ukcp] = GameSessionKcp(ukcp)
    }

    override fun handleReceive(buf: ByteBuf, kcp: Ukcp) {
        logicThread.execute { sessions[kcp]?.receive(buf) }
    }

    override fun handleException(ex: Throwable, ukcp: Ukcp) {}

    override fun handleClose(ukcp: Ukcp) {
        sessions.remove(ukcp)
        ukcp.close()
    }
}

fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")
    val kcpServer = with(KcpServer()) {
        init(GameServerListener, channelConfig, address)
        this
    }
}

class World(
    val id: Int,
    val host: GameSession,
    val guests: MutableList<GameSession>,
    val isMultiplayer: Boolean = false,
    val weatherId: Int,
    val climate: ClimateType
) {
    val players: List<GameSession>
        get() = guests + host
    val accounts: List<Account>
        get() = players.map(GameSession::account)
}

abstract class GameSession {
    lateinit var account: Account
    var lastClientSeq = 10
    open val srtt: Int = 10

    var world: World? = null
    lateinit var scene: Scene
    var curTeamId: Int = 0
    val curTeam: List<EntityAvatar> = listOf()
    var curAvatarId = 0
    val curAvatar: EntityAvatar
        get() = curTeam[curAvatarId]
    var enterSceneToken: Int = 0
    var peerId: Int = 0
    var clientTime: Long = 0
    var lastPingTime = System.currentTimeMillis()
    var isPaused = false
    var lastLocationNotify: Long = System.currentTimeMillis()
    lateinit var locationInfo: PlayerLocationInfo
    // playerLocationInfo {
    //                        uid = it.account.uid
    //                        pos = it.pos.toProto()
    //                        rot = it.rot.toProto()
    //                    }

    abstract fun send(vararg packets: BasePacket)
    abstract fun send(
        data: GeneratedMessageV3?,
        header: ByteArray = ByteArray(0),
        encryption: ByteArray? = null, // null | Crypto.DISPATCH_KEY | Crypto.ENCRYPT_KEY
        opcode: PacketOpcodes = PacketOpcodes.valueOf(data!!.javaClass.simpleName)
    )
    open fun getHostAddress(): String = "0.0.0.0"
}

class GameSessionKcp(val ukcp: Ukcp): GameSession() {
    override fun getHostAddress(): String =
        ukcp.user().remoteAddress.address.hostAddress

    override val srtt: Int
        get() = ukcp.srtt()

    @Suppress("SENSELESS_COMPARISON")
    fun receive(buf: ByteBuf) {
        val bytes = ByteArray(buf.capacity())
        buf.getBytes(0, bytes)
        xor(bytes, if (account != null) ENCRYPT_KEY else DISPATCH_KEY)
        val packet = Unpooled.wrappedBuffer(bytes)
        try {
            while (packet.readableBytes() > 11) { // Length
                val c1 = packet.readShort().toInt()
                if (c1 != const1)
                    return logger.trace("Bad Data Package Received: got $c1, expected $const1")

                // Data
                val opcode = packet.readShort().toInt()
                val header = ByteArray(packet.readShort().toInt())
                val payload = ByteArray(packet.readInt())
                packet.readBytes(header)
                packet.readBytes(payload)

                val c2 = packet.readShort().toInt()
                if (c2 != const2)
                    return logger.trace("Bad Data Package Received: got $c2, expected $const2")

                val packetOpCode = PacketOpcodes.from(opcode)!!
                if (!PacketOpcodes.LOOP_PACKETS.contains(packetOpCode))
                    logger.trace("RECV: ${packetOpCode.name} ($opcode): ${payload.toHex()}")
                GameRouter.route(this, packetOpCode, payload)
                    ?: logger.debug("Unhandled packet ($opcode)")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            packet.release()
        }
    }

    override fun send(vararg packets: BasePacket) {
        for (packet in packets)
            send(packet.data, packet.header, packet.encryption, packet.opcode)
    }

    override fun send(
        data: GeneratedMessageV3?,
        header: ByteArray,
        encryption: ByteArray?,
        opcode: PacketOpcodes
    ) {
        // DO NOT REMOVE (unless we find a way to validate code before
        // sending to client which I don't think we can)
        // Stop WindSeedClientNotify from being sent for security purposes.
        if (PacketOpcodes.BANNED_PACKETS.contains(opcode)) return

        logger.trace("SEND: ${opcode.name} ($opcode): $data")

        val bytes = data?.toByteArray() ?: ByteArray(0)
        val baos = ByteArrayOutputStream(2 + 2 + 2 + 4 + header.size + bytes.size + 2)
        writeUint16(baos, const1)
        writeUint16(baos, opcode.code)
        writeUint16(baos, header.size)
        writeUint32(baos, bytes.size)
        baos.write(header)
        baos.write(bytes)
        writeUint16(baos, const2)
        val packet0 = baos.toByteArray()
        val buf = Unpooled.wrappedBuffer(if (encryption == null) packet0 else xor(packet0, encryption))
        ukcp.write(buf)
        buf.release()
    }

    private fun writeUint16(baos: ByteArrayOutputStream, i: Int) {
        // Unsigned short
        baos.write((i ushr 8 and 0xFF).toByte().toInt())
        baos.write((i and 0xFF).toByte().toInt())
    }

    private fun writeUint32(baos: ByteArrayOutputStream, i: Int) {
        // Unsigned int (long)
        baos.write((i ushr 24 and 0xFF).toByte().toInt())
        baos.write((i ushr 16 and 0xFF).toByte().toInt())
        baos.write((i ushr 8 and 0xFF).toByte().toInt())
        baos.write((i and 0xFF).toByte().toInt())
    }

    companion object {
        val logger = LoggerFactory.getLogger(GameSession::class.java) as Logger

        private val secureRandom = SecureRandom()
        var DISPATCH_KEY = readBytes("keys/dispatchKey.bin")
        var DISPATCH_SEED = readBytes("keys/dispatchSeed.bin")
        var ENCRYPT_KEY = readBytes("keys/secretKey.bin")
        var ENCRYPT_SEED_BUFFER = readBytes("keys/secretKeyBuffer.bin")
        var ENCRYPT_SEED = java.lang.Long.parseUnsignedLong("11468049314633205968")

        fun xor(packet: ByteArray, key: ByteArray): ByteArray {
//        return // try {
            packet.indices.forEach { i -> packet[i] = (packet[i].toInt() xor key[i % key.size].toInt()).toByte() }
//        } catch (e: Exception) { logger.error("Crypto error.", e); 0b0 }
            return packet
//        try {
//            packet.indices.forEach { i ->
//                packet[i] = (packet[i].toInt() xor key[i % key.size].toInt()).toByte()
//            }
//        } catch (e: Exception) {
//            logger.error("Crypto error.", e)
//        }
        }

        fun readBytes(path: String) = GameSession::class.java.classLoader.getResource(path)!!.readBytes()

        fun createSessionKey(length: Int): ByteArray {
            val bytes = ByteArray(length)
            secureRandom.nextBytes(bytes)
            return bytes
        }

        private const val const1 = 17767 // 0x4567
        private const val const2 = -30293 // 0x89ab
    }
}

open class BasePacket(
    val data: GeneratedMessageV3?,
    var header: ByteArray = ByteArray(0),
    var encryption: ByteArray? = null, // null | Crypto.DISPATCH_KEY | Crypto.ENCRYPT_KEY
    val opcode: PacketOpcodes = PacketOpcodes.valueOf(data!!.javaClass.simpleName),
) {
    companion object {
        fun buildHeader(clientSequence: Int = 0): ByteArray = packetHead {
            clientSequenceId = clientSequence
            timestamp = System.currentTimeMillis()
        }.toByteArray()
    }
}

fun today(): LocalDate = LocalDate.now(ZoneId.of("America/Phoenix")) // arbitrary GMT-7 timezone