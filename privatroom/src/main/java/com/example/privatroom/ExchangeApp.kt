package com.example.privatroom

import android.app.Application

class ExchangeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ExchangeSync.schedulePeriodic(this)
    }
}