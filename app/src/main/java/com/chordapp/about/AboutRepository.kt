package com.chordapp.about

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

interface AboutCache {
    fun read(): String?
    fun write(content: String)
}

class AboutCacheStore(private val file: File) : AboutCache {
    override fun read(): String? = runCatching {
        file.takeIf(File::isFile)
            ?.readText(StandardCharsets.UTF_8)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }.getOrNull()

    override fun write(content: String) {
        require(content.isNotBlank())
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(content, StandardCharsets.UTF_8)
        try {
            Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}

class AboutRepository(
    private val remote: AboutRemoteSource,
    private val cache: AboutCache,
    private val fallback: String
) {
    fun initialContent(): String = cache.read() ?: fallback

    fun refresh(): Result<String> = runCatching {
        remote.fetch().also(cache::write)
    }
}
