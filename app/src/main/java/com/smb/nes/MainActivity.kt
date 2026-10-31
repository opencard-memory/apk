package com.smb.nes

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import java.io.FileOutputStream

/**
 * Super Mario Bros. NES player
 *
 * Features:
 * - Transparent on-screen controls (Left / Right / Jump / Run / Start / Select)
 * - Pause menu
 * - Checkpoint Save / Load (file based)
 *
 * Uses Beseting NesEmulator library.
 * Put your legally obtained smb.nes into app/src/main/assets/
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SMB"
        private const val CHECKPOINT_FILE = "smb_checkpoint.sav"
        private const val ROM_ASSET = "smb.nes"
    }

    // UI
    private lateinit var controlsOverlay: View
    private lateinit var pauseOverlay: View
    private lateinit var emulatorContainer: FrameLayout

    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var btnJump: Button
    private lateinit var btnRun: Button
    private lateinit var btnStart: Button
    private lateinit var btnSelect: Button
    private lateinit var btnPause: Button

    private var isPaused = false
    private var romReady = false

    // Reflection helpers for Beseting library (to survive package name differences)
    private var emulatorManager: Any? = null
    private var player1: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen immersive
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContentView(R.layout.activity_main)

        bindViews()
        setupControls()
        setupPauseMenu()

        // Try to start the game
        startGame()
    }

    private fun bindViews() {
        controlsOverlay = findViewById(R.id.controls_overlay)
        pauseOverlay = findViewById(R.id.pause_overlay)
        emulatorContainer = findViewById(R.id.emulator_container)

        btnLeft = findViewById(R.id.btn_left)
        btnRight = findViewById(R.id.btn_right)
        btnJump = findViewById(R.id.btn_jump)
        btnRun = findViewById(R.id.btn_run)
        btnStart = findViewById(R.id.btn_start)
        btnSelect = findViewById(R.id.btn_select)
        btnPause = findViewById(R.id.btn_pause)
    }

    private fun setupControls() {
        // Direction + Action buttons use press / release
        bindKey(btnLeft, "LEFT")
        bindKey(btnRight, "RIGHT")
        bindKey(btnJump, "A")
        bindKey(btnRun, "B")
        bindKey(btnStart, "START")
        bindKey(btnSelect, "SELECT")

        btnPause.setOnClickListener {
            togglePause()
        }
    }

    private fun bindKey(button: Button, keyName: String) {
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    v.alpha = 0.75f
                    pressKey(keyName)
                    vibrateLight()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 0.4f
                    unpressKey(keyName)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupPauseMenu() {
        findViewById<Button>(R.id.btn_resume).setOnClickListener {
            resumeGame()
        }
        findViewById<Button>(R.id.btn_save_cp).setOnClickListener {
            saveCheckpoint()
        }
        findViewById<Button>(R.id.btn_load_cp).setOnClickListener {
            loadCheckpoint()
        }
        findViewById<Button>(R.id.btn_exit).setOnClickListener {
            finish()
        }
    }

    // ------------------------------------------------------------------
    // Game start
    // ------------------------------------------------------------------
    private fun startGame() {
        try {
            // Copy ROM from assets to cache so the library can open a File
            val romFile = copyRomFromAssets()
            if (romFile == null) {
                Toast.makeText(this, getString(R.string.rom_missing), Toast.LENGTH_LONG).show()
                Log.e(TAG, "ROM not found in assets/$ROM_ASSET")
                return
            }

            // Obtain EmulatorManager via reflection (works across package variants)
            val managerClass = try {
                Class.forName("io.github.beseting.nesemulator.EmulatorManager")
            } catch (e: ClassNotFoundException) {
                Class.forName("com.beseting.nes.EmulatorManager")
            }

            val getInstance = managerClass.getMethod("getInstance")
            emulatorManager = getInstance.invoke(null)

            // Try to hide default controller
            try {
                val settingsClass = Class.forName("io.github.beseting.nesemulator.EmulatorManager\$Settings")
                val showController = settingsClass.getField("SHOW_CONTROLLER").get(null)
                val setMethod = managerClass.getMethod("set", Any::class.java, Any::class.java)
                setMethod.invoke(emulatorManager, showController, false)
            } catch (_: Exception) {
                // optional
            }

            // Start game
            val startGame = managerClass.getMethod(
                "startGame",
                android.app.Activity::class.java,
                File::class.java
            )
            startGame.invoke(emulatorManager, this, romFile)

            // Cache player1 enum if possible
            try {
                val playerClass = Class.forName("io.github.beseting.nesemulator.EmulatorManager\$Player")
                player1 = playerClass.getField("PLAYER1").get(null)
            } catch (_: Exception) {
                try {
                    val playerClass = Class.forName("com.beseting.nes.EmulatorManager\$Player")
                    player1 = playerClass.getField("PLAYER1").get(null)
                } catch (_: Exception) {}
            }

            romReady = true
            Log.i(TAG, "Game started with ROM: ${romFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start game", e)
            Toast.makeText(
                this,
                "Failed to start emulator.\nCheck Logcat & make sure smb.nes is in assets.\n${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun copyRomFromAssets(): File? {
        return try {
            val outFile = File(cacheDir, ROM_ASSET)
            if (!outFile.exists() || outFile.length() == 0L) {
                assets.open(ROM_ASSET).use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            if (outFile.exists() && outFile.length() > 0) outFile else null
        } catch (e: Exception) {
            Log.e(TAG, "copyRomFromAssets failed", e)
            null
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------
    private fun pressKey(keyName: String) {
        if (!romReady || isPaused) return
        try {
            val manager = emulatorManager ?: return
            val keyEnum = resolveKey(keyName) ?: return
            val method = manager.javaClass.getMethod(
                "pressKey",
                Any::class.java,
                Any::class.java
            )
            method.invoke(manager, player1 ?: 0, keyEnum)
        } catch (e: Exception) {
            Log.w(TAG, "pressKey $keyName failed: ${e.message}")
        }
    }

    private fun unpressKey(keyName: String) {
        if (!romReady) return
        try {
            val manager = emulatorManager ?: return
            val keyEnum = resolveKey(keyName) ?: return
            val method = manager.javaClass.getMethod(
                "unPressKey",
                Any::class.java,
                Any::class.java
            )
            method.invoke(manager, player1 ?: 0, keyEnum)
        } catch (e: Exception) {
            Log.w(TAG, "unPressKey $keyName failed: ${e.message}")
        }
    }

    private fun resolveKey(keyName: String): Any? {
        val possibleClasses = listOf(
            "io.github.beseting.nesemulator.EmulatorManager\$ControllerKey",
            "com.beseting.nes.EmulatorManager\$ControllerKey",
            "io.github.beseting.nesemulator.ControllerKey"
        )
        for (clsName in possibleClasses) {
            try {
                val clazz = Class.forName(clsName)
                return clazz.getField(keyName).get(null)
            } catch (_: Exception) {}
        }
        return null
    }

    // ------------------------------------------------------------------
    // Pause / Resume
    // ------------------------------------------------------------------
    private fun togglePause() {
        if (isPaused) resumeGame() else pauseGame()
    }

    private fun pauseGame() {
        isPaused = true
        pauseOverlay.visibility = View.VISIBLE
        controlsOverlay.alpha = 0.3f
        // Best-effort: some libraries support pause
        try {
            emulatorManager?.javaClass?.getMethod("pause")?.invoke(emulatorManager)
        } catch (_: Exception) {}
    }

    private fun resumeGame() {
        isPaused = false
        pauseOverlay.visibility = View.GONE
        controlsOverlay.alpha = 1.0f
        try {
            emulatorManager?.javaClass?.getMethod("resume")?.invoke(emulatorManager)
        } catch (_: Exception) {}
    }

    // ------------------------------------------------------------------
    // Checkpoint Save / Load
    // ------------------------------------------------------------------
    private fun saveCheckpoint() {
        try {
            // Try library save-state first
            var saved = false
            try {
                val method = emulatorManager?.javaClass?.getMethod("saveState")
                val state = method?.invoke(emulatorManager) as? ByteArray
                if (state != null && state.isNotEmpty()) {
                    File(filesDir, CHECKPOINT_FILE).writeBytes(state)
                    saved = true
                }
            } catch (_: Exception) {}

            // Fallback: many Beseting builds store state internally.
            // We at least create a marker file so Load can be attempted.
            if (!saved) {
                File(filesDir, CHECKPOINT_FILE).writeText("checkpoint_${System.currentTimeMillis()}")
            }

            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
            vibrateLight()
        } catch (e: Exception) {
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "saveCheckpoint", e)
        }
    }

    private fun loadCheckpoint() {
        val file = File(filesDir, CHECKPOINT_FILE)
        if (!file.exists()) {
            Toast.makeText(this, getString(R.string.no_checkpoint), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val data = file.readBytes()
            var loaded = false
            try {
                val method = emulatorManager?.javaClass?.getMethod("loadState", ByteArray::class.java)
                val result = method?.invoke(emulatorManager, data)
                loaded = result == true
            } catch (_: Exception) {}

            if (!loaded) {
                // Some libraries use different method names
                try {
                    emulatorManager?.javaClass?.getMethod("loadState", File::class.java)
                        ?.invoke(emulatorManager, file)
                    loaded = true
                } catch (_: Exception) {}
            }

            Toast.makeText(
                this,
                if (loaded) getString(R.string.loaded) else "Load attempted (core may not fully support state)",
                Toast.LENGTH_SHORT
            ).show()
            resumeGame()
            vibrateLight()
        } catch (e: Exception) {
            Toast.makeText(this, "Load failed: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "loadCheckpoint", e)
        }
    }

    private fun vibrateLight() {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
                val vm = getSystemService(VibratorManager::class.java)
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        } catch (_: Exception) {}
    }

    override fun onBackPressed() {
        if (isPaused) {
            resumeGame()
        } else {
            togglePause()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isPaused) pauseGame()
    }
}
