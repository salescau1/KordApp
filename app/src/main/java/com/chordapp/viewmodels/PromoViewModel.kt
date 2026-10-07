package com.chordapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.promo.Promo
import com.chordapp.promo.PromoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Menyediakan promo aktif untuk popup Home. Diambil sekali saat ViewModel dibuat
 * (yakni tiap kali app dibuka lalu masuk Home). `promo` bernilai null sampai ada
 * promo aktif yang valid; jika gagal/offline/tidak ada, tetap null -> popup tidak
 * muncul. `dismiss()` menutup popup untuk sesi ini.
 */
class PromoViewModel(
    private val repository: PromoRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _promo = MutableStateFlow<Promo?>(null)
    val promo: StateFlow<Promo?> = _promo.asStateFlow()

    private var dismissed = false

    init {
        if (!shownThisLaunch) {
            viewModelScope.launch {
                val active = withContext(ioDispatcher) { repository.loadActive() }
                if (!dismissed && active != null) {
                    shownThisLaunch = true
                    _promo.value = active
                }
            }
        }
    }

    fun dismiss() {
        dismissed = true
        _promo.value = null
    }

    companion object {
        /**
         * Guard proses: popup promo hanya tampil SEKALI per peluncuran app.
         * Bertahan saat NavBackStackEntry Home dibuat ulang (pindah tab lalu balik),
         * dan otomatis reset saat proses mati (cold start) -> muncul lagi.
         */
        @Volatile
        private var shownThisLaunch = false
    }
}
