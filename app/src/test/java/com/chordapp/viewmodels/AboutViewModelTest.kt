package com.chordapp.viewmodels

import com.chordapp.about.AboutCache
import com.chordapp.about.AboutRemoteSource
import com.chordapp.about.AboutRepository
import com.chordapp.about.AppMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AboutViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows cached content before successful background refresh`() = runTest {
        val cache = MemoryCache("cached")
        val viewModel = AboutViewModel(
            AppMetadata("ChordApp", "1.0", 1),
            AboutRepository(AboutRemoteSource { "fresh" }, cache, "fallback"),
            dispatcher
        )

        assertEquals("cached", viewModel.content.value)
        advanceUntilIdle()
        assertEquals("fresh", viewModel.content.value)
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `refresh failure keeps fallback content`() = runTest {
        val viewModel = AboutViewModel(
            AppMetadata("ChordApp", "1.0", 1),
            AboutRepository(AboutRemoteSource { error("offline") }, MemoryCache(null), "fallback"),
            dispatcher
        )

        assertEquals("fallback", viewModel.content.value)
        advanceUntilIdle()
        assertEquals("fallback", viewModel.content.value)
        assertFalse(viewModel.isRefreshing.value)
    }

    private class MemoryCache(private var value: String?) : AboutCache {
        override fun read(): String? = value
        override fun write(content: String) { value = content }
    }
}
