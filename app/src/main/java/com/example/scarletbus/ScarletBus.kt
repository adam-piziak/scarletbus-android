package com.example.scarletbus

import android.app.Application
import com.example.scarletbus.models.Stop

class ScarletBus: Application() {
    var stops: ArrayList<Stop> = ArrayList()

    override fun onCreate() {
        super.onCreate()
    }
}