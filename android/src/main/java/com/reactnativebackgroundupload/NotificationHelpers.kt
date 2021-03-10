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
    const val CHANNEL_ID = "UploadChannel"
    const val CHANNEL_NAME = "Upload notification channel"
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

  fun getProgressNotificationBuilder(progress: Int): NotificationCompat.Builder {
    return NotificationCompat.Builder(context, CHANNEL_ID).apply {
      setContentTitle("Đang tải lên...")
      setContentText("$progress%")
      setSmallIcon(android.R.drawable.stat_notify_sync)
      setOngoing(true)
      setProgress(100, progress, false)
      setDefaults(NotificationCompat.DEFAULT_ALL)
      setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      priority = NotificationCompat.PRIORITY_DEFAULT
    }
  }

  fun getCompleteNotificationBuilder(): NotificationCompat.Builder {
    return NotificationCompat.Builder(context, CHANNEL_ID).apply {
      setContentTitle("Tải lên hoàn tất")
      setContentText("Hoàn thành")
      setSmallIcon(android.R.drawable.stat_notify_sync)
      setOngoing(false)
      setDefaults(NotificationCompat.DEFAULT_ALL)
      setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      priority = NotificationCompat.PRIORITY_DEFAULT
    }
  }

  fun getFailureNotificationBuilder(): NotificationCompat.Builder {
    return NotificationCompat.Builder(context, CHANNEL_ID).apply {
      setContentTitle("Tải lên thất bại")
      setContentText("Vui lòng thử lại sau")
      setSmallIcon(android.R.drawable.stat_notify_sync)
      setOngoing(false)
      setDefaults(NotificationCompat.DEFAULT_ALL)
      setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      priority = NotificationCompat.PRIORITY_DEFAULT
    }
  }

  fun getSplitNotificationBuilder(): NotificationCompat.Builder {
    return NotificationCompat.Builder(context, CHANNEL_ID).apply {
      setContentTitle("Chuẩn bị file để tải lên")
//      setContentText("Hoàn thành")
      setSmallIcon(android.R.drawable.ic_menu_upload)
      setProgress(100, 0, true)
      setOngoing(false)
      setDefaults(NotificationCompat.DEFAULT_ALL)
      setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      priority = NotificationCompat.PRIORITY_DEFAULT
    }
  }

  fun startNotify(notificationId: Int, notification: Notification) {
    with(NotificationManagerCompat.from(context)) {
      notify(notificationId, notification)
    }
  }

  fun cancelNotification(notificationId: Int) {
    with(NotificationManagerCompat.from(context)) {
      cancel(notificationId)
    }
  }
}
