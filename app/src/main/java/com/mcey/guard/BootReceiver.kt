package com.mcey.guard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Cihaz yeniden başladığında tetiklenir.
 *
 * Erişilebilirlik servisi, bir kez etkinleştirildikten sonra yeniden başlatma
 * sonrası sistem tarafından otomatik olarak geri bağlanır. Bu alıcı, oyun
 * listesi önbelleğini tazeleyerek açılışta her şeyin hazır olmasını sağlar.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            // Oyun paketlerini yeniden tara.
            BlockRules.getGamePackages(context.applicationContext, forceRefresh = true)
        }
    }
}
