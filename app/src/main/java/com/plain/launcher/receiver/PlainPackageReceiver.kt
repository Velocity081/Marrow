package com.plain.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.plain.launcher.PlainApplication

class PlainPackageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? PlainApplication ?: return
        app.appRepository.reloadApps()
    }
}
