package com.xthan.stickies


import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView = findViewById<WebView>(R.id.webview)
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(WebAppInterface(), "AndroidBridge")

        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun launchApp(packageName: String) {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) {
                startActivity(intent)
            }
        }

        @JavascriptInterface
        fun saveImageToDevice(dataUrl: String) {
            try {
                // Strip out the data:image/png;base64 prefix
                val base64Data = dataUrl.substringAfter(",")
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                val filename = "lcd-drawing-${System.currentTimeMillis()}.png"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/airstairsSTICKIES")
                    }
                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { stream ->
                            stream.write(decodedBytes)
                        }
                    }
                } else {
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, filename)
                    FileOutputStream(file).write(decodedBytes)
                }

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Saved to Pictures/airstairsSTICKIES 📸", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
