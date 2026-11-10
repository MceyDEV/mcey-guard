package com.mcey.guard

import android.content.Context

/**
 * Tüm ayarların tutulduğu basit SharedPreferences sarmalayıcısı.
 */
object Prefs {
    private const val FILE = "mcey_guard_prefs"

    const val KEY_BLOCK_STORES = "block_stores"
    const val KEY_BLOCK_GAMES = "block_games"
    const val KEY_BLOCK_INSTALLER = "block_installer"
    const val KEY_CUSTOM_PACKAGES = "custom_packages"
    const val KEY_MESSAGE = "warning_message"
    const val KEY_PROTECTION_ON = "protection_on"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // Ana koruma anahtarı (kapalıyken hiçbir engelleme yapılmaz)
    fun isProtectionOn(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_PROTECTION_ON, true)

    fun setProtectionOn(ctx: Context, value: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_PROTECTION_ON, value).apply()

    fun isBlockStores(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_BLOCK_STORES, true)

    fun setBlockStores(ctx: Context, value: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_BLOCK_STORES, value).apply()

    fun isBlockGames(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_BLOCK_GAMES, true)

    fun setBlockGames(ctx: Context, value: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_BLOCK_GAMES, value).apply()

    fun isBlockInstaller(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_BLOCK_INSTALLER, false)

    fun setBlockInstaller(ctx: Context, value: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_BLOCK_INSTALLER, value).apply()

    fun getMessage(ctx: Context): String =
        prefs(ctx).getString(KEY_MESSAGE, null)
            ?: ctx.getString(R.string.default_block_message)

    fun setMessage(ctx: Context, value: String) =
        prefs(ctx).edit().putString(KEY_MESSAGE, value).apply()

    /** Kullanıcının elle seçtiği ek engelli paketler. */
    fun getCustomPackages(ctx: Context): MutableSet<String> =
        HashSet(prefs(ctx).getStringSet(KEY_CUSTOM_PACKAGES, emptySet()) ?: emptySet())

    fun setCustomPackages(ctx: Context, packages: Set<String>) =
        prefs(ctx).edit().putStringSet(KEY_CUSTOM_PACKAGES, HashSet(packages)).apply()

    fun addCustomPackage(ctx: Context, pkg: String) {
        val set = getCustomPackages(ctx)
        set.add(pkg)
        setCustomPackages(ctx, set)
    }

    fun removeCustomPackage(ctx: Context, pkg: String) {
        val set = getCustomPackages(ctx)
        set.remove(pkg)
        setCustomPackages(ctx, set)
    }
}
