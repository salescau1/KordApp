package com.chordapp.charts

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Persists the last good chart snapshot per platform as JSON under
 * [dir]/<platform.cacheFile>. A corrupt/missing file is treated as "no cache"
 * (never a crash).
 */
class ChartCacheStore(private val dir: File) {

    private fun fileFor(platform: ChartPlatform): File = File(dir, platform.cacheFile)

    fun read(platform: ChartPlatform): ChartSnapshot? {
        return try {
            val file = fileFor(platform)
            if (!file.exists()) {
                null
            } else {
                val root = JSONObject(file.readText())
                val arr = root.getJSONArray("entries")
                val entries = buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(ChartEntry(o.getInt("rank"), o.getString("title"), o.getString("artist")))
                    }
                }
                ChartSnapshot(entries, root.getLong("fetchedAt"))
            }
        } catch (e: Exception) {
            null
        }
    }

    fun write(platform: ChartPlatform, snapshot: ChartSnapshot) = try {
        dir.mkdirs()
        val arr = JSONArray()
        snapshot.entries.forEach {
            arr.put(
                JSONObject()
                    .put("rank", it.rank)
                    .put("title", it.title)
                    .put("artist", it.artist)
            )
        }
        val root = JSONObject()
            .put("fetchedAt", snapshot.fetchedAtEpochMs)
            .put("entries", arr)
        val target = fileFor(platform)
        val temp = File(dir, "${platform.cacheFile}.tmp")
        temp.writeText(root.toString())
        try {
            Files.move(
                temp.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: Exception) {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    } catch (_: Exception) {
        // Never let cache trouble break a fresh chart load.
    }
}
