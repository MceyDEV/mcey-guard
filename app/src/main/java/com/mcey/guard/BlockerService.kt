package com.mcey.guard

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import android.widget.Toast

/**
 * Ön plandaki uygulamayı izleyen erişilebilirlik servisi.
 *
 * Engelli bir uygulama ön plana geldiğinde:
 *   1) Cihaz anında ana ekrana (HOME) gönderilir -> uygulama kapanır.
 *   2) Ekranda kullanıcının belirlediği uyarı yazısı gösterilir.
 */
class BlockerService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastBlockedPkg: String? = null
    private var lastBlockTime: Long = 0L

    // Aynı olayın art arda tetiklenmesini engellemek için küçük bir debounce.
    private val minIntervalMs = 600L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val pkg = event.packageName?.toString() ?: return

        // Sadece gerçek bir uygulama penceresi ile ilgilen.
        if (pkg.isBlank()) return

        if (!BlockRules.isBlocked(this, pkg)) {
            return
        }

        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPkg && now - lastBlockTime < minIntervalMs) {
            // Aynı engeli çok kısa sürede tekrar işleme.
            return
        }
        lastBlockedPkg = pkg
        lastBlockTime = now

        blockNow()
    }

    private fun blockNow() {
        // 1) Uygulamayı kapat: ana ekrana dön.
        performGlobalAction(GLOBAL_ACTION_HOME)
        // Bazı cihazlarda tek çağrı yetmeyebilir; kısa süre sonra bir daha dene.
        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 120)

        // 2) Ekranda uyarı göster.
        val message = Prefs.getMessage(this)
        showWarning(message)
    }

    private fun showWarning(message: String) {
        // Üstüne çizim izni varsa tam ekran kırmızı bir uyarı göster,
        // yoksa Toast ile göster (izin gerektirmez).
        if (Settings.canDrawOverlays(this)) {
            showOverlay(message)
        } else {
            handler.post {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showOverlay(message: String) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val tv = TextView(this).apply {
            text = message
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#E6C62828")) // yarı saydam kırmızı
            setPadding(64, 160, 64, 160)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        handler.post {
            try {
                wm.addView(tv, params)
                // 2.5 saniye sonra kaldır.
                handler.postDelayed({
                    try {
                        wm.removeView(tv)
                    } catch (_: Exception) {
                    }
                }, 2500)
            } catch (_: Exception) {
                // Overlay eklenemezse Toast'a düş.
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onInterrupt() {
        // Gerekli değil.
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Servis bağlanınca oyun listesini önceden hazırla.
        BlockRules.getGamePackages(this, forceRefresh = true)
    }
}
