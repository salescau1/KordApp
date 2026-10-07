package com.chordapp.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AboutRepositoryTest {
    @Test
    fun `initial content prefers cache and refresh replaces it`() {
        val cache = MemoryCache("cached")
        val repository = AboutRepository(
            remote = AboutRemoteSource { "fresh" },
            cache = cache,
            fallback = "fallback"
        )

        assertEquals("cached", repository.initialContent())
        assertEquals("fresh", repository.refresh().getOrThrow())
        assertEquals("fresh", cache.value)
    }

    @Test
    fun `missing cache uses fallback and failure preserves it`() {
        val cache = MemoryCache(null)
        val repository = AboutRepository(
            remote = AboutRemoteSource { error("offline") },
            cache = cache,
            fallback = "fallback"
        )

        assertEquals("fallback", repository.initialContent())
        assertTrue(repository.refresh().isFailure)
        assertEquals(null, cache.value)
    }

    @Test
    fun `file cache ignores blank content and survives corrupt absence`() {
        val root = File(System.getProperty("java.io.tmpdir"), "about-cache-${System.nanoTime()}")
        val file = File(root, "about.txt")
        val store = AboutCacheStore(file)
        assertEquals(null, store.read())

        root.mkdirs()
        file.writeText("   ")
        assertEquals(null, store.read())

        store.write("valid")
        assertEquals("valid", store.read())
        assertFalse(File(root, "about.txt.tmp").exists())
        root.deleteRecursively()
    }

    private class MemoryCache(var value: String?) : AboutCache {
        override fun read(): String? = value
        override fun write(content: String) { value = content }
    }
}
