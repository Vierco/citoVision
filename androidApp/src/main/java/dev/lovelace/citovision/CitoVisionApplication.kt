package dev.lovelace.citovision

import android.app.Application
import dev.lovelace.citovision.composition.di.initKoin
import org.koin.android.ext.koin.androidContext

/**
 * Entry point de proceso en Android. Arranca Koin una única vez (RULES.md) y registra
 * el Context de Android para las dependencias de plataforma (p.ej. DataStore en fase 1B).
 */
class CitoVisionApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@CitoVisionApplication)
        }
    }
}
