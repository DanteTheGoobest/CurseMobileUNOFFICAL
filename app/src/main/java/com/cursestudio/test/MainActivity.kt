package com.cursestudio.test

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private lateinit var root: LinearLayout
    private lateinit var controls: LinearLayout
    private lateinit var handle: TextView
    private lateinit var status: TextView

    private val homeUrl = "https://www.curseforge.com/"
    private val authorsUrl = "https://authors.curseforge.com/"

    private val prefs by lazy { getSharedPreferences("cs", Context.MODE_PRIVATE) }
    private var zoom = 0.7f
    private var barTop = false
    private var barShown = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        zoom = prefs.getFloat("zoom", 0.7f)
        barTop = prefs.getBoolean("barTop", false)
        barShown = prefs.getBoolean("barShown", true)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Control bar: buttons + status line
        controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun addButton(label: String, weight: Float = 1f, onClick: () -> Unit) {
            buttons.addView(
                Button(this).apply {
                    text = label
                    isAllCaps = false
                    setOnClickListener { onClick() }
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
            )
        }
        addButton("Home") { web.loadUrl(homeUrl) }
        addButton("Authors") { web.loadUrl(authorsUrl) }
        addButton("Reload") { web.reload() }
        addButton("-", 0.5f) { changeZoom(-0.1f) }
        addButton("+", 0.5f) { changeZoom(0.1f) }
        addButton("\u21C5", 0.5f) { setBarTop(!barTop) }
        controls.addView(buttons)

        status = TextView(this).apply {
            textSize = 12f
            setPadding(24, 8, 24, 8)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        controls.addView(status)

        // Thin strip that is always visible; tap to hide or show the controls
        handle = TextView(this).apply {
            text = "\u2022 \u2022 \u2022"
            gravity = Gravity.CENTER
            textSize = 11f
            setTextColor(Color.parseColor("#BBBBBB"))
            setBackgroundColor(Color.parseColor("#2A2A2A"))
            setOnClickListener { setBarShown(!barShown) }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (28 * resources.displayMetrics.density).toInt()
            )
        }

        web = WebView(this)
        root.addView(
            web,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        applyControls()
        setContentView(root)

        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(web, true)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                updateStatus(url, "loading")
            }

            override fun onPageFinished(view: WebView, url: String) {
                cookies.flush()
                applyZoom()
                updateStatus(url, "done")
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) {
                    web.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        if (savedInstanceState == null) {
            web.loadUrl(homeUrl)
        } else {
            web.restoreState(savedInstanceState)
        }
    }

    // Puts the controls and handle above or below the page, and shows/hides the controls
    private fun applyControls() {
        root.removeView(controls)
        root.removeView(handle)
        if (barTop) {
            root.addView(handle, 0)
            root.addView(controls, 1)
        } else {
            root.addView(controls)
            root.addView(handle)
        }
        controls.visibility = if (barShown) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun setBarTop(top: Boolean) {
        barTop = top
        prefs.edit().putBoolean("barTop", top).apply()
        applyControls()
    }

    private fun setBarShown(shown: Boolean) {
        barShown = shown
        prefs.edit().putBoolean("barShown", shown).apply()
        applyControls()
    }

    private fun applyZoom() {
        web.evaluateJavascript("document.documentElement.style.zoom='$zoom'", null)
    }

    private fun changeZoom(delta: Float) {
        zoom = (((zoom + delta).coerceIn(0.4f, 1.5f)) * 10).roundToInt() / 10f
        prefs.edit().putFloat("zoom", zoom).apply()
        applyZoom()
        updateStatus(web.url ?: "", "zoom")
    }

    private fun updateStatus(url: String, state: String) {
        val cookieString = CookieManager.getInstance().getCookie("https://www.curseforge.com") ?: ""
        val cookieCount = if (cookieString.isBlank()) 0 else cookieString.split(";").size
        val embedded = web.settings.userAgentString.contains("; wv)")
        val host = Uri.parse(url).host ?: ""
        status.text = "[$state] $host | cookies: $cookieCount | wv: $embedded | zoom: ${(zoom * 100).roundToInt()}%"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }
}
