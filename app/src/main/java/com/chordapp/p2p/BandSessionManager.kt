package com.chordapp.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*
import java.util.concurrent.CopyOnWriteArrayList

enum class BandMode {
    NORMAL,
    SERVER,
    CLIENT
}

data class DiscoveredServer(
    val name: String,
    val host: String,
    val port: Int,
    val requiresPin: Boolean = false
)

data class LiveSongPayload(
    val title: String,
    val artist: String,
    val artistSlug: String,
    val titleSlug: String,
    val source: String,
    val sourceUrl: String,
    val versionName: String?,
    val effectiveKey: String?,
    val transpose: Int = 0,
    val capo: Int = 0,
    val rawContent: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("title", title)
        obj.put("artist", artist)
        obj.put("artistSlug", artistSlug)
        obj.put("titleSlug", titleSlug)
        obj.put("source", source)
        obj.put("sourceUrl", sourceUrl)
        obj.put("versionName", versionName ?: "")
        obj.put("effectiveKey", effectiveKey ?: "")
        obj.put("transpose", transpose)
        obj.put("capo", capo)
        obj.put("rawContent", rawContent)
        obj.put("timestamp", timestamp)
        return obj.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): LiveSongPayload {
            val obj = JSONObject(jsonStr)
            return LiveSongPayload(
                title = obj.optString("title", ""),
                artist = obj.optString("artist", ""),
                artistSlug = obj.optString("artistSlug", ""),
                titleSlug = obj.optString("titleSlug", ""),
                source = obj.optString("source", ""),
                sourceUrl = obj.optString("sourceUrl", ""),
                versionName = obj.optString("versionName", "").ifBlank { null },
                effectiveKey = obj.optString("effectiveKey", "").ifBlank { null },
                transpose = obj.optInt("transpose", 0),
                capo = obj.optInt("capo", 0),
                rawContent = obj.optString("rawContent", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}

sealed class ClientConnectionState {
    object Disconnected : ClientConnectionState()
    data class Connecting(val serverName: String) : ClientConnectionState()
    data class Connected(val serverName: String, val host: String) : ClientConnectionState()
    data class Error(val message: String) : ClientConnectionState()
}

/**
 * Global singleton managing P2P Band Live Sync over Local Wi-Fi or Mobile Hotspot.
 * Zero internet required. Uses NSD + UDP broadcast for discovery and TCP socket for live sync.
 */
object BandSessionManager {
    private const val TAG = "BandSessionManager"
    private const val PREFS_NAME = "kordapp_band_prefs"
    private const val KEY_MODE = "band_mode"
    private const val KEY_NAME = "device_name"
    private const val KEY_PIN = "server_pin"
    private const val KEY_PIN_ENABLED = "pin_enabled"
    private const val KEY_PIN_PROMPT_DISMISSED = "pin_prompt_dismissed"
    private const val DEFAULT_TCP_PORT = 8989
    private const val UDP_DISCOVERY_PORT = 8990
    private const val NSD_SERVICE_TYPE = "_kordband._tcp."

    private var appContext: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentMode = MutableStateFlow(BandMode.NORMAL)
    val currentMode: StateFlow<BandMode> = _currentMode.asStateFlow()

    private val _deviceName = MutableStateFlow(Build.MODEL ?: "KordApp Device")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _serverPin = MutableStateFlow("")
    val serverPin: StateFlow<String> = _serverPin.asStateFlow()

    private val _isPinEnabled = MutableStateFlow(false)
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _isBroadcastPaused = MutableStateFlow(false)
    val isBroadcastPaused: StateFlow<Boolean> = _isBroadcastPaused.asStateFlow()

    // Server State
    private val _connectedClients = MutableStateFlow<List<String>>(emptyList())
    val connectedClients: StateFlow<List<String>> = _connectedClients.asStateFlow()
    private val _webStageUrl = MutableStateFlow<String?>(null)
    val webStageUrl: StateFlow<String?> = _webStageUrl.asStateFlow()
    private val _webStageIpUrl = MutableStateFlow<String?>(null)
    val webStageIpUrl: StateFlow<String?> = _webStageIpUrl.asStateFlow()
    private val serverClients = CopyOnWriteArrayList<ClientSession>()
    private var serverSocket: ServerSocket? = null
    private var webStageServer: WebStageServer? = null
    private var currentActiveSong: LiveSongPayload? = null

    // Client State
    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers.asStateFlow()

    private val _connectionState = MutableStateFlow<ClientConnectionState>(ClientConnectionState.Disconnected)
    val connectionState: StateFlow<ClientConnectionState> = _connectionState.asStateFlow()

    private val _incomingSong = MutableSharedFlow<LiveSongPayload>(replay = 0, extraBufferCapacity = 64)
    val incomingSong: SharedFlow<LiveSongPayload> = _incomingSong.asSharedFlow()

    private val _incomingTranspose = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val incomingTranspose: SharedFlow<Int> = _incomingTranspose.asSharedFlow()

    private val _standbyTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val standbyTrigger: SharedFlow<Unit> = _standbyTrigger.asSharedFlow()

    private val rawSheetCache = ConcurrentHashMap<String, String>()

    fun cacheRawSheet(url: String, content: String) {
        if (url.isNotBlank() && content.isNotBlank()) {
            rawSheetCache[url] = content
        }
    }

    fun getCachedRawSheet(url: String): String? = rawSheetCache[url]

    private var nsdManager: NsdManager? = null
    private var nsdRegistrationListener: NsdManager.RegistrationListener? = null
    private var nsdHttpRegistrationListener: NsdManager.RegistrationListener? = null
    private var nsdDiscoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private var clientSocket: Socket? = null
    private var clientJob: Job? = null
    private var udpJob: Job? = null
    private var mdnsResponder: MdnsResponder? = null

    fun initialize(context: Context) {
        val app = context.applicationContext
        appContext = app
        nsdManager = app.getSystemService(Context.NSD_SERVICE) as? NsdManager

        val wifi = app.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        multicastLock = wifi?.createMulticastLock("kordapp_p2p_lock")?.apply {
            setReferenceCounted(true)
        }

        val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedModeStr = prefs.getString(KEY_MODE, BandMode.NORMAL.name) ?: BandMode.NORMAL.name
        val savedMode = runCatching { BandMode.valueOf(savedModeStr) }.getOrDefault(BandMode.NORMAL)
        val savedName = prefs.getString(KEY_NAME, Build.MODEL) ?: Build.MODEL
        val savedPin = prefs.getString(KEY_PIN, "") ?: ""
        val savedPinEnabled = prefs.getBoolean(KEY_PIN_ENABLED, false)

        _deviceName.value = savedName
        _serverPin.value = savedPin
        _isPinEnabled.value = savedPinEnabled
        setMode(savedMode)
    }

    fun setMode(mode: BandMode) {
        if (_currentMode.value == mode && mode != BandMode.NORMAL) return
        _currentMode.value = mode

        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs?.edit()?.putString(KEY_MODE, mode.name)?.apply()

        when (mode) {
            BandMode.NORMAL -> {
                stopServer()
                stopClient()
            }
            BandMode.SERVER -> {
                stopClient()
                startServer()
            }
            BandMode.CLIENT -> {
                stopServer()
                startClient()
            }
        }
    }

    fun setDeviceName(name: String) {
        val clean = name.trim().ifBlank { Build.MODEL ?: "KordApp" }
        _deviceName.value = clean
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()?.putString(KEY_NAME, clean)?.apply()
        // If server is running, restart to re-broadcast new name
        if (_currentMode.value == BandMode.SERVER) {
            startServer()
        }
    }

    fun setPin(pin: String) {
        val clean = pin.trim()
        _serverPin.value = clean
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_PIN, clean)
            ?.apply()
        if (clean.isNotBlank()) {
            setPinPromptDismissed(dismissed = true)
        }
    }

    fun setPinEnabled(enabled: Boolean) {
        _isPinEnabled.value = enabled
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()?.putBoolean(KEY_PIN_ENABLED, enabled)?.apply()
    }

    fun isPinPromptDismissed(context: Context? = appContext): Boolean {
        val ctx = context ?: appContext ?: return false
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PIN_PROMPT_DISMISSED, false)
    }

    fun setPinPromptDismissed(context: Context? = appContext, dismissed: Boolean = true) {
        val ctx = context ?: appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PIN_PROMPT_DISMISSED, dismissed).apply()
    }

    fun toggleBroadcastPause() {
        _isBroadcastPaused.value = !_isBroadcastPaused.value
    }

    /* =========================================================================
     * SERVER IMPLEMENTATION
     * ========================================================================= */

    private fun startServer() {
        stopServer()
        multicastLock?.acquire()
        registerNsdService()
        startMdnsResponder()
        startUdpResponder()
        startTcpServer()
        startWebStageServer()
    }

    private fun startMdnsResponder() {
        stopMdnsResponder()
        mdnsResponder = MdnsResponder("kordapp.local") {
            getLocalIpAddress()
        }.apply {
            start(scope)
        }
    }

    private fun stopMdnsResponder() {
        mdnsResponder?.stop()
        mdnsResponder = null
    }

    private fun startWebStageServer() {
        try {
            webStageServer = WebStageServer(8990) { _deviceName.value }.apply {
                start()
            }
            val ip = getLocalIpAddress() ?: "127.0.0.1"
            _webStageUrl.value = "http://kordapp.local:8990"
            _webStageIpUrl.value = "http://$ip:8990"
            if (currentActiveSong != null && !_isBroadcastPaused.value) {
                webStageServer?.broadcastSong(currentActiveSong!!)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WebStageServer", e)
        }
    }

    private fun stopServer() {
        unregisterNsdService()
        stopMdnsResponder()
        udpJob?.cancel()
        udpJob = null

        webStageServer?.stop()
        webStageServer = null
        _webStageUrl.value = null
        _webStageIpUrl.value = null

        serverClients.forEach { it.close() }
        serverClients.clear()
        _connectedClients.value = emptyList()

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        if (multicastLock?.isHeld == true) {
            try { multicastLock?.release() } catch (_: Exception) {}
        }
    }

    private fun registerNsdService() {
        val manager = nsdManager ?: return
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "KordApp-${_deviceName.value}"
            serviceType = NSD_SERVICE_TYPE
            port = DEFAULT_TCP_PORT
        }

        nsdRegistrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo?) {
                Log.d(TAG, "NSD service registered: ${info?.serviceName}")
            }
            override fun onRegistrationFailed(info: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "NSD registration failed: $errorCode")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo?) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo?, errorCode: Int) {}
        }

        try {
            manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, nsdRegistrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register NSD service", e)
        }

        val httpServiceInfo = NsdServiceInfo().apply {
            serviceName = "kordapp"
            serviceType = "_http._tcp."
            port = 8990
        }

        nsdHttpRegistrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo?) {
                Log.d(TAG, "NSD HTTP service registered: ${info?.serviceName}")
            }
            override fun onRegistrationFailed(info: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "NSD HTTP registration failed: $errorCode")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo?) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo?, errorCode: Int) {}
        }

        try {
            manager.registerService(httpServiceInfo, NsdManager.PROTOCOL_DNS_SD, nsdHttpRegistrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register NSD HTTP service", e)
        }
    }

    private fun unregisterNsdService() {
        val manager = nsdManager ?: return
        nsdRegistrationListener?.let {
            try { manager.unregisterService(it) } catch (_: Exception) {}
        }
        nsdRegistrationListener = null

        nsdHttpRegistrationListener?.let {
            try { manager.unregisterService(it) } catch (_: Exception) {}
        }
        nsdHttpRegistrationListener = null
    }

    private fun startUdpResponder() {
        udpJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(UDP_DISCOVERY_PORT).apply {
                    broadcast = true
                    soTimeout = 0
                }
                val buffer = ByteArray(1024)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val msg = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                    if (msg.startsWith("KORD_DISCOVER")) {
                        val hasPin = if (_isPinEnabled.value && _serverPin.value.isNotBlank()) 1 else 0
                        val response = "KORD_OFFER|${_deviceName.value}|$DEFAULT_TCP_PORT|$hasPin"
                        val respData = response.toByteArray(Charsets.UTF_8)
                        val respPacket = DatagramPacket(respData, respData.size, packet.address, packet.port)
                        socket.send(respPacket)
                    }
                }
            } catch (e: Exception) {
                if (isActive) Log.w(TAG, "UDP responder error", e)
            } finally {
                socket?.close()
            }
        }
    }

    private fun startTcpServer() {
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(DEFAULT_TCP_PORT)
                Log.d(TAG, "TCP Server listening on port $DEFAULT_TCP_PORT")
                while (isActive) {
                    val sock = serverSocket?.accept() ?: break
                    sock.tcpNoDelay = true
                    handleIncomingClient(sock)
                }
            } catch (e: Exception) {
                if (isActive) Log.e(TAG, "TCP Server error", e)
            }
        }
    }

    private fun handleIncomingClient(sock: Socket) {
        scope.launch(Dispatchers.IO) {
            try {
                sock.tcpNoDelay = true
                val outputStream = sock.getOutputStream()
                val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))

                fun sendDirect(msg: String) {
                    val line = msg.replace("\r", "").replace("\n", "") + "\n"
                    val bytes = line.toByteArray(Charsets.UTF_8)
                    synchronized(outputStream) {
                        outputStream.write(bytes)
                        outputStream.flush()
                    }
                }

                // First line: Handshake AUTH|clientName|pin
                val line = reader.readLine() ?: return@launch sock.close()
                val parts = line.split("|")
                if (parts.size < 2 || parts[0] != "AUTH") {
                    sendDirect("ERROR|INVALID_HANDSHAKE")
                    return@launch sock.close()
                }

                val clientName = parts[1]
                val clientPin = parts.getOrNull(2) ?: ""

                if (_isPinEnabled.value && _serverPin.value.isNotBlank()) {
                    if (clientPin != _serverPin.value) {
                        sendDirect("ERROR|INVALID_PIN")
                        return@launch sock.close()
                    }
                }

                sendDirect("AUTH_OK|${_deviceName.value}")
                val session = ClientSession(clientName, sock, outputStream)
                serverClients.add(session)
                updateConnectedClients()

                // If a song is currently active and broadcast is not paused, push it immediately
                val active = currentActiveSong
                if (active != null && !_isBroadcastPaused.value) {
                    val b64 = android.util.Base64.encodeToString(active.toJson().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                    session.send("SONG|$b64")
                }

                // Listen for keep-alive or disconnect
                while (isActive) {
                    val msg = reader.readLine() ?: break
                    if (msg == "PING") {
                        session.send("PONG")
                    }
                }
            } catch (_: Exception) {
            } finally {
                serverClients.removeAll { it.socket == sock }
                updateConnectedClients()
                try { sock.close() } catch (_: Exception) {}
            }
        }
    }

    private fun updateConnectedClients() {
        _connectedClients.value = serverClients.map { it.name }
    }

    /**
     * Broadcasts a song opened by Server to all connected clients and Web Stage Mirror.
     */
    fun broadcastSong(payload: LiveSongPayload) {
        if (_currentMode.value != BandMode.SERVER || _isBroadcastPaused.value) return
        currentActiveSong = payload
        webStageServer?.broadcastSong(payload)
        val b64 = android.util.Base64.encodeToString(payload.toJson().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        val msg = "SONG|$b64"
        scope.launch(Dispatchers.IO) {
            serverClients.forEach { it.send(msg) }
        }
    }

    /**
     * Broadcasts transpose update to all connected clients and Web Stage Mirror.
     */
    fun broadcastTranspose(transpose: Int) {
        if (_currentMode.value != BandMode.SERVER || _isBroadcastPaused.value) return
        webStageServer?.broadcastTranspose(transpose)
        val msg = "TRANSPOSE|$transpose"
        scope.launch(Dispatchers.IO) {
            serverClients.forEach { it.send(msg) }
        }
    }

    /**
     * Broadcasts standby when server leaves player screen.
     */
    fun broadcastStandby() {
        if (_currentMode.value != BandMode.SERVER || _isBroadcastPaused.value) return
        currentActiveSong = null
        webStageServer?.broadcastStandby()
        val msg = "STANDBY|"
        scope.launch(Dispatchers.IO) {
            serverClients.forEach { it.send(msg) }
        }
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces() ?: return null
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private class ClientSession(val name: String, val socket: Socket, private val outputStream: java.io.OutputStream) {
        fun send(text: String) {
            try {
                val line = text.replace("\r", "").replace("\n", "") + "\n"
                val bytes = line.toByteArray(Charsets.UTF_8)
                synchronized(outputStream) {
                    outputStream.write(bytes)
                    outputStream.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send to client $name", e)
            }
        }
        fun close() {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    /* =========================================================================
     * CLIENT IMPLEMENTATION
     * ========================================================================= */

    private fun startClient() {
        stopClient()
        multicastLock?.acquire()
        startNsdDiscovery()
        startUdpDiscovery()
    }

    private fun stopClient() {
        stopNsdDiscovery()
        clientJob?.cancel()
        clientJob = null
        try { clientSocket?.close() } catch (_: Exception) {}
        clientSocket = null
        _connectionState.value = ClientConnectionState.Disconnected
        _discoveredServers.value = emptyList()

        if (multicastLock?.isHeld == true) {
            try { multicastLock?.release() } catch (_: Exception) {}
        }
    }

    private fun startNsdDiscovery() {
        val manager = nsdManager ?: return
        nsdDiscoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String?) {
                Log.d(TAG, "NSD discovery started")
            }
            override fun onServiceFound(info: NsdServiceInfo?) {
                if (info == null || !info.serviceType.contains("kordband")) return
                manager.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onServiceResolved(resolved: NsdServiceInfo?) {
                        resolved ?: return
                        val rawName = resolved.serviceName.removePrefix("KordApp-")
                        val host = resolved.host?.hostAddress ?: return
                        addDiscoveredServer(DiscoveredServer(rawName, host, resolved.port, false))
                    }
                    override fun onResolveFailed(info: NsdServiceInfo?, errorCode: Int) {}
                })
            }
            override fun onServiceLost(info: NsdServiceInfo?) {}
            override fun onDiscoveryStopped(serviceType: String?) {}
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
        }

        try {
            manager.discoverServices(NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, nsdDiscoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start NSD discovery", e)
        }
    }

    private fun stopNsdDiscovery() {
        val manager = nsdManager ?: return
        val listener = nsdDiscoveryListener ?: return
        try {
            manager.stopServiceDiscovery(listener)
        } catch (_: Exception) {}
        nsdDiscoveryListener = null
    }

    private fun startUdpDiscovery() {
        clientJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket().apply {
                    broadcast = true
                    soTimeout = 2000
                }
                while (isActive) {
                    // Send discovery ping to broadcast and default hotspot gateway (192.168.43.1)
                    val pingBytes = "KORD_DISCOVER".toByteArray(Charsets.UTF_8)
                    val targets = listOf(
                        InetAddress.getByName("255.255.255.255"),
                        InetAddress.getByName("192.168.43.1")
                    )
                    targets.forEach { target ->
                        try {
                            val packet = DatagramPacket(pingBytes, pingBytes.size, target, UDP_DISCOVERY_PORT)
                            socket.send(packet)
                        } catch (_: Exception) {}
                    }

                    // Receive responses
                    val buffer = ByteArray(1024)
                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < 2000 && isActive) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            val msg = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                            if (msg.startsWith("KORD_OFFER|")) {
                                val parts = msg.split("|")
                                if (parts.size >= 4) {
                                    val name = parts[1]
                                    val port = parts[2].toIntOrNull() ?: DEFAULT_TCP_PORT
                                    val hasPin = parts[3] == "1"
                                    val host = packet.address.hostAddress ?: continue
                                    addDiscoveredServer(DiscoveredServer(name, host, port, hasPin))
                                }
                            }
                        } catch (_: SocketTimeoutException) {
                            break
                        } catch (_: Exception) {}
                    }
                    delay(3000)
                }
            } catch (_: Exception) {
            } finally {
                socket?.close()
            }
        }
    }

    private fun addDiscoveredServer(server: DiscoveredServer) {
        val current = _discoveredServers.value.toMutableList()
        val index = current.indexOfFirst { it.host == server.host && it.port == server.port }
        if (index >= 0) {
            current[index] = server
        } else {
            current.add(server)
        }
        _discoveredServers.value = current
    }

    /**
     * Connects to a selected Server.
     */
    fun connectToServer(server: DiscoveredServer, pin: String? = null, onAuthFailed: (String) -> Unit = {}) {
        scope.launch(Dispatchers.IO) {
            _connectionState.value = ClientConnectionState.Connecting(server.name)
            try {
                try { clientSocket?.close() } catch (_: Exception) {}
                val sock = Socket()
                sock.tcpNoDelay = true
                sock.connect(InetSocketAddress(server.host, server.port), 5000)
                clientSocket = sock

                val outputStream = sock.getOutputStream()
                val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))

                fun sendClientMsg(msg: String) {
                    val line = msg.replace("\r", "").replace("\n", "") + "\n"
                    val bytes = line.toByteArray(Charsets.UTF_8)
                    synchronized(outputStream) {
                        outputStream.write(bytes)
                        outputStream.flush()
                    }
                }

                // Send Handshake
                sendClientMsg("AUTH|${_deviceName.value}|${pin ?: ""}")
                val resp = reader.readLine() ?: throw Exception("Server closed connection")

                if (resp.startsWith("AUTH_OK|")) {
                    val serverName = resp.removePrefix("AUTH_OK|")
                    _connectionState.value = ClientConnectionState.Connected(serverName, server.host)

                    // Listen loop for incoming server events
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        val parts = line.split("|", limit = 2)
                        when (parts[0]) {
                            "SONG" -> {
                                val rawData = parts.getOrNull(1) ?: continue
                                val json = if (rawData.startsWith("{")) {
                                    rawData
                                } else {
                                    String(android.util.Base64.decode(rawData, android.util.Base64.DEFAULT), Charsets.UTF_8)
                                }
                                val payload = LiveSongPayload.fromJson(json)
                                if (payload.rawContent.isNotBlank()) {
                                    cacheRawSheet(payload.sourceUrl, payload.rawContent)
                                }
                                val ctx = appContext
                                if (ctx != null) {
                                    val entity = com.chordapp.catalog.CatalogSongEntity(
                                        id = null,
                                        source = payload.source.ifBlank { "P2P" },
                                        sourceUrl = payload.sourceUrl,
                                        title = payload.title,
                                        artist = payload.artist,
                                        titleSlug = payload.titleSlug.ifBlank { com.chordapp.catalog.catalogSlug(payload.title) },
                                        artistSlug = payload.artistSlug.ifBlank { com.chordapp.catalog.catalogSlug(payload.artist) },
                                        versionName = payload.versionName,
                                        rating = null,
                                        votes = null,
                                        keyBase = payload.effectiveKey,
                                        difficulty = null,
                                        category = null,
                                        region = "ID",
                                        chords = "",
                                        chordCount = 0,
                                        lineCount = payload.rawContent.lines().size,
                                        content = payload.rawContent,
                                        contentHash = "",
                                        scrapedAt = ""
                                    )
                                    try {
                                        com.chordapp.catalog.CatalogDatabase.getDatabase(ctx).catalogDao().insertSong(entity)
                                    } catch (_: Exception) {}
                                }
                                _incomingSong.emit(payload)
                            }
                            "TRANSPOSE" -> {
                                val delta = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                _incomingTranspose.emit(delta)
                            }
                            "STANDBY" -> {
                                _standbyTrigger.emit(Unit)
                            }
                        }
                    }
                } else if (resp.startsWith("ERROR|INVALID_PIN")) {
                    _connectionState.value = ClientConnectionState.Error("PIN Salah")
                    withContext(Dispatchers.Main) { onAuthFailed("PIN Salah") }
                    sock.close()
                    return@launch
                } else {
                    throw Exception(resp)
                }
            } catch (e: Exception) {
                if (_currentMode.value == BandMode.CLIENT) {
                    _connectionState.value = ClientConnectionState.Error(e.message ?: "Gagal terhubung")
                }
            }
        }
    }

    fun disconnectClient() {
        try { clientSocket?.close() } catch (_: Exception) {}
        clientSocket = null
        _connectionState.value = ClientConnectionState.Disconnected
    }
}
