package com.chordapp

import android.app.Application

class ChordApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.chordapp.p2p.BandSessionManager.initialize(this)
    }
}
