package com.chordapp.usercatalog

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object KordPackageSerializer {
    private const val APP_IDENTIFIER = "KordApp"
    private const val SCHEMA_VERSION = 1
    private const val TYPE_SCRAPED = "SCRAPED"

    fun serializeScrapedSongs(songs: List<ScrapedSongEntity>): String {
        val root = JSONObject()
        root.put("app", APP_IDENTIFIER)
        root.put("version", SCHEMA_VERSION)
        root.put("type", TYPE_SCRAPED)
        root.put("exported_at", System.currentTimeMillis())
        root.put("songs_count", songs.size)

        val songsArray = JSONArray()
        for (s in songs) {
            val item = JSONObject()
            item.put("source", s.source)
            item.put("source_url", s.sourceUrl)
            item.put("title", s.title)
            item.put("artist", s.artist)
            item.put("title_slug", s.titleSlug)
            item.put("artist_slug", s.artistSlug)
            item.put("version_name", s.versionName ?: "")
            if (s.rating != null) item.put("rating", s.rating.toDouble())
            if (s.votes != null) item.put("votes", s.votes)
            item.put("key_base", s.keyBase ?: "")
            item.put("difficulty", s.difficulty ?: "")
            item.put("category", s.category ?: "")
            item.put("region", s.region)
            item.put("chords", s.chords)
            item.put("chord_count", s.chordCount)
            item.put("line_count", s.lineCount)
            item.put("content", s.content)
            item.put("content_hash", s.contentHash)
            item.put("scraped_at", s.scrapedAt)
            songsArray.put(item)
        }
        root.put("songs", songsArray)
        return root.toString(2)
    }

    fun deserializeScrapedSongs(jsonString: String): List<ScrapedSongEntity> {
        val root = JSONObject(jsonString)
        val app = root.optString("app")
        if (app != APP_IDENTIFIER) {
            throw IllegalArgumentException("File ini bukan format .kord resmi KordApp")
        }

        val songsArray = root.optJSONArray("songs") ?: return emptyList()
        val list = ArrayList<ScrapedSongEntity>()
        for (i in 0 until songsArray.length()) {
            val item = songsArray.getJSONObject(i)
            val entity = ScrapedSongEntity(
                id = null,
                source = item.optString("source", "web"),
                sourceUrl = item.optString("source_url", ""),
                title = item.optString("title", "Untitled"),
                artist = item.optString("artist", "Unknown"),
                titleSlug = item.optString("title_slug", ""),
                artistSlug = item.optString("artist_slug", ""),
                versionName = item.optString("version_name").takeIf { it.isNotBlank() },
                rating = if (item.has("rating")) item.optDouble("rating").toFloat() else null,
                votes = if (item.has("votes")) item.optInt("votes") else null,
                keyBase = item.optString("key_base").takeIf { it.isNotBlank() },
                difficulty = item.optString("difficulty").takeIf { it.isNotBlank() },
                category = item.optString("category").takeIf { it.isNotBlank() },
                region = item.optString("region", "ID"),
                chords = item.optString("chords", ""),
                chordCount = item.optInt("chord_count", 0),
                lineCount = item.optInt("line_count", 0),
                content = item.optString("content", ""),
                contentHash = item.optString("content_hash", ""),
                scrapedAt = item.optString("scraped_at", "")
            )
            list.add(entity)
        }
        return list
    }

    fun exportScrapedToFile(context: Context, songs: List<ScrapedSongEntity>): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(exportDir, "kordapp_scraped_$timeStamp.kord")
        val json = serializeScrapedSongs(songs)
        file.writeText(json)
        return file
    }
}
