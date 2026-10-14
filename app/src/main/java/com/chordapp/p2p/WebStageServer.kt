package com.chordapp.p2p

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Lightweight embedded HTTP server running on port 8990 during Server Mode.
 * Serves a real-time responsive Dark Mode Web Stage Mirror for iPhone Safari,
 * Android devices, and laptops on the local Wi-Fi / Hotspot.
 * Zero external internet required.
 */
class WebStageServer(private val port: Int = 8990) {

    companion object {
        private const val TAG = "WebStageServer"
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var activePayload: LiveSongPayload? = null

    @Volatile
    private var activeTranspose: Int = 0

    @Volatile
    private var isStandby: Boolean = true

    private val sseClients = CopyOnWriteArrayList<SseClient>()

    private class SseClient(
        val socket: Socket,
        val writer: PrintWriter,
        val outputStream: OutputStream
    ) {
        fun send(event: String, data: String) {
            try {
                synchronized(outputStream) {
                    writer.print("event: $event\n")
                    writer.print("data: $data\n\n")
                    writer.flush()
                }
            } catch (_: Exception) {
                close()
            }
        }

        fun ping() {
            try {
                synchronized(outputStream) {
                    writer.print(": ping\n\n")
                    writer.flush()
                }
            } catch (_: Exception) {
                close()
            }
        }

        fun close() {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    fun start() {
        stop()
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                runCatching { Log.i(TAG, "WebStageServer started on port $port") }

                while (isActive) {
                    val clientSocket = serverSocket?.accept() ?: break
                    clientSocket.tcpNoDelay = true
                    launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                if (e !is SocketException || isActive) {
                    runCatching { Log.w(TAG, "WebStageServer error: ${e.message}") }
                }
            }
        }

        // Heartbeat / Keepalive for SSE clients
        scope.launch {
            while (isActive) {
                delay(15000)
                val deadClients = mutableListOf<SseClient>()
                for (client in sseClients) {
                    if (client.socket.isClosed || !client.socket.isConnected) {
                        deadClients.add(client)
                    } else {
                        client.ping()
                    }
                }
                if (deadClients.isNotEmpty()) {
                    sseClients.removeAll(deadClients.toSet())
                }
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        serverJob = null

        sseClients.forEach { it.close() }
        sseClients.clear()

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        runCatching { Log.i(TAG, "WebStageServer stopped") }
    }

    fun broadcastSong(payload: LiveSongPayload) {
        activePayload = payload
        activeTranspose = payload.transpose
        isStandby = false
        val json = buildSongJson(payload, activeTranspose)
        sendSseToAll("SONG", json)
    }

    fun broadcastTranspose(transpose: Int) {
        activeTranspose = transpose
        val obj = JSONObject().apply {
            put("transpose", transpose)
        }
        sendSseToAll("TRANSPOSE", obj.toString())
    }

    fun broadcastStandby() {
        isStandby = true
        activePayload = null
        sendSseToAll("STANDBY", "{}")
    }

    private fun sendSseToAll(event: String, data: String) {
        val dead = mutableListOf<SseClient>()
        for (client in sseClients) {
            try {
                client.send(event, data)
            } catch (_: Exception) {
                dead.add(client)
            }
        }
        if (dead.isNotEmpty()) {
            sseClients.removeAll(dead.toSet())
        }
    }

    private fun buildSongJson(payload: LiveSongPayload, transpose: Int): String {
        return JSONObject().apply {
            put("type", "SONG")
            put("title", payload.title)
            put("artist", payload.artist)
            put("source", payload.source)
            put("sourceUrl", payload.sourceUrl)
            put("versionName", payload.versionName ?: "")
            put("effectiveKey", payload.effectiveKey ?: "")
            put("transpose", transpose)
            put("capo", payload.capo)
            put("rawContent", payload.rawContent)
            put("timestamp", payload.timestamp)
        }.toString()
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            val line = reader.readLine() ?: return socket.close()
            val parts = line.split(" ")
            if (parts.size < 2) return socket.close()

            val method = parts[0].uppercase()
            val path = parts[1].substringBefore("?")

            when {
                method == "OPTIONS" -> {
                    val out = socket.getOutputStream()
                    val response = "HTTP/1.1 204 No Content\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Access-Control-Allow-Methods: GET, OPTIONS\r\n" +
                            "Access-Control-Allow-Headers: Content-Type\r\n" +
                            "Connection: close\r\n\r\n"
                    out.write(response.toByteArray(StandardCharsets.UTF_8))
                    out.flush()
                    socket.close()
                }

                path == "/stream" -> {
                    handleSseStream(socket)
                }

                path == "/api/current" -> {
                    handleApiCurrent(socket)
                }

                else -> {
                    handleHtmlIndex(socket)
                }
            }
        } catch (_: Exception) {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun handleSseStream(socket: Socket) {
        val outputStream = socket.getOutputStream()
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/event-stream; charset=utf-8\r\n" +
                "Cache-Control: no-cache, no-transform\r\n" +
                "Connection: keep-alive\r\n" +
                "Access-Control-Allow-Origin: *\r\n\r\n"
        outputStream.write(headers.toByteArray(StandardCharsets.UTF_8))
        outputStream.flush()

        val writer = PrintWriter(outputStream, true)
        val client = SseClient(socket, writer, outputStream)
        sseClients.add(client)

        // Send initial state immediately
        if (isStandby || activePayload == null) {
            client.send("STANDBY", "{}")
        } else {
            val json = buildSongJson(activePayload!!, activeTranspose)
            client.send("SONG", json)
        }
    }

    private fun handleApiCurrent(socket: Socket) {
        val out = socket.getOutputStream()
        val json = if (isStandby || activePayload == null) {
            JSONObject().apply {
                put("state", "standby")
            }.toString()
        } else {
            JSONObject().apply {
                put("state", "song")
                put("title", activePayload?.title ?: "")
                put("artist", activePayload?.artist ?: "")
                put("effectiveKey", activePayload?.effectiveKey ?: "")
                put("transpose", activeTranspose)
                put("capo", activePayload?.capo ?: 0)
                put("rawContent", activePayload?.rawContent ?: "")
            }.toString()
        }

        val bodyBytes = json.toByteArray(StandardCharsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Cache-Control: no-cache\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n" +
                "Connection: close\r\n\r\n"

        out.write(headers.toByteArray(StandardCharsets.UTF_8))
        out.write(bodyBytes)
        out.flush()
        socket.close()
    }

    private fun handleHtmlIndex(socket: Socket) {
        val out = socket.getOutputStream()
        val htmlBytes = HTML_PLAYER_PAGE.toByteArray(StandardCharsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Cache-Control: no-cache\r\n" +
                "Content-Length: ${htmlBytes.size}\r\n" +
                "Connection: close\r\n\r\n"

        out.write(headers.toByteArray(StandardCharsets.UTF_8))
        out.write(htmlBytes)
        out.flush()
        socket.close()
    }

    private val HTML_PLAYER_PAGE = """
<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
  <title>KordApp — Web Stage Mirror</title>
  <style>
    :root {
      --bg: #0C0E12;
      --card-bg: #13171E;
      --raised-bg: #1A202A;
      --border: #222B38;
      --gold: #E5A93C;
      --gold-bg: rgba(229, 169, 60, 0.14);
      --chord-color: #FFB74D;
      --text-primary: #F5F8FF;
      --text-muted: #7D8A99;
      --green: #22C55E;
    }
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      -webkit-tap-highlight-color: transparent;
    }
    body {
      background: var(--bg);
      color: var(--text-primary);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      overflow-x: hidden;
      user-select: none;
    }
    /* Header Bar */
    header {
      position: sticky;
      top: 0;
      z-index: 100;
      background: var(--card-bg);
      border-bottom: 1px solid var(--border);
      padding: 12px 18px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      box-shadow: 0 4px 20px rgba(0,0,0,0.5);
    }
    .header-title-box h1 {
      font-size: 16px;
      font-weight: 800;
      letter-spacing: -0.2px;
      color: var(--text-primary);
    }
    .header-title-box p {
      font-size: 12px;
      color: var(--gold);
      font-weight: 600;
      margin-top: 2px;
    }
    .live-pill {
      background: rgba(34, 197, 94, 0.12);
      border: 1px solid rgba(34, 197, 94, 0.4);
      padding: 5px 12px;
      border-radius: 20px;
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 11px;
      font-weight: 700;
      color: var(--green);
    }
    .pulse-dot {
      width: 7px;
      height: 7px;
      background: var(--green);
      border-radius: 50%;
      box-shadow: 0 0 8px var(--green);
      animation: pulseAnim 1.5s infinite;
    }
    @keyframes pulseAnim {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.4; transform: scale(0.8); }
    }

    /* Container Views */
    #viewStandby {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 40px 20px;
      text-align: center;
      gap: 24px;
    }
    .radar-container {
      position: relative;
      width: 140px;
      height: 140px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .radar-circle {
      position: absolute;
      border-radius: 50%;
      border: 1.5px solid var(--gold);
      opacity: 0;
      animation: radarPulse 2.8s infinite cubic-bezier(0.2, 0.6, 0.4, 1);
    }
    .radar-circle:nth-child(1) { width: 140px; height: 140px; animation-delay: 0s; }
    .radar-circle:nth-child(2) { width: 100px; height: 100px; animation-delay: 0.9s; }
    .radar-circle:nth-child(3) { width: 60px; height: 60px; animation-delay: 1.8s; }
    @keyframes radarPulse {
      0% { transform: scale(0.6); opacity: 0.8; }
      80% { transform: scale(1.3); opacity: 0; }
      100% { transform: scale(1.4); opacity: 0; }
    }
    .radar-core {
      width: 72px;
      height: 72px;
      background: var(--gold-bg);
      border: 2px solid var(--gold);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--gold);
      font-size: 34px;
      z-index: 10;
      box-shadow: 0 0 25px rgba(229, 169, 60, 0.35);
    }
    .standby-title {
      font-size: 22px;
      font-weight: 800;
      color: var(--text-primary);
    }
    .standby-subtitle {
      font-size: 14px;
      color: var(--text-muted);
      max-width: 320px;
      line-height: 1.4;
    }

    /* Player View */
    #viewPlayer {
      display: none;
      flex: 1;
      flex-direction: column;
      padding: 18px 20px 100px 20px;
      max-width: 800px;
      margin: 0 auto;
      width: 100%;
    }
    .song-meta-bar {
      display: flex;
      gap: 8px;
      margin-bottom: 16px;
      flex-wrap: wrap;
    }
    .meta-badge {
      background: var(--raised-bg);
      border: 1px solid var(--border);
      padding: 4px 10px;
      border-radius: 8px;
      font-size: 12px;
      font-weight: 600;
      color: var(--gold);
    }
    #sheetContent {
      font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, "Liberation Mono", monospace;
      font-size: 15px;
      line-height: 1.65;
      white-space: pre-wrap;
      word-break: break-word;
    }
    .chord {
      color: var(--chord-color);
      font-weight: 700;
    }
    .section-header {
      color: var(--gold);
      font-weight: 700;
      margin-top: 12px;
      display: block;
    }

    /* Floating Dock Controls */
    .dock-bar {
      position: fixed;
      bottom: 16px;
      left: 50%;
      transform: translateX(-50%);
      background: rgba(19, 23, 30, 0.94);
      backdrop-filter: blur(12px);
      -webkit-backdrop-filter: blur(12px);
      border: 1px solid var(--border);
      border-radius: 30px;
      padding: 6px 14px;
      display: flex;
      align-items: center;
      gap: 12px;
      z-index: 1000;
      box-shadow: 0 10px 30px rgba(0,0,0,0.6);
    }
    .dock-btn {
      background: transparent;
      border: none;
      color: var(--text-primary);
      font-size: 13px;
      font-weight: 700;
      padding: 6px 12px;
      border-radius: 20px;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 6px;
      transition: background 0.15s;
    }
    .dock-btn:active {
      background: rgba(255,255,255,0.1);
    }
    .dock-btn.active {
      background: var(--gold);
      color: #0C0E12;
    }
  </style>
</head>
<body>

  <header>
    <div class="header-title-box">
      <h1 id="headerTitle">Standby Panggung</h1>
      <p id="headerSubtitle">KordApp Stage Mirror</p>
    </div>
    <div class="live-pill">
      <div class="pulse-dot"></div>
      <span id="syncText">Live Sync</span>
    </div>
  </header>

  <!-- Standby State -->
  <div id="viewStandby">
    <div class="radar-container">
      <div class="radar-circle"></div>
      <div class="radar-circle"></div>
      <div class="radar-circle"></div>
      <div class="radar-core">♫</div>
    </div>
    <div>
      <h2 class="standby-title">Standby Panggung</h2>
      <p class="standby-subtitle">Menunggu lagu dari pemandu musik di panggung...</p>
    </div>
  </div>

  <!-- Player State -->
  <div id="viewPlayer">
    <div class="song-meta-bar" id="metaBar"></div>
    <div id="sheetContent"></div>
  </div>

  <!-- Floating Dock -->
  <div class="dock-bar" id="dockBar" style="display:none;">
    <button class="dock-btn" id="btnScroll" onclick="toggleScroll()">▶ Scroll</button>
    <button class="dock-btn" onclick="adjustTranspose(-1)">♭ −1</button>
    <span id="dockTranspose" class="dock-btn" style="pointer-events:none;color:var(--gold);padding:6px 2px;">0</span>
    <button class="dock-btn" onclick="adjustTranspose(1)">♯ +1</button>
    <button class="dock-btn" onclick="adjustFont(-1)">A−</button>
    <button class="dock-btn" onclick="adjustFont(1)">A+</button>
    <button class="dock-btn" id="btnWake" onclick="requestWakeLock()">☀ Layar Nyala</button>
  </div>

  <script>
    let currentData = null;
    let scrollInterval = null;
    let scrollSpeed = 1.0;
    let fontSize = 15;
    let wakeLock = null;

    const notesSharp = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"];
    const notesFlat = ["C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"];

    function transposeSingle(chord, delta) {
      if (!chord || delta === 0) return chord;
      let rootLen = 1;
      if (chord.length > 1 && (chord[1] === '#' || chord[1] === 'b')) {
        rootLen = 2;
      }
      const root = chord.substring(0, rootLen);
      const suffix = chord.substring(rootLen);
      const isFlat = root.indexOf('b') !== -1;
      const noteArray = isFlat ? notesFlat : notesSharp;
      let index = -1;
      for (let i = 0; i < noteArray.length; i++) {
        if (noteArray[i].toUpperCase() === root.toUpperCase()) {
          index = i;
          break;
        }
      }
      if (index === -1) return chord;
      let newIndex = (index + delta) % 12;
      if (newIndex < 0) newIndex += 12;
      const outArray = isFlat ? notesFlat : notesSharp;
      return outArray[newIndex] + suffix;
    }

    function transposeChord(chord, delta) {
      if (!chord || delta === 0) return chord;
      if (chord.indexOf('/') !== -1) {
        const parts = chord.split('/');
        if (parts.length === 2 && parts[0] && parts[1]) {
          return transposeSingle(parts[0].trim(), delta) + '/' + transposeSingle(parts[1].trim(), delta);
        }
      }
      return transposeSingle(chord, delta);
    }

    function isChordSymbol(token) {
      if (!token) return false;
      const clean = token.replace(/^[-\u2013~(]+/, '').replace(/[)]+$/, '').trim();
      return /^[A-G][b#]?(?:m|maj|min|dim|aug|sus|add)?[0-9]*(?:\/[A-G][b#]?)?$/i.test(clean);
    }

    function adjustTranspose(delta) {
      if (!currentData) return;
      currentData.transpose = (currentData.transpose || 0) + delta;
      updateUi(currentData);
    }

    // WakeLock API
    async function requestWakeLock() {
      try {
        if ('wakeLock' in navigator) {
          wakeLock = await navigator.wakeLock.request('screen');
          document.getElementById('btnWake').classList.add('active');
        }
      } catch (err) {
        console.log("WakeLock error: " + err);
      }
    }

    function adjustFont(delta) {
      fontSize = Math.max(11, Math.min(28, fontSize + delta));
      document.getElementById('sheetContent').style.fontSize = fontSize + 'px';
    }

    function toggleScroll() {
      const btn = document.getElementById('btnScroll');
      if (scrollInterval) {
        clearInterval(scrollInterval);
        scrollInterval = null;
        btn.innerHTML = '▶ Scroll';
        btn.classList.remove('active');
      } else {
        btn.innerHTML = '❚❚ Pause';
        btn.classList.add('active');
        scrollInterval = setInterval(() => {
          window.scrollBy({ top: 1, behavior: 'smooth' });
        }, 40 / scrollSpeed);
      }
    }

    function escapeHtml(text) {
      const div = document.createElement('div');
      div.innerText = text;
      return div.innerHTML;
    }

    function renderSheet(rawContent, delta) {
      if (!rawContent) return "";
      const lines = rawContent.split(/\r?\n/);
      let html = "";
      const bracketChordRegex = /\[([A-G][b#]?[^\]]*)\]/g;
      const sectionHeaderRegex = /^\s*\[(Intro|Verse|Chorus|Bridge|Outro|Refrain|Pre-Chorus|Solo|Interlude|Ending|Tab|Instrumental|Hook|Drop|Bait|Reff)[^\]]*\]\s*$/i;
      const chordRegex = /\b[A-G][b#]?(?:m|maj|min|dim|aug|sus|add)?[0-9]*(?:\/[A-G][b#]?)?\b/g;

      for (let line of lines) {
        let trimmed = line.trim();
        if (sectionHeaderRegex.test(trimmed)) {
          html += '<span class="section-header">' + escapeHtml(trimmed) + '</span>\n';
        } else if (bracketChordRegex.test(line)) {
          bracketChordRegex.lastIndex = 0;
          let highlighted = escapeHtml(line).replace(/\[([A-G][b#]?[^\]]*)\]/g, (match, chordInside) => {
            if (isChordSymbol(chordInside)) {
              const tr = transposeChord(chordInside, delta || 0);
              return '<span class="chord">[' + escapeHtml(tr) + ']</span>';
            }
            return match;
          });
          html += highlighted + '\n';
        } else {
          // Plain chord line
          const tokens = trimmed.split(/\s+/).filter(t => t.length > 0);
          const chordCount = tokens.filter(t => isChordSymbol(t)).length;
          if (tokens.length > 0 && chordCount >= 1 && chordCount >= tokens.length * 0.5) {
            let highlighted = escapeHtml(line).replace(chordRegex, (m) => {
              const tr = transposeChord(m, delta || 0);
              return '<span class="chord">' + escapeHtml(tr) + '</span>';
            });
            html += highlighted + '\n';
          } else {
            html += escapeHtml(line) + '\n';
          }
        }
      }
      return html;
    }

    function updateUi(data) {
      if (!data || data.type === 'STANDBY' || data.state === 'standby') {
        document.getElementById('viewStandby').style.display = 'flex';
        document.getElementById('viewPlayer').style.display = 'none';
        document.getElementById('dockBar').style.display = 'none';
        document.getElementById('headerTitle').innerText = 'Standby Panggung';
        document.getElementById('headerSubtitle').innerText = 'KordApp Stage Mirror';
        if (scrollInterval) toggleScroll();
        return;
      }

      currentData = data;
      const tr = data.transpose || 0;
      const trStr = tr > 0 ? ('+' + tr) : ('' + tr);

      document.getElementById('viewStandby').style.display = 'none';
      document.getElementById('viewPlayer').style.display = 'flex';
      document.getElementById('dockBar').style.display = 'flex';

      document.getElementById('headerTitle').innerText = data.title || 'Tanpa Judul';
      document.getElementById('headerSubtitle').innerText = data.artist || 'Artis';

      const dockTr = document.getElementById('dockTranspose');
      if (dockTr) dockTr.innerText = trStr;

      const metaBar = document.getElementById('metaBar');
      let badges = '';
      if (data.effectiveKey) {
        const keyTransposed = transposeChord(data.effectiveKey, tr);
        badges += '<span class="meta-badge">Key: ' + escapeHtml(keyTransposed) + '</span>';
      }
      badges += '<span class="meta-badge">Transpose: ' + trStr + '</span>';
      if (data.capo && data.capo > 0) badges += '<span class="meta-badge">Capo: ' + data.capo + '</span>';
      metaBar.innerHTML = badges;

      document.getElementById('sheetContent').innerHTML = renderSheet(data.rawContent || '', tr);
      requestWakeLock();
    }

    // Connect to SSE stream
    function connectSse() {
      const evtSource = new EventSource('/stream');

      evtSource.onmessage = (e) => {
        try {
          const parsed = JSON.parse(e.data);
          updateUi(parsed);
        } catch(err) {}
      };

      evtSource.addEventListener('SONG', (e) => {
        try { updateUi(JSON.parse(e.data)); } catch(err) {}
      });

      evtSource.addEventListener('TRANSPOSE', (e) => {
        try {
          const parsed = JSON.parse(e.data);
          if (currentData) {
            currentData.transpose = parsed.transpose;
            updateUi(currentData);
          }
        } catch(err) {}
      });

      evtSource.addEventListener('STANDBY', () => {
        updateUi({ state: 'standby' });
      });

      evtSource.onerror = () => {
        evtSource.close();
        document.getElementById('syncText').innerText = 'Menyambung...';
        setTimeout(connectSse, 3000);
      };

      evtSource.onopen = () => {
        document.getElementById('syncText').innerText = 'Live Sync';
      };
    }

    // Polling fallback
    setInterval(async () => {
      try {
        const res = await fetch('/api/current');
        if (res.ok) {
          const data = await res.json();
          if (data.state === 'standby' && document.getElementById('viewStandby').style.display === 'none') {
            updateUi({ state: 'standby' });
          }
        }
      } catch(err) {}
    }, 4000);

    connectSse();
  </script>
</body>
</html>
    """.trimIndent()
}
