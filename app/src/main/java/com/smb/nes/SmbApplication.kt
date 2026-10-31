package com.smb.nes

import android.app.Application
import android.util.Log

/**
 * Application class – initialize NES emulator SDK here.
 */
class SmbApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Beseting NesEmulator initialization
            // Class name may vary slightly depending on library version
            val clazz = Class.forName("io.github.beseting.nesemulator.EmulatorSDK")
            val method = clazz.getMethod("init", android.content.Context::class.java)
            method.invoke(null, applicationContext)
            Log.i("SMB", "NesEmulator SDK initialized")
        } catch (e: Exception) {
            // Fallback – some versions use different package
            try {
                val clazz = Class.forName("com.beseting.nes.EmulatorSDK")
                val method = clazz.getMethod("init", android.content.Context::class.java)
                method.invoke(null, applicationContext)
                Log.i("SMB", "NesEmulator SDK initialized (alt package)")
            } catch (e2: Exception) {
                Log.w("SMB", "Could not init EmulatorSDK automatically. " +
                        "You may need to call it manually or the library version differs. " +
                        e2.message)
            }
        }
    }
}
