package com.chordapp.promo

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

interface PromoCache {
    fun read(): String?
    fun write(content: String)
}

class PromoCacheStore(private val file: File) : PromoCache {
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

/**
 * Cache-first: coba jaringan; sukses -> simpan & pakai; gagal -> pakai cache
 * terakhir bila ada. Mengembalikan promo AKTIF pertama, atau null (yang berarti
 * "jangan tampilkan popup" — tidak pernah ada isi palsu).
 */
class PromoRepository(
    private val remote: PromoRemoteSource,
    private val cache: PromoCache
) {
    fun loadActive(): Promo? {
        val fresh = runCatching { remote.fetch() }.getOrNull()
        val json = if (fresh != null) {
            runCatching { cache.write(fresh) }
            fresh
        } else {
            cache.read() ?: return null
        }
        return PromoParser.parseActive(json)
    }
}
