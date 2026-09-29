package com.topcollege.care

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.topcollege.care.ACTION_MORNING_DIGEST" -> {
                NotificationHelper.showMorningDigest(context)
                // Перепланируем на следующий день
                NotificationHelper.scheduleAlarms(context)
            }
            "com.topcollege.care.ACTION_EVENING_DIGEST" -> {
                NotificationHelper.showEveningSkincare(context)
                // Перепланируем на следующий день
                NotificationHelper.scheduleAlarms(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Восстановление будильников после перезагрузки смартфона
                NotificationHelper.scheduleAlarms(context)
            }
        }
    }
}
