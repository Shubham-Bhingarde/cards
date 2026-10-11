package com.shubham.cards

import android.app.Application
import com.shubham.cards.log.AppLogger

class CardsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        AppLogger.i("CardsApp", "Application started (alpha, read-only diagnostics)")
    }
}
