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
      position: relative;
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
      line-height: 1.6;
      user-select: text;
    }
    .blank-line {
      height: 1.2em;
    }
    .section-header {
      color: var(--gold);
      font-weight: 800;
      font-size: 1.05em;
      margin-top: 20px;
      margin-bottom: 6px;
      display: block;
      letter-spacing: 0.2px;
    }
    .chord-flow-line {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-end;
      margin-bottom: 3px;
    }
    .chord-cell {
      display: inline-flex;
      flex-direction: column;
      align-items: flex-start;
      white-space: pre;
    }
    .chord-cell .chord {
      color: var(--chord-color);
      font-weight: 700;
      font-size: 1em;
      line-height: 1.25;
      min-height: 1.25em;
    }
    .chord-cell .lyric {
      color: var(--text-primary);
      font-size: 1em;
      line-height: 1.4;
    }
    .chord-cell .chord-empty,
    .chord-cell .lyric-empty {
      opacity: 0;
      user-select: none;
    }
    .chord-only-row {
      white-space: pre;
      margin-bottom: 3px;
    }
    .chord-only-row .chord {
      color: var(--chord-color);
      font-weight: 700;
    }
    .chord-spacer {
      white-space: pre;
    }
    .bar-line {
      font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;
      font-weight: 600;
      margin-bottom: 6px;
      white-space: pre-wrap;
      color: var(--text-primary);
    }
    .bar-line .chord {
      color: var(--chord-color);
      font-weight: 700;
    }
    .lyric-line {
      font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;
      color: var(--text-primary);
      line-height: 1.45;
      white-space: pre-wrap;
      margin-bottom: 3px;
    }

    /* Page Flip Container */
    .pageflip-container {
      width: 100%;
      min-height: calc(100vh - 180px);
      display: flex;
      flex-direction: column;
      justify-content: flex-start;
      animation: fadeInPage 0.18s ease-out;
    }
    @keyframes fadeInPage {
      from { opacity: 0; transform: translateY(3px); }
      to { opacity: 1; transform: translateY(0); }
    }

    /* Floating Dock Controls */
    .dock-bar {
      position: fixed;
      bottom: calc(16px + env(safe-area-inset-bottom, 0px));
      left: 50%;
      transform: translateX(-50%);
      background: rgba(18, 22, 29, 0.92);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      border: 1px solid rgba(229, 169, 60, 0.28);
      border-radius: 40px;
      padding: 5px 10px;
      display: flex;
      align-items: center;
      gap: 4px;
      z-index: 1000;
      box-shadow: 0 10px 32px rgba(0,0,0,0.7), 0 0 16px rgba(229, 169, 60, 0.1);
      max-width: 98vw;
    }
    .dock-sep {
      width: 1px;
      height: 18px;
      background: rgba(255, 255, 255, 0.12);
      margin: 0 2px;
    }
    .dock-btn {
      background: transparent;
      border: none;
      color: #D2D9E5;
      width: 36px;
      height: 36px;
      border-radius: 50%;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.18s ease;
      flex-shrink: 0;
    }
    .dock-btn:hover {
      background: rgba(255, 255, 255, 0.08);
      color: #FFFFFF;
    }
    .dock-btn:active {
      transform: scale(0.9);
      background: rgba(229, 169, 60, 0.2);
    }
    .dock-btn.active {
      background: var(--gold);
      color: #0C0E12;
      box-shadow: 0 0 14px rgba(229, 169, 60, 0.5);
    }
    .dock-btn.always-lit {
      color: var(--gold);
      background: rgba(229, 169, 60, 0.14);
      border: 1px solid rgba(229, 169, 60, 0.35);
      box-shadow: 0 0 10px rgba(229, 169, 60, 0.25);
    }
    .dock-btn.always-lit:active {
      background: rgba(229, 169, 60, 0.3);
    }

    /* Speed Control Group inside Dock Bar */
    .dock-speed-group {
      display: flex;
      align-items: center;
      gap: 1px;
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid rgba(229, 169, 60, 0.28);
      border-radius: 18px;
      padding: 1px 3px;
      margin: 0 1px;
    }
    .dock-btn-stepper {
      background: transparent;
      border: none;
      color: #D2D9E5;
      width: 20px;
      height: 26px;
      border-radius: 12px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 700;
      transition: all 0.15s;
    }
    .dock-btn-stepper:hover {
      color: #FFF;
      background: rgba(255,255,255,0.1);
    }
    .dock-btn-stepper:active {
      transform: scale(0.88);
    }
    .dock-speed-badge {
      background: transparent;
      border: none;
      color: var(--gold);
      font-size: 11.5px;
      font-weight: 800;
      padding: 0 3px;
      min-width: 32px;
      text-align: center;
      cursor: pointer;
      letter-spacing: -0.3px;
      user-select: none;
      transition: text-shadow 0.15s;
    }
    .dock-speed-badge:hover {
      text-shadow: 0 0 8px rgba(229, 169, 60, 0.6);
    }

    .transpose-badge {
      font-size: 13px;
      font-weight: 800;
      color: var(--gold);
      min-width: 20px;
      text-align: center;
      letter-spacing: -0.5px;
      user-select: none;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    /* Vertical Speed Rail (Slider on right screen edge) */
    .speed-rail-container {
      position: fixed;
      right: 14px;
      top: 50%;
      transform: translateY(-50%) translateX(70px);
      width: 42px;
      height: 220px;
      background: rgba(18, 22, 29, 0.88);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      border: 1px solid rgba(229, 169, 60, 0.35);
      border-radius: 22px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: space-between;
      padding: 8px 0;
      z-index: 999;
      box-shadow: 0 8px 30px rgba(0,0,0,0.6), 0 0 14px rgba(229, 169, 60, 0.15);
      opacity: 0;
      pointer-events: none;
      transition: opacity 0.25s ease, transform 0.25s ease;
      user-select: none;
    }
    .speed-rail-container.visible {
      opacity: 1;
      pointer-events: auto;
      transform: translateY(-50%) translateX(0);
    }
    .rail-btn {
      background: transparent;
      border: none;
      color: var(--gold);
      font-size: 18px;
      font-weight: 800;
      width: 32px;
      height: 28px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      transition: all 0.15s;
    }
    .rail-btn:active {
      background: rgba(229, 169, 60, 0.2);
      transform: scale(0.9);
    }
    .rail-track {
      position: relative;
      width: 6px;
      flex: 1;
      margin: 6px 0;
      background: rgba(255, 255, 255, 0.12);
      border-radius: 3px;
      cursor: pointer;
      touch-action: none;
    }
    .rail-track-fill {
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      background: var(--gold);
      border-radius: 3px;
      box-shadow: 0 0 8px rgba(229, 169, 60, 0.6);
      pointer-events: none;
    }
    .rail-thumb {
      position: absolute;
      left: 50%;
      transform: translate(-50%, 50%);
      width: 18px;
      height: 18px;
      border-radius: 50%;
      background: var(--gold);
      border: 2px solid #0C0E12;
      box-shadow: 0 0 10px rgba(229, 169, 60, 0.8);
      pointer-events: none;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .rail-thumb-tip {
      position: absolute;
      right: 26px;
      background: rgba(18, 22, 29, 0.95);
      border: 1px solid var(--gold);
      color: var(--gold);
      font-size: 11px;
      font-weight: 800;
      padding: 2px 6px;
      border-radius: 8px;
      white-space: nowrap;
      pointer-events: none;
      box-shadow: 0 4px 12px rgba(0,0,0,0.5);
    }

    /* Page Flip Dots Rail (Clean minimal dots without text numbers) */
    .pageflip-rail {
      position: fixed;
      right: 14px;
      top: 50%;
      transform: translateY(-50%) translateX(70px);
      background: rgba(18, 22, 29, 0.88);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      border: 1px solid rgba(229, 169, 60, 0.35);
      border-radius: 20px;
      padding: 10px 6px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 7px;
      z-index: 999;
      box-shadow: 0 8px 30px rgba(0,0,0,0.6), 0 0 14px rgba(229, 169, 60, 0.15);
      opacity: 0;
      pointer-events: none;
      transition: opacity 0.25s ease, transform 0.25s ease;
      max-height: 70vh;
      overflow-y: auto;
    }
    .pageflip-rail.visible {
      opacity: 1;
      pointer-events: auto;
      transform: translateY(-50%) translateX(0);
    }
    .pageflip-dot {
      width: 8px;
      height: 8px;
      border-radius: 4px;
      background: rgba(255, 255, 255, 0.22);
      border: 1px solid rgba(255, 255, 255, 0.1);
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .pageflip-dot:hover {
      background: rgba(229, 169, 60, 0.6);
    }
    .pageflip-dot.active {
      width: 8px;
      height: 22px;
      background: var(--gold);
      border-color: var(--gold);
      box-shadow: 0 0 10px rgba(229, 169, 60, 0.8);
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

  <!-- Floating Dock (Full Icon Controls) -->
  <div class="dock-bar" id="dockBar" style="display:none;">
    <!-- Auto Scroll (Play / Pause) -->
    <button class="dock-btn" id="btnScroll" onclick="toggleScroll()" title="Auto Scroll (Play / Pause)">
      <svg id="iconScrollPlay" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5.14v13.72a1 1 0 0 0 1.5.86l11.04-6.86a1 1 0 0 0 0-1.72L9.5 4.28a1 1 0 0 0-1.5.86z"/></svg>
      <svg id="iconScrollPause" style="display:none;" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="5" width="4" height="14" rx="1"/><rect x="14" y="5" width="4" height="14" rx="1"/></svg>
    </button>

    <!-- Speed Controls (Stepper - / Badge / Stepper +) -->
    <div class="dock-speed-group" title="Kecepatan Auto Scroll">
      <button class="dock-btn-stepper" onclick="adjustSpeed(-0.2)" title="Perlambat Scroll">−</button>
      <button class="dock-speed-badge" id="dockSpeed" onclick="cycleSpeedPreset()" title="Klik untuk ganti preset kecepatan">1.0×</button>
      <button class="dock-btn-stepper" onclick="adjustSpeed(0.2)" title="Percepat Scroll">+</button>
    </div>

    <!-- Page Flip Toggle (Open Book) -->
    <button class="dock-btn" id="btnPageFlip" onclick="togglePageFlip()" title="Page Flip (Buka Halaman per Slide)">
      <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
        <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
      </svg>
    </button>

    <div class="dock-sep"></div>

    <!-- Transpose Controls (Flat, Badge, Sharp) -->
    <button class="dock-btn" onclick="adjustTranspose(-1)" title="Transpose Turun (Flat)">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><text x="6" y="17" font-size="19" font-weight="900" font-family="-apple-system, sans-serif">♭</text></svg>
    </button>
    <span id="dockTranspose" class="transpose-badge" title="Status Transpose">0</span>
    <button class="dock-btn" onclick="adjustTranspose(1)" title="Transpose Naik (Sharp)">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><text x="5" y="17" font-size="18" font-weight="900" font-family="-apple-system, sans-serif">♯</text></svg>
    </button>

    <div class="dock-sep"></div>

    <!-- Font Size Controls (A-, A+) -->
    <button class="dock-btn" onclick="adjustFont(-1)" title="Kecilkan Huruf">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><text x="1" y="17" font-size="14" font-weight="900" font-family="-apple-system, sans-serif">A</text><rect x="13" y="12" width="9" height="2.5" rx="1.25"/></svg>
    </button>
    <button class="dock-btn" onclick="adjustFont(1)" title="Besarkan Huruf">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><text x="1" y="17" font-size="14" font-weight="900" font-family="-apple-system, sans-serif">A</text><rect x="13" y="12" width="9" height="2.5" rx="1.25"/><rect x="16.25" y="8.75" width="2.5" height="9" rx="1.25"/></svg>
    </button>

    <div class="dock-sep"></div>

    <!-- Layar Tetap Menyala (Always-Lit WakeLock Screen Indicator) -->
    <button class="dock-btn always-lit" id="btnWake" onclick="requestWakeLock()" title="Layar Tetap Menyala (Stage Awake)">
      <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4.5"/><line x1="12" y1="1.5" x2="12" y2="3.5"/><line x1="12" y1="20.5" x2="12" y2="22.5"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1.5" y1="12" x2="3.5" y2="12"/><line x1="20.5" y1="12" x2="22.5" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
    </button>

    <div class="dock-sep"></div>

    <!-- Full Screen Trigger -->
    <button class="dock-btn" id="btnFullscreen" onclick="toggleFullScreen()" title="Layar Penuh (Full Screen)">
      <svg id="iconFsEnter" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/></svg>
      <svg id="iconFsExit" style="display:none;" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3"/></svg>
    </button>
  </div>

  <!-- Vertical Speed Rail (Slider on right edge) -->
  <div class="speed-rail-container" id="speedRail">
    <button class="rail-btn" onclick="adjustSpeed(0.2)" title="Percepat">+</button>
    <div class="rail-track" id="speedRailTrack">
      <div class="rail-track-fill" id="speedRailFill"></div>
      <div class="rail-thumb" id="speedRailThumb">
        <span class="rail-thumb-tip" id="speedRailTip">1.0×</span>
      </div>
    </div>
    <button class="rail-btn" onclick="adjustSpeed(-0.2)" title="Perlambat">−</button>
  </div>

  <!-- Page Flip Dots Rail (Pills at right edge) -->
  <div class="pageflip-rail" id="pageFlipRail"></div>

  <script>
    let currentData = null;
    let isScrolling = false;
    let scrollAnimFrame = null;
    let lastScrollTime = 0;
    let scrollSubpixel = 0;
    let scrollSpeed = parseFloat(localStorage.getItem('kordapp_scroll_speed') || '1.0') || 1.0;
    let fontSize = parseInt(localStorage.getItem('kordapp_font_size') || '15') || 15;
    let isPageFlip = false;
    let currentPageIndex = 0;
    let pageList = [];
    let parsedSheetLines = [];
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

    const suffixAllowed = "0123456789#+-badgijmnsu()";

    function transposeChord(symbol, delta) {
      if (!symbol || delta === 0) return symbol;
      let prefix = "";
      let clean = symbol.trim();
      while (clean.length > 0 && (clean[0] === '-' || clean[0] === '\u2013' || clean[0] === '~')) {
        prefix += clean[0];
        clean = clean.substring(1).trim();
      }
      if (clean.indexOf('/') !== -1) {
        const parts = clean.split('/');
        if (parts.length === 2) {
          const m = parts[0] ? transposeSingle(parts[0], delta) : "";
          const b = parts[1] ? transposeSingle(parts[1], delta) : "";
          return prefix + (m ? m + '/' + b : '/' + b);
        }
      }
      return prefix + transposeSingle(clean, delta);
    }

    function isChordSymbol(s) {
      if (!s) return false;
      let token = s.trim();
      while (token.length > 0 && (token[0] === '-' || token[0] === '\u2013' || token[0] === '~')) {
        token = token.substring(1).trim();
      }
      if (token.startsWith('(') && token.endsWith(')')) {
        token = token.substring(1, token.length - 1).trim();
      }
      if (!token) return false;
      if (token.indexOf('/') !== -1) {
        const parts = token.split('/');
        if (parts.length !== 2) return false;
        const main = parts[0];
        const bass = parts[1];
        if (!main) return isChordSymbol(bass);
        return isChordSymbol(main) && isChordSymbol(bass);
      }
      let i = 0;
      if (i < token.length && token[i] >= 'A' && token[i] <= 'G') i++;
      else return false;
      if (i < token.length && (token[i] === '#' || token[i] === 'b')) i++;
      if (i === token.length) return true;
      const suffix = token.substring(i).toLowerCase();
      for (let c of suffix) {
        if (suffixAllowed.indexOf(c) === -1) return false;
      }
      let hasMarker = false;
      let letterCount = 0;
      for (let c of suffix) {
        if ((c >= '0' && c <= '9') || c === '#' || c === '+' || c === '-') hasMarker = true;
        if (c >= 'a' && c <= 'z') letterCount++;
      }
      if (!hasMarker && letterCount > 3) return false;
      return true;
    }

    function adjustTranspose(delta) {
      if (!currentData) return;
      currentData.transpose = (currentData.transpose || 0) + delta;
      updateUi(currentData);
    }

    // WakeLock API (Layar Tetap Menyala)
    async function requestWakeLock() {
      try {
        if ('wakeLock' in navigator) {
          wakeLock = await navigator.wakeLock.request('screen');
        }
      } catch (err) {
        console.log("WakeLock info: " + err);
      }
    }

    function adjustFont(delta) {
      fontSize = Math.max(11, Math.min(28, fontSize + delta));
      localStorage.setItem('kordapp_font_size', fontSize);
      document.getElementById('sheetContent').style.fontSize = fontSize + 'px';
      if (isPageFlip) {
        paginateSheet();
        renderCurrentPage();
      }
    }

    /* =========================================================
       SPEED ADJUSTMENT & SPEED RAIL
       ========================================================= */
    function setScrollSpeed(val) {
      scrollSpeed = Math.max(0.4, Math.min(3.0, Math.round(val * 10) / 10));
      localStorage.setItem('kordapp_scroll_speed', scrollSpeed);
      updateSpeedUI();
    }

    function adjustSpeed(delta) {
      setScrollSpeed(scrollSpeed + delta);
    }

    function cycleSpeedPreset() {
      const presets = [0.6, 0.8, 1.0, 1.3, 1.6, 2.0, 2.5];
      let next = presets[0];
      for (let i = 0; i < presets.length; i++) {
        if (presets[i] > scrollSpeed + 0.05) {
          next = presets[i];
          break;
        }
      }
      setScrollSpeed(next);
    }

    function updateSpeedUI() {
      const str = scrollSpeed.toFixed(1) + '\u00D7';
      const dockBadge = document.getElementById('dockSpeed');
      if (dockBadge) dockBadge.innerText = str;

      const tip = document.getElementById('speedRailTip');
      if (tip) tip.innerText = str;

      const pct = Math.max(0, Math.min(1, (scrollSpeed - 0.4) / (3.0 - 0.4)));
      const fill = document.getElementById('speedRailFill');
      if (fill) fill.style.height = (pct * 100) + '%';
      const thumb = document.getElementById('speedRailThumb');
      if (thumb) thumb.style.bottom = (pct * 100) + '%';
    }

    function initSpeedRail() {
      const track = document.getElementById('speedRailTrack');
      if (!track) return;
      let isDragging = false;

      function updateFromPointer(clientY) {
        const rect = track.getBoundingClientRect();
        const height = rect.height;
        if (height <= 0) return;
        const relY = clientY - rect.top;
        const clampedY = Math.max(0, Math.min(height, relY));
        const pct = 1 - (clampedY / height);
        const speed = 0.4 + pct * (3.0 - 0.4);
        setScrollSpeed(speed);
      }

      track.addEventListener('pointerdown', (e) => {
        isDragging = true;
        track.setPointerCapture(e.pointerId);
        updateFromPointer(e.clientY);
      });
      track.addEventListener('pointermove', (e) => {
        if (isDragging) updateFromPointer(e.clientY);
      });
      track.addEventListener('pointerup', (e) => { isDragging = false; });
      track.addEventListener('pointercancel', (e) => { isDragging = false; });
    }

    /* =========================================================
       AUTO SCROLL (SMOOTH ANIMATION FRAME)
       ========================================================= */
    function scrollLoop(time) {
      if (!isScrolling) return;
      if (!lastScrollTime) lastScrollTime = time;
      const dt = (time - lastScrollTime) / 1000;
      lastScrollTime = time;

      const pixelsPerSec = 32 * scrollSpeed;
      scrollSubpixel += pixelsPerSec * dt;

      if (scrollSubpixel >= 1) {
        const px = Math.floor(scrollSubpixel);
        window.scrollBy(0, px);
        scrollSubpixel -= px;
      }

      if ((window.innerHeight + window.scrollY) >= (document.documentElement.scrollHeight - 2)) {
        toggleScroll(false);
        return;
      }

      scrollAnimFrame = requestAnimationFrame(scrollLoop);
    }

    function toggleScroll(forceState) {
      const shouldStart = (forceState !== undefined) ? forceState : !isScrolling;
      const btn = document.getElementById('btnScroll');
      const iconPlay = document.getElementById('iconScrollPlay');
      const iconPause = document.getElementById('iconScrollPause');
      const rail = document.getElementById('speedRail');

      if (shouldStart) {
        if (isPageFlip) {
          togglePageFlip(false);
        }
        isScrolling = true;
        lastScrollTime = 0;
        scrollSubpixel = 0;
        if (iconPlay) iconPlay.style.display = 'none';
        if (iconPause) iconPause.style.display = 'block';
        if (btn) btn.classList.add('active');
        if (rail) rail.classList.add('visible');
        cancelAnimationFrame(scrollAnimFrame);
        scrollAnimFrame = requestAnimationFrame(scrollLoop);
        requestWakeLock();
      } else {
        isScrolling = false;
        cancelAnimationFrame(scrollAnimFrame);
        scrollAnimFrame = null;
        if (iconPlay) iconPlay.style.display = 'block';
        if (iconPause) iconPause.style.display = 'none';
        if (btn) btn.classList.remove('active');
        if (rail) rail.classList.remove('visible');
      }
    }

    /* =========================================================
       PAGE FLIP (DISCRETE SLIDE VIEWPORT PAGINATION)
       ========================================================= */
    function togglePageFlip(forceState) {
      const shouldActive = (forceState !== undefined) ? forceState : !isPageFlip;
      const btn = document.getElementById('btnPageFlip');
      const rail = document.getElementById('pageFlipRail');

      if (shouldActive) {
        if (isScrolling) {
          toggleScroll(false);
        }
        isPageFlip = true;
        if (btn) btn.classList.add('active');
        if (rail) rail.classList.add('visible');
        window.scrollTo({ top: 0, behavior: 'instant' });
        paginateSheet();
        renderCurrentPage();
      } else {
        isPageFlip = false;
        if (btn) btn.classList.remove('active');
        if (rail) rail.classList.remove('visible');
        renderContinuousSheet();
      }
    }

    function paginateSheet() {
      if (!parsedSheetLines || parsedSheetLines.length === 0) {
        pageList = [];
        return;
      }
      const fontLineHeight = fontSize * 1.6;
      const availableH = window.innerHeight - 170;
      const capacity = Math.max(6, Math.floor(availableH / fontLineHeight));

      const pages = [];
      let currentPage = [];
      let usedSlots = 0;

      for (let i = 0; i < parsedSheetLines.length; i++) {
        const item = parsedSheetLines[i];
        if (currentPage.length === 0 && item.type === 'blank') {
          continue;
        }
        let cost = 1;
        if (item.type === 'section') {
          cost = 2;
        } else if (item.type === 'chord-lyric-flow') {
          cost = 2;
        }

        const isSection = (item.type === 'section');
        const extraForSection = isSection ? 2 : 0;

        if (currentPage.length > 0 && (usedSlots + cost + extraForSection) > capacity) {
          pages.push(currentPage);
          currentPage = [];
          usedSlots = 0;
          if (item.type === 'blank') continue;
        }

        currentPage.push(item);
        usedSlots += cost;
      }

      if (currentPage.length > 0) {
        pages.push(currentPage);
      }

      pageList = pages.length > 0 ? pages : [[]];
      if (currentPageIndex >= pageList.length) {
        currentPageIndex = Math.max(0, pageList.length - 1);
      }
      updatePageFlipRailUI();
    }

    function renderCurrentPage() {
      const container = document.getElementById('sheetContent');
      if (!container) return;
      if (!pageList || pageList.length === 0 || !pageList[currentPageIndex]) {
        container.innerHTML = "";
        return;
      }
      const lines = pageList[currentPageIndex];
      const tr = (currentData && currentData.transpose) || 0;
      let html = '<div class="pageflip-container">';
      for (let item of lines) {
        html += item.getHtml(tr) + '\n';
      }
      html += '</div>';
      container.innerHTML = html;
      window.scrollTo({ top: 0, behavior: 'instant' });
      updatePageFlipRailUI();
    }

    function renderContinuousSheet() {
      const container = document.getElementById('sheetContent');
      if (!container) return;
      const tr = (currentData && currentData.transpose) || 0;
      let html = "";
      for (let item of parsedSheetLines) {
        html += item.getHtml(tr) + '\n';
      }
      container.innerHTML = html;
    }

    function updatePageFlipRailUI() {
      const rail = document.getElementById('pageFlipRail');
      if (!rail) return;
      const total = pageList.length;
      if (total <= 1 || !isPageFlip) {
        rail.innerHTML = "";
        rail.classList.remove('visible');
        return;
      }
      rail.classList.add('visible');
      let dotsHtml = "";
      for (let i = 0; i < total; i++) {
        const isCur = (i === currentPageIndex);
        dotsHtml += '<div class="pageflip-dot ' + (isCur ? 'active' : '') + '" onclick="goToPage(' + i + ')" title="Halaman ' + (i + 1) + '"></div>';
      }
      rail.innerHTML = dotsHtml;
    }

    function goToPage(idx) {
      if (idx < 0 || idx >= pageList.length) return;
      currentPageIndex = idx;
      renderCurrentPage();
    }

    function nextPage() {
      if (currentPageIndex < pageList.length - 1) {
        currentPageIndex++;
        renderCurrentPage();
      }
    }

    function prevPage() {
      if (currentPageIndex > 0) {
        currentPageIndex--;
        renderCurrentPage();
      }
    }

    function toggleFullScreen() {
      const doc = window.document;
      const docEl = doc.documentElement;
      const requestFs = docEl.requestFullscreen || docEl.webkitRequestFullscreen || docEl.mozRequestFullScreen || docEl.msRequestFullscreen;
      const cancelFs = doc.exitFullscreen || doc.webkitExitFullscreen || doc.mozCancelFullScreen || doc.msExitFullscreen;

      if (!doc.fullscreenElement && !doc.webkitFullscreenElement && !doc.mozFullScreenElement && !doc.msFullscreenElement) {
        if (requestFs) requestFs.call(docEl).catch(() => {});
      } else {
        if (cancelFs) cancelFs.call(doc).catch(() => {});
      }
    }

    function updateFsIcon() {
      const isFs = !!(document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement || document.msFullscreenElement);
      const enterIcon = document.getElementById('iconFsEnter');
      const exitIcon = document.getElementById('iconFsExit');
      const btnFs = document.getElementById('btnFullscreen');
      if (enterIcon && exitIcon) {
        enterIcon.style.display = isFs ? 'none' : 'block';
        exitIcon.style.display = isFs ? 'block' : 'none';
      }
      if (btnFs) {
        if (isFs) btnFs.classList.add('active');
        else btnFs.classList.remove('active');
      }
    }

    document.addEventListener('fullscreenchange', updateFsIcon);
    document.addEventListener('webkitfullscreenchange', updateFsIcon);

    function escapeHtml(text) {
      if (!text) return "";
      const div = document.createElement('div');
      div.innerText = text;
      return div.innerHTML;
    }

    function unescapeHtml(text) {
      if (!text) return "";
      return text
        .replace(/&#0*39;/g, "'")
        .replace(/&quot;/g, '"')
        .replace(/&amp;/g, '&')
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')
        .replace(/&apos;/g, "'")
        .replace(/&#x27;/gi, "'")
        .replace(/&rsquo;/g, "'")
        .replace(/&lsquo;/g, "'")
        .replace(/&ldquo;/g, '"')
        .replace(/&rdquo;/g, '"')
        .replace(/&#x2F;/gi, '/')
        .replace(/&hellip;/g, '...');
    }

    function isBarNotationLine(line, chordSpans) {
      if (line.indexOf('|') === -1) return false;
      let chars = line.split('');
      for (let span of chordSpans) {
        for (let i = span.start; i < span.end; i++) {
          chars[i] = ' ';
        }
      }
      let remaining = chars.join('')
        .replace(/(?:N\.C\.?|NC|\b(?:[xX]\d+|\d+[xX]|riff|fill|solo|stop)\b)/gi, ' ')
        .replace(/[|\s:.\-_/\\()%+*~^0-9]/g, '');
      return remaining.length === 0 || !/[a-zA-Z]/.test(remaining);
    }

    function isOverlayOnly(line, chordSpans) {
      if (line.indexOf('|') !== -1) return false;
      let chars = line.split('');
      for (let span of chordSpans) {
        for (let i = span.start; i < span.end; i++) {
          chars[i] = ' ';
        }
      }
      let remaining = chars.join('')
        .replace(/(?:N\.C\.?|NC|\b(?:[xX]\d+|\d+[xX]|riff|fill|solo|stop|hold)\b)/gi, ' ')
        .replace(/[|\s:.\-_/\\()%+*~^0-9]/g, '');
      return remaining.length === 0 || !/[a-zA-Z]/.test(remaining);
    }

    function splitInline(line, bracketChords) {
      let tokens = [];
      let lyric = "";
      let removedBefore = 0;
      let cursor = 0;
      for (let bc of bracketChords) {
        let col = bc.index - removedBefore;
        tokens.push({ symbol: bc.chord, column: col });
        lyric += line.substring(cursor, bc.index);
        cursor = bc.index + bc.length;
        removedBefore += bc.length;
      }
      lyric += line.substring(cursor);
      return { tokens: tokens, lyric: lyric };
    }

    function extractTokens(line) {
      let tokens = [];
      let regex = /\S+/g;
      let m;
      while ((m = regex.exec(line)) !== null) {
        tokens.push({ symbol: m[0], column: m.index });
      }
      return tokens;
    }

    function isChordLine(tokens) {
      if (!tokens || tokens.length === 0) return false;
      let chordCount = 0;
      for (let t of tokens) {
        if (isChordSymbol(t.symbol)) {
          chordCount++;
        } else {
          if (t.symbol.length > 7 && t.symbol.indexOf('/') === -1) return false;
        }
      }
      return (chordCount / tokens.length) > 0.5;
    }

    function buildChordLyricSegments(tokens, lyric, delta) {
      const sorted = tokens.slice().sort((a, b) => a.column - b.column);
      if (sorted.length === 0) return [{ chord: null, text: lyric }];
      if (!lyric) return sorted.map(t => ({ chord: transposeChord(t.symbol, delta), text: "" }));

      let segments = [];
      const firstCol = sorted[0].column;
      if (firstCol > 0) {
        const prefixEnd = Math.min(firstCol, lyric.length);
        segments.push({ chord: null, text: lyric.substring(0, prefixEnd) });
      }

      for (let i = 0; i < sorted.length; i++) {
        const t = sorted[i];
        const col = t.column;
        const sym = transposeChord(t.symbol, delta);
        const nextCol = (i + 1 < sorted.length) ? sorted[i + 1].column : Math.max(lyric.length, col);

        let textSlice = "";
        if (col < lyric.length) {
          const sliceEnd = Math.min(nextCol, lyric.length);
          textSlice = lyric.substring(col, sliceEnd);
        } else {
          const spaceCount = (i + 1 < sorted.length) ? Math.max(1, nextCol - col) : 1;
          textSlice = " ".repeat(spaceCount);
        }
        segments.push({ chord: sym, text: textSlice });
      }
      return segments;
    }

    function renderBarLine(line, delta) {
      let html = '<div class="bar-line">';
      if (line.bracketed) {
        const regex = /\[([^\[\]\r\n]+)\]/g;
        let cursor = 0;
        let m;
        while ((m = regex.exec(line.originalText)) !== null) {
          if (isChordSymbol(m[1])) {
            if (m.index > cursor) {
              html += escapeHtml(line.originalText.substring(cursor, m.index));
            }
            const tr = transposeChord(m[1].trim(), delta);
            html += '<span class="chord">' + escapeHtml(tr) + '</span>';
            cursor = m.index + m[0].length;
          }
        }
        if (cursor < line.originalText.length) {
          html += escapeHtml(line.originalText.substring(cursor));
        }
      } else {
        let cursor = 0;
        const sorted = (line.tokens || []).filter(t => isChordSymbol(t.symbol)).sort((a,b) => a.column - b.column);
        for (let t of sorted) {
          if (t.column > cursor) {
            html += escapeHtml(line.originalText.substring(cursor, t.column));
          }
          const tr = transposeChord(t.symbol, delta);
          html += '<span class="chord">' + escapeHtml(tr) + '</span>';
          cursor = t.column + t.symbol.length;
        }
        if (cursor < line.originalText.length) {
          html += escapeHtml(line.originalText.substring(cursor));
        }
      }
      html += '</div>';
      return html;
    }

    function renderChordLyricFlow(tokens, lyric, delta) {
      if (!tokens || tokens.length === 0) {
        return '<div class="lyric-line">' + escapeHtml(lyric || '') + '</div>';
      }
      if (!lyric || lyric.trim().length === 0) {
        let html = '<div class="chord-flow-line chord-only-row">';
        const sorted = tokens.slice().sort((a, b) => a.column - b.column);
        let col = 0;
        for (let t of sorted) {
          if (t.column > col) {
            const spaces = t.column - col;
            html += '<span class="chord-spacer">' + ' '.repeat(spaces) + '</span>';
            col = t.column;
          }
          const tr = transposeChord(t.symbol, delta);
          html += '<span class="chord">' + escapeHtml(tr) + '</span>';
          col += tr.length;
        }
        html += '</div>';
        return html;
      }

      const segments = buildChordLyricSegments(tokens, lyric, delta);
      let html = '<div class="chord-flow-line">';
      for (let seg of segments) {
        html += '<div class="chord-cell">';
        if (seg.chord) {
          const chordText = (seg.text.length === 0 || seg.chord.length >= seg.text.length) ? (seg.chord + " ") : seg.chord;
          html += '<span class="chord">' + escapeHtml(chordText) + '</span>';
        } else {
          html += '<span class="chord chord-empty">&nbsp;</span>';
        }
        if (seg.text && seg.text.length > 0) {
          html += '<span class="lyric">' + escapeHtml(seg.text) + '</span>';
        } else {
          html += '<span class="lyric lyric-empty">&nbsp;</span>';
        }
        html += '</div>';
      }
      html += '</div>';
      return html;
    }

    function parseSheet(rawContent) {
      if (!rawContent) {
        parsedSheetLines = [];
        return;
      }
      const rawLines = unescapeHtml(rawContent).split(/\r?\n/);
      let sheetLines = [];
      const bracketGroupRegex = /\[([^\[\]\r\n]+)\]/g;

      for (let line of rawLines) {
        const trimmed = line.trim();
        if (trimmed.length === 0) {
          sheetLines.push({
            type: 'blank',
            getHtml: () => '<div class="blank-line"></div>'
          });
          continue;
        }

        let bracketChords = [];
        let chordSpans = [];
        let m;
        bracketGroupRegex.lastIndex = 0;
        while ((m = bracketGroupRegex.exec(line)) !== null) {
          if (isChordSymbol(m[1])) {
            bracketChords.push({ chord: m[1].trim(), index: m.index, length: m[0].length });
            chordSpans.push({ start: m.index, end: m.index + m[0].length });
          }
        }

        if (bracketChords.length > 0) {
          if (isBarNotationLine(line, chordSpans)) {
            const lObj = {
              tokens: bracketChords.map(bc => ({ symbol: bc.chord, column: bc.index })),
              originalText: line,
              bracketed: true,
              isBarLine: true
            };
            sheetLines.push({
              type: 'chords-bar',
              getHtml: (d) => renderBarLine(lObj, d)
            });
          } else if (isOverlayOnly(line, chordSpans)) {
            const lObj = {
              tokens: bracketChords.map(bc => ({ symbol: bc.chord, column: bc.index })),
              originalText: line,
              bracketed: true,
              isBarLine: false
            };
            sheetLines.push({
              type: 'chord-lyric-flow',
              tokens: lObj.tokens,
              lyric: "",
              getHtml: (d) => renderChordLyricFlow(lObj.tokens, "", d)
            });
          } else {
            const split = splitInline(line, bracketChords);
            sheetLines.push({
              type: 'chord-lyric-flow',
              tokens: split.tokens,
              lyric: "",
              getHtml: (d) => renderChordLyricFlow(split.tokens, "", d)
            });
            if (split.lyric.trim().length > 0 && /[a-zA-Z]/.test(split.lyric)) {
              sheetLines.push({
                type: 'lyric',
                getHtml: (d) => renderChordLyricFlow(null, split.lyric, d)
              });
            }
          }
          continue;
        }

        if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
          sheetLines.push({
            type: 'section',
            getHtml: () => '<div class="section-header">' + escapeHtml(trimmed) + '</div>'
          });
          continue;
        }

        const tokens = extractTokens(line);
        if (isChordLine(tokens)) {
          const isBar = line.indexOf('|') !== -1;
          if (isBar) {
            const lObj = { tokens: tokens, originalText: line, bracketed: false, isBarLine: true };
            sheetLines.push({
              type: 'chords-bar',
              getHtml: (d) => renderBarLine(lObj, d)
            });
          } else {
            sheetLines.push({
              type: 'chords-overlay',
              tokens: tokens,
              originalText: line
            });
          }
        } else if (line.indexOf('|') !== -1 && isBarNotationLine(line, tokens.map(t => ({ start: t.column, end: t.column + t.symbol.length })))) {
          const lObj = { tokens: tokens, originalText: line, bracketed: false, isBarLine: true };
          sheetLines.push({
            type: 'chords-bar',
            getHtml: (d) => renderBarLine(lObj, d)
          });
        } else {
          sheetLines.push({
            type: 'lyric',
            text: line,
            getHtml: (d) => renderChordLyricFlow(null, line, d)
          });
        }
      }

      // Pair chord overlay lines with following lyric line
      let combined = [];
      for (let i = 0; i < sheetLines.length; i++) {
        const item = sheetLines[i];
        if (item.type === 'chords-overlay') {
          const next = sheetLines[i + 1];
          let lyricText = null;
          if (next && next.type === 'lyric') {
            lyricText = next.text;
            i++;
          }
          const tks = item.tokens;
          combined.push({
            type: 'chord-lyric-flow',
            tokens: tks,
            lyric: lyricText,
            getHtml: (d) => renderChordLyricFlow(tks, lyricText, d)
          });
        } else {
          combined.push(item);
        }
      }
      parsedSheetLines = combined;
    }

    function updateUi(data) {
      if (!data || data.type === 'STANDBY' || data.state === 'standby') {
        document.getElementById('viewStandby').style.display = 'flex';
        document.getElementById('viewPlayer').style.display = 'none';
        document.getElementById('dockBar').style.display = 'none';
        document.getElementById('headerTitle').innerText = 'Standby Panggung';
        document.getElementById('headerSubtitle').innerText = 'KordApp Stage Mirror';
        if (isScrolling) toggleScroll(false);
        if (isPageFlip) togglePageFlip(false);
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

      parseSheet(data.rawContent || '');
      if (isPageFlip) {
        paginateSheet();
        renderCurrentPage();
      } else {
        renderContinuousSheet();
      }
      updateSpeedUI();
      requestWakeLock();
    }

    // Touch and Gesture Navigation for Page Flip
    let touchStartX = 0;
    let touchStartY = 0;
    let touchStartTime = 0;

    document.addEventListener('touchstart', (e) => {
      if (e.touches.length === 1) {
        touchStartX = e.touches[0].clientX;
        touchStartY = e.touches[0].clientY;
        touchStartTime = Date.now();
      }
    }, { passive: true });

    document.addEventListener('touchend', (e) => {
      if (!isPageFlip) return;
      if (e.changedTouches.length === 1) {
        const deltaX = e.changedTouches[0].clientX - touchStartX;
        const deltaY = e.changedTouches[0].clientY - touchStartY;
        const elapsed = Date.now() - touchStartTime;

        const target = e.target;
        if (target && (target.closest('#dockBar') || target.closest('#pageFlipRail') || target.closest('#speedRail'))) {
          return;
        }

        // Horizontal swipe gesture
        if (Math.abs(deltaX) > 40 && Math.abs(deltaY) < 65 && elapsed < 500) {
          if (deltaX < 0) {
            nextPage();
          } else {
            prevPage();
          }
          return;
        }

        // Tap gesture (right half = next, left half = prev)
        if (Math.abs(deltaX) < 15 && Math.abs(deltaY) < 15 && elapsed < 350) {
          const clickX = e.changedTouches[0].clientX;
          if (clickX > window.innerWidth * 0.5) {
            nextPage();
          } else {
            prevPage();
          }
        }
      }
    }, { passive: true });

    // Click on desktop screen for Page Flip
    document.getElementById('viewPlayer').addEventListener('click', (e) => {
      if (!isPageFlip) return;
      if (e.target && (e.target.closest('#dockBar') || e.target.closest('#pageFlipRail') || e.target.closest('#speedRail'))) {
        return;
      }
      if (e.clientX > window.innerWidth * 0.5) {
        nextPage();
      } else {
        prevPage();
      }
    });

    // Keyboard Navigation for Page Flip
    document.addEventListener('keydown', (e) => {
      if (!isPageFlip) return;
      if (e.key === 'ArrowRight' || e.key === 'ArrowDown' || e.key === 'PageDown' || e.key === ' ') {
        e.preventDefault();
        nextPage();
      } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp' || e.key === 'PageUp') {
        e.preventDefault();
        prevPage();
      }
    });

    // Window resize handler for dynamic pagination
    let resizeTimer = null;
    window.addEventListener('resize', () => {
      if (isPageFlip) {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(() => {
          paginateSheet();
          renderCurrentPage();
        }, 150);
      }
    });

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

    // Initialize components on load
    initSpeedRail();
    updateSpeedUI();
    document.getElementById('sheetContent').style.fontSize = fontSize + 'px';
    connectSse();
  </script>
</body>
</html>
    """.trimIndent()
}
