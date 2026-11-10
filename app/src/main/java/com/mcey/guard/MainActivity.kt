package com.mcey.guard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mcey.guard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Switch başlangıç değerleri
        b.swProtection.isChecked = Prefs.isProtectionOn(this)
        b.swStores.isChecked = Prefs.isBlockStores(this)
        b.swGames.isChecked = Prefs.isBlockGames(this)
        b.swInstaller.isChecked = Prefs.isBlockInstaller(this)
        b.editMessage.setText(Prefs.getMessage(this))

        b.swProtection.setOnCheckedChangeListener { _, v -> Prefs.setProtectionOn(this, v) }
        b.swStores.setOnCheckedChangeListener { _, v -> Prefs.setBlockStores(this, v) }
        b.swGames.setOnCheckedChangeListener { _, v ->
            Prefs.setBlockGames(this, v)
            if (v) BlockRules.getGamePackages(this, forceRefresh = true)
        }
        b.swInstaller.setOnCheckedChangeListener { _, v -> Prefs.setBlockInstaller(this, v) }

        b.btnOpenAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        b.btnSaveMessage.setOnClickListener {
            val text = b.editMessage.text.toString().trim()
            if (text.isEmpty()) {
                Prefs.setMessage(this, getString(R.string.default_block_message))
            } else {
                Prefs.setMessage(this, text)
            }
            Toast.makeText(this, R.string.toast_message_saved, Toast.LENGTH_SHORT).show()
        }

        b.btnPickApps.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }

        b.btnOverlayPermission.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.toast_overlay_already, Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateAccessibilityStatus()
    }

    private fun updateAccessibilityStatus() {
        val enabled = isAccessibilityServiceEnabled()
        if (enabled) {
            b.txtAccessibilityStatus.setBackgroundColor(0xFFD4EDDA.toInt())
            b.txtAccessibilityStatus.text = getString(R.string.status_on)
            b.btnOpenAccessibility.text = getString(R.string.btn_open_accessibility_settings)
        } else {
            b.txtAccessibilityStatus.setBackgroundColor(0xFFF8D7DA.toInt())
            b.txtAccessibilityStatus.text = getString(R.string.status_off)
            b.btnOpenAccessibility.text = getString(R.string.btn_open_accessibility)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedId = "$packageName/${BlockerService::class.java.name}"

        // Önce resmi API, sonra ayarlar dizesi (bazı cihazlarda farklı davranır).
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (info in enabledServices) {
            val id = info.id ?: continue
            if (id.equals(expectedId, ignoreCase = true)) return true
        }

        val setting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(setting)
        for (part in splitter) {
            if (part.equals(expectedId, ignoreCase = true)) return true
        }
        return false
    }
}
