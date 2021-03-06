package com.sanicorporation

import android.app.Application

class BluetoothChat: Application() {

    companion object{
        lateinit var packageNameForService: String

    }

    override fun onCreate() {
        super.onCreate()
        packageNameForService = packageName
    }
}