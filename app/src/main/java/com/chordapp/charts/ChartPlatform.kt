package com.chordapp.charts

/** Chart platforms backed by real, reachable, no-key public sources. */
enum class ChartPlatform(
    val label: String,
    /** Cache file name (relative to the charts cache dir). */
    val cacheFile: String
) {
    SPOTIFY("Spotify", "spotify.json"),
    YOUTUBE("YouTube", "youtube.json")
}
