package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.LiturgicalHour
import java.util.Calendar

class PrayerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val hourName = intent.getStringExtra("hour_name") ?: return
        val displayName = intent.getStringExtra("hour_display_name") ?: "Liturgy of the Hours"
        Log.d("PrayerReceiver", "Received broadcast reminder for $hourName")
        showNotification(context, hourName, displayName)
    }

    companion object {
        fun showNotification(context: Context, hourName: String, displayName: String) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "prayer_reminders_channel"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Prayer Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders to pray the Liturgy of the Hours"
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.example.ACTION_LAUNCH_BREVIARY"
                setPackage(context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("hour_name", hourName)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                hourName.hashCode(),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Time for $displayName")
                .setContentText("Step away for a few minutes of custom prayer and reflection.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
                
            notificationManager.notify(hourName.hashCode(), notification)
        }
    }
}

object PrayerReminderScheduler {
    
    fun setRemindersEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("breviary_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("reminders_enabled", enabled).apply()
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        LiturgicalHour.entries.forEach { hour ->
            val intent = Intent(context, PrayerReminderReceiver::class.java).apply {
                putExtra("hour_name", hour.name)
                putExtra("hour_display_name", hour.displayName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                hour.ordinal + 1000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            if (enabled) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour.startHour)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // If the scheduled time is in the past, schedule for tomorrow
                if (calendar.timeInMillis < System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                
                try {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                    Log.d("PrayerScheduler", "Scheduled daily alarm for ${hour.displayName} at ${hour.startHour}:00")
                } catch (e: Exception) {
                    Log.e("PrayerScheduler", "Failed to schedule repeating alarm", e)
                }
            } else {
                try {
                    alarmManager.cancel(pendingIntent)
                    Log.d("PrayerScheduler", "Cancelled daily alarm for ${hour.displayName}")
                } catch (e: Exception) {
                    Log.e("PrayerScheduler", "Failed to cancel alarm", e)
                }
            }
        }
    }

    fun isRemindersEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("breviary_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("reminders_enabled", false)
    }
}
