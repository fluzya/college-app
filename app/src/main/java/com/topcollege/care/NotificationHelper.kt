package com.topcollege.care

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_MORNING_ID = "channel_morning_digest_v2"
    const val CHANNEL_EVENING_ID = "channel_evening_skincare_v2"

    const val NOTIF_MORNING_ID = 1001
    const val NOTIF_EVENING_ID = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val morningChannel = NotificationChannel(
                CHANNEL_MORNING_ID,
                "Утренний дайджест (06:00)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Расписание пар на сегодня и утренний уход за лицом"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val eveningChannel = NotificationChannel(
                CHANNEL_EVENING_ID,
                "Вечерний уход (22:00)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Вечерний протокол ухода за лицом"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(morningChannel)
            notificationManager.createNotificationChannel(eveningChannel)
        }
    }

    fun showMorningDigest(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val today = Calendar.getInstance()
            val api = TopAcademyApi(context)
            val (lessons, _) = api.fetchSchedule(today)
            val care = SkincareManager.getRoutineForDay(today)

            val scheduleSb = StringBuilder()
            if (lessons.isEmpty()) {
                scheduleSb.append("🎉 Пар нет! Отдыхай.\n")
            } else {
                lessons.forEach { l ->
                    scheduleSb.append("${l.lesson}. ${l.startedAt}-${l.finishedAt} | ${l.subjectName}")
                    if (l.roomName.isNotEmpty()) scheduleSb.append(" (${l.roomName})")
                    scheduleSb.append("\n")
                }
            }

            val careSb = StringBuilder()
            care.morningSteps.forEach { step ->
                careSb.append("• $step\n")
            }

            val fullText = "📚 РАСПИСАНИЕ НА СЕГОДНЯ:\n$scheduleSb\n🧴 УХОД ЗА ЛИЦОМ (УТРО):\n$careSb"

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_MORNING_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🌅 ${care.title} — 06:00 Дайджест")
                .setContentText(if (lessons.isNotEmpty()) "Сегодня ${lessons.size} пар(ы). Нажми, чтобы открыть план." else "Пар нет. План ухода готов.")
                .setStyle(NotificationCompat.BigTextStyle().bigText(fullText.trim()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_MORNING_ID, notification)
        }
    }

    fun showEveningSkincare(context: Context) {
        val today = Calendar.getInstance()
        val care = SkincareManager.getRoutineForDay(today)

        val careSb = StringBuilder()
        care.eveningSteps.forEach { step ->
            careSb.append("• $step\n")
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_EVENING_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🌙 Вечерний уход (${care.title})")
            .setContentText("Время вечернего протокола ухода за кожей!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(careSb.toString().trim()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_EVENING_ID, notification)
    }

    fun scheduleAlarms(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            // 1. Утренний будильник на 06:00
            val morningIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.topcollege.care.ACTION_MORNING_DIGEST"
            }
            val morningPi = PendingIntent.getBroadcast(
                context,
                101,
                morningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val morningCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 6)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            safeSetExactAlarm(alarmManager, morningCal.timeInMillis, morningPi)

            // 2. Вечерний будильник на 22:00
            val eveningIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.topcollege.care.ACTION_EVENING_DIGEST"
            }
            val eveningPi = PendingIntent.getBroadcast(
                context,
                102,
                eveningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val eveningCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 22)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            safeSetExactAlarm(alarmManager, eveningCal.timeInMillis, eveningPi)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun safeSetExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pi: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (e: SecurityException) {
            // Если в Android 13/14 нет разрешения на точные будильники, используем обычный
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
