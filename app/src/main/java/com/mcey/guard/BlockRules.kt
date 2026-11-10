package com.mcey.guard

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * Bir paketin engellenip engellenmeyeceğine karar veren katman.
 *
 * Üç kaynak birleştirilir:
 *  1) Bilinen uygulama mağazaları (açık/kapalı — Prefs.KEY_BLOCK_STORES)
 *  2) APK yükleyici (açık/kapalı — Prefs.KEY_BLOCK_INSTALLER)
 *  3) Oyun kategorisindeki uygulamalar (açık/kapalı — Prefs.KEY_BLOCK_GAMES)
 *  4) Kullanıcının elle eklediği paketler (her zaman)
 */
object BlockRules {

    /** Bilinen uygulama indirme / mağaza paketleri. */
    val STORE_PACKAGES: Set<String> = setOf(
        // Google
        "com.android.vending",
        // Samsung
        "com.sec.android.app.samsungapps",
        // Huawei
        "com.huawei.appmarket",
        // Xiaomi
        "com.xiaomi.market",
        "com.xiaomi.mipicks",
        // Oppo / Realme
        "com.heytap.market",
        "com.oppo.market",
        // Vivo
        "com.bbk.appstore",
        "com.vivo.appstore",
        // Amazon
        "com.amazon.venezia",
        "com.amazon.mShop.android.shopping",
        // Üçüncü taraf mağazalar
        "cm.aptoide.pt",
        "com.aptoide.partners",
        "com.apkpure.aegon",
        "com.uptodown",
        "com.farsitel.bazaar",   // Cafe Bazaar
        "ir.mservices.market",   // Myket
        "org.fdroid.fdroid",
        "com.qooapp.qoohelper",
        "me.onemobile.android",
        "com.mobile.indiapp",    // 9Apps
        "com.tapzapp.store",
        "com.getjar.rewards"
    )

    /** APK kurulumunu gerçekleştiren sistem yükleyicileri. */
    val INSTALLER_PACKAGES: Set<String> = setOf(
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.miui.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.huawei.packageinstaller",
        "com.oppo.packageinstaller",
        "com.vivo.packageinstaller"
    )

    // Oyun paketleri önbelleği (PackageManager taraması pahalı olduğu için).
    @Volatile
    private var gameCache: Set<String> = emptySet()

    @Volatile
    private var gameCacheBuiltAt: Long = 0L
    private const val GAME_CACHE_TTL_MS = 5 * 60 * 1000L // 5 dakika

    /**
     * Yüklü uygulamalar içinden oyun kategorisindekileri bulur.
     * API 26+ -> ApplicationInfo.category == CATEGORY_GAME.
     */
    @Synchronized
    fun getGamePackages(ctx: Context, forceRefresh: Boolean = false): Set<String> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && gameCache.isNotEmpty() &&
            now - gameCacheBuiltAt < GAME_CACHE_TTL_MS
        ) {
            return gameCache
        }

        val pm = ctx.packageManager
        val result = HashSet<String>()
        try {
            val apps = pm.getInstalledApplications(0)
            for (app in apps) {
                if (isGame(app)) {
                    result.add(app.packageName)
                }
            }
        } catch (e: Exception) {
            // Herhangi bir hata olursa önceki önbelleği koru.
            return gameCache
        }

        gameCache = result
        gameCacheBuiltAt = now
        return result
    }

    @Suppress("DEPRECATION")
    private fun isGame(app: ApplicationInfo): Boolean {
        // Modern yol: uygulama kategorisi.
        if (app.category == ApplicationInfo.CATEGORY_GAME) return true
        // Eski "oyun" bayrağı (çok eski uygulamalarda).
        if (app.flags and ApplicationInfo.FLAG_IS_GAME != 0) return true
        return false
    }

    fun invalidateGameCache() {
        gameCacheBuiltAt = 0L
    }

    /**
     * Verilen paketin şu anki ayarlara göre engellenip engellenmeyeceği.
     */
    fun isBlocked(ctx: Context, pkg: String?): Boolean {
        if (pkg.isNullOrEmpty()) return false
        if (!Prefs.isProtectionOn(ctx)) return false

        // Kendimizi ve çekirdek sistem arayüzünü asla engelleme.
        if (pkg == ctx.packageName) return false
        if (pkg in SAFE_PACKAGES) return false

        if (Prefs.isBlockStores(ctx) && pkg in STORE_PACKAGES) return true
        if (Prefs.isBlockInstaller(ctx) && pkg in INSTALLER_PACKAGES) return true
        if (pkg in Prefs.getCustomPackages(ctx)) return true
        if (Prefs.isBlockGames(ctx) && pkg in getGamePackages(ctx)) return true

        return false
    }

    /** Kilitlenmesi tehlikeli olan temel sistem bileşenleri. */
    private val SAFE_PACKAGES: Set<String> = setOf(
        "com.android.systemui",
        "android",
        "com.android.settings"
    )
}
