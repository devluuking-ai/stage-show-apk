package com.devluuking.stageshow

import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var showView: WebView
    private lateinit var controlView: WebView
    private lateinit var statusText: TextView
    private lateinit var controlBar: View
    private var showMode = false
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        showView = findViewById(R.id.showView)
        controlView = findViewById(R.id.controlView)
        statusText = findViewById(R.id.statusText)
        controlBar = findViewById(R.id.controlBar)

        setupWebView(showView)
        setupWebView(controlView)

        findViewById<Button>(R.id.controlButton).setOnClickListener { showControl(true) }
        findViewById<Button>(R.id.showButton).setOnClickListener { showControl(false) }
        findViewById<Button>(R.id.fullscreenButton).setOnClickListener { enterShowMode() }
        showView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && showMode) {
                exitShowMode()
                true
            } else {
                false
            }
        }

        statusText.text = "Server စတင်နေသည်…"
        NodeRuntime.start(
            this,
            onReady = {
                runOnUiThread {
                    statusText.text = "အဆင်သင့် — Control မှာ TikTok username ချိတ်ပြီး Show ကို အပြည့်ဖွင့်ပါ"
                    showView.loadUrl("http://127.0.0.1:3000/live/overlay.html")
                    controlView.loadUrl("http://127.0.0.1:3000/live/control.html")
                }
            },
            onError = { msg -> runOnUiThread { statusText.text = msg } }
        )
    }

    private fun showControl(control: Boolean) {
        controlView.visibility = if (control) View.VISIBLE else View.GONE
        showView.visibility = if (control) View.GONE else View.VISIBLE
    }

    private fun setupWebView(webView: WebView) {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = false
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback
                val intent = fileChooserParams?.createIntent()
                return if (intent != null) {
                    fileChooserLauncher.launch(intent)
                    true
                } else {
                    this@MainActivity.filePathCallback = null
                    false
                }
            }
        }
    }

    private fun enterShowMode() {
        showControl(false)
        showMode = true
        controlBar.visibility = View.GONE
        statusText.visibility = View.GONE
        applyImmersive(true)
    }

    private fun exitShowMode() {
        showMode = false
        controlBar.visibility = View.VISIBLE
        statusText.visibility = View.VISIBLE
        applyImmersive(false)
    }

    private fun applyImmersive(on: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, !on)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (on) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && showMode) applyImmersive(true)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            showMode -> exitShowMode()
            controlView.visibility == View.VISIBLE && controlView.canGoBack() -> controlView.goBack()
            showView.canGoBack() -> showView.goBack()
            else -> super.onBackPressed()
        }
    }
}
