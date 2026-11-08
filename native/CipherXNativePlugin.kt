package com.cipherx.app

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import java.io.File

/**
 * Lets CipherX be opened from other apps ("Open with" / "Share to").
 * The incoming content:// stream is copied into the app cache and its path is
 * handed to the web layer; the cache is swept again by the web layer on start.
 */
@CapacitorPlugin(name = "CipherXNative")
class CipherXNativePlugin : Plugin() {

    private fun extract(i: Intent?): JSObject? {
        if (i == null) return null
        @Suppress("DEPRECATION")
        val uri: Uri = when (i.action) {
            Intent.ACTION_VIEW -> i.data
            Intent.ACTION_SEND -> i.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            else -> null
        } ?: return null
        return try {
            var name = "shared.bin"
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val ix = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && ix >= 0) name = c.getString(ix) ?: name
            }
            val safe = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val dir = File(context.cacheDir, "open")
            dir.deleteRecursively()
            dir.mkdirs()
            val out = File(dir, safe)
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
            val r = JSObject()
            r.put("path", out.absolutePath)
            r.put("name", safe)
            r.put("size", out.length())
            r
        } catch (e: Exception) {
            null
        }
    }

    /** Called by the web layer once on startup (cold start via "Open with"). */
    @PluginMethod
    fun getOpenedFile(call: PluginCall) {
        val intent = activity?.intent
        val f = extract(intent)
        if (f != null) activity?.intent = Intent()  // consume so it is not re-opened
        call.resolve(f ?: JSObject())
    }

    /** App already running (singleTask): a new intent arrives. */
    override fun handleOnNewIntent(intent: Intent) {
        super.handleOnNewIntent(intent)
        val f = extract(intent)
        if (f != null) notifyListeners("fileOpened", f, true)
    }
}
