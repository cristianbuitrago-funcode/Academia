package com.funcode.academy

import android.app.Application

/** Punto de entrada del proceso. Crea una sola vez las dependencias compartidas. */
class FuncodeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
