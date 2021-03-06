package com.reactnativebackgroundupload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelpers(private val context: Context) {

  companion object {
    val CHANNEL_ID = "UploadChannel"
    val CHANNEL_NAME = "Upload notification channel"
    val NOTIFICATION_ID = 1
  }

  fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
        description = CHANNEL_NAME
      }
      // Register the channel with the system
      val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }

  fun getProgressNotification(progress: Int, icon: Int): Notification {
    return NotificationCompat.Builder(context, CHANNEL_ID).apply {
      setContentTitle("Đang tải lên...")
      setContentText("Download in progress")
      setSmallIcon(icon)
      setOngoing(true)
      setProgress(100, progress, true)
      setDefaults(NotificationCompat.DEFAULT_ALL)
      setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      priority = NotificationCompat.PRIORITY_HIGH
    }.build()
  }

  fun notify(notification: Notification) {
    with(NotificationManagerCompat.from(context)) {
      notify(1, notification)
    }
  }

  fun cancelNotification() {
    with(NotificationManagerCompat.from(context)) {
      cancel(NOTIFICATION_ID)
    }
  }
}
