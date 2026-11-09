package com.cipherx.app

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
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

    /**
     * Argon2id key derivation (BouncyCastle). Input/outputs are base64.
     * Runs on the Capacitor plugin thread, never on the UI thread.
     */
    @PluginMethod
    fun argon2id(call: PluginCall) {
        try {
            val pw = Base64.decode(call.getString("password") ?: "", Base64.NO_WRAP)
            val salt = Base64.decode(call.getString("salt") ?: "", Base64.NO_WRAP)
            val t = call.getInt("t") ?: 3
            val m = call.getInt("m") ?: 32768
            val p = call.getInt("p") ?: 2
            val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(t)
                .withMemoryAsKB(m)
                .withParallelism(p)
                .withSalt(salt)
                .build()
            val gen = Argon2BytesGenerator()
            gen.init(params)
            val out = ByteArray(32)
            gen.generateBytes(pw, out)
            val r = JSObject()
            r.put("key", Base64.encodeToString(out, Base64.NO_WRAP))
            call.resolve(r)
        } catch (e: Exception) {
            call.reject(e.message ?: "argon2id failed")
        }
    }

    /** App already running (singleTask): a new intent arrives. */
    override fun handleOnNewIntent(intent: Intent) {
        super.handleOnNewIntent(intent)
        val f = extract(intent)
        if (f != null) notifyListeners("fileOpened", f, true)
    }
}
