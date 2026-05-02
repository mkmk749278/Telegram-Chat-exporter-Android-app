package com.example.tgexporter

import android.app.Application
import com.example.tgexporter.di.AppContainer

class ExporterApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
