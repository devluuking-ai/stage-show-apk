package com.devluuking.stageshow

import android.content.Context
import android.system.Os
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Runs the bundled Stage Show Node server (live/server.mjs + ttl-live)
 * inside the app via nodejs-mobile, so no Termux is needed.
 * Node can start only once per process; [start] is guarded accordingly.
 */
object NodeRuntime {
    private const val TAG = "NodeRuntime"
    private const val ASSET_VERSION = "stage-v36"

    @Volatile var started = false
        private set

    fun start(ctx: Context, onReady: () -> Unit, onError: (String) -> Unit) {
        if (started) {
            waitReady(onReady, onError)
            return
        }
        started = true
        Thread {
            try {
                val dir = extractAssets(ctx)
                Os.setenv("HOME", ctx.filesDir.absolutePath, true)
                Os.setenv("TMPDIR", ctx.cacheDir.absolutePath, true)
                Os.setenv("PORT", "3000", true)
                Os.setenv("HOST", "0.0.0.0", true)
                Thread {
                    try {
                        val code = NodeBridge.startNodeWithArguments(
                            arrayOf("node", File(dir, "live/server.mjs").absolutePath)
                        )
                        Log.i(TAG, "node exited with $code")
                    } catch (e: Throwable) {
                        Log.e(TAG, "node start failed", e)
                    }
                }.start()
                waitReady(onReady, onError)
            } catch (e: Throwable) {
                Log.e(TAG, "extract failed", e)
                onError("Server စတင်မရပါ: ${e.message}")
            }
        }.start()
    }

    private fun waitReady(onReady: () -> Unit, onError: (String) -> Unit) {
        Thread {
            for (i in 0 until 80) {
                try {
                    val conn = URL("http://127.0.0.1:3000/live/overlay.html").openConnection() as HttpURLConnection
                    conn.connectTimeout = 1000
                    conn.readTimeout = 1000
                    if (conn.responseCode == 200) {
                        onReady()
                        return@Thread
                    }
                } catch (_: Exception) {
                }
                Thread.sleep(500)
            }
            onError("Server မတက်လာပါ — app ကို ပြန်ဖွင့်ကြည့်ပါ")
        }.start()
    }

    private fun extractAssets(ctx: Context): File {
        val out = File(ctx.filesDir, "stage")
        val marker = File(out, ".version")
        if (marker.exists() && marker.readText() == ASSET_VERSION && File(out, "live/server.mjs").exists()) {
            return out
        }
        if (out.exists()) out.deleteRecursively()
        out.mkdirs()
        copyAssetDir(ctx, "stage", out)
        marker.writeText(ASSET_VERSION)
        return out
    }

    private fun copyAssetDir(ctx: Context, assetPath: String, outDir: File) {
        val entries = ctx.assets.list(assetPath) ?: return
        if (entries.isEmpty()) {
            ctx.assets.open(assetPath).use { input ->
                outDir.outputStream().use { input.copyTo(it) }
            }
            return
        }
        outDir.mkdirs()
        for (name in entries) {
            copyAssetDir(ctx, "$assetPath/$name", File(outDir, name))
        }
    }
}
