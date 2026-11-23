package com.cursestudio.test

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private lateinit var status: TextView

    private val homeUrl = "https://www.curseforge.com/"
    private val authorsUrl = "https://authors.curseforge.com/"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun addButton(label: String, onClick: () -> Unit) {
            buttons.addView(
                Button(this).apply {
                    text = label
                    isAllCaps = false
                    setOnClickListener { onClick() }
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
        }
        addButton("Home") { web.loadUrl(homeUrl) }
        addButton("Authors") { web.loadUrl(authorsUrl) }
        addButton("Reload") { web.reload() }
        root.addView(buttons)

        status = TextView(this).apply {
            textSize = 12f
            setPadding(24, 8, 24, 8)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        root.addView(status)

        web = WebView(this)
        root.addView(
            web,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
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

    private fun updateStatus(url: String, state: String) {
        val cookieString = CookieManager.getInstance().getCookie("https://www.curseforge.com") ?: ""
        val cookieCount = if (cookieString.isBlank()) 0 else cookieString.split(";").size
        val embedded = web.settings.userAgentString.contains("; wv)")
        status.text = "[$state] ${Uri.parse(url).host} | cookies: $cookieCount | wv: $embedded"
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
