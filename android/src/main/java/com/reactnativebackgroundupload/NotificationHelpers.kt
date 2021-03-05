package com.reactnativebackgroundupload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


class NotificationHelpers(private val context: Context) {
  private var manager: NotificationManagerCompat? = null

  companion object {
    val CHANNEL_ID = "UploadChannel"
    val CHANNEL_NAME = "Upload notification channel"
    val NOTIFICATION_ID = 1
  }

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val description = ""
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
      channel.description = description
      // Register the channel with the system; you can't change the importance
      // or other notification behaviors after this
      getManager().createNotificationChannel(channel)
    }
  }

  private fun getManager(): NotificationManagerCompat {
    if (manager == null) {
      manager = NotificationManagerCompat.from(context)
    }
    return manager!!
  }

  fun getProgressNotification(progress: Int): Notification {
    return NotificationCompat.Builder(context, CHANNEL_ID).apply {
      setContentTitle("Đang tải...")
      setOngoing(true)
      setProgress(100, progress, true)
      setDefaults(NotificationCompat.DEFAULT_ALL)
      setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      priority = NotificationCompat.PRIORITY_HIGH
    }.build()
  }

  fun notify(notification: Notification) {
    getManager().notify(NOTIFICATION_ID, notification)
  }

  fun cancelNotification() {
    getManager().cancel(NOTIFICATION_ID)
  }
}
