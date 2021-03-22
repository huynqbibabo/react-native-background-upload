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
      val importance = NotificationManager.IMPORTANCE_LOW
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
        description = CHANNEL_NAME
      }
      // Register the channel with the system
      val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }

  private fun getBasicNotificationBuilder(): NotificationCompat.Builder {
    return NotificationCompat.Builder(context, CHANNEL_ID).apply {
      setSmallIcon(android.R.drawable.ic_menu_upload)
      setDefaults(NotificationCompat.DEFAULT_ALL)
      setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }
  }

  fun getProgressNotificationBuilder(progress: Int): NotificationCompat.Builder {
    return getBasicNotificationBuilder().apply {
      setContentTitle("Đang tải lên...")
      setContentText("$progress%")
      setOngoing(true)
      setProgress(100, progress, false)
      priority = NotificationCompat.PRIORITY_LOW
    }
  }

  fun getCompleteNotificationBuilder(): NotificationCompat.Builder {
    return getBasicNotificationBuilder().apply {
      setContentTitle("Tải lên hoàn tất")
      setOngoing(false)
      priority = NotificationCompat.PRIORITY_LOW
    }
  }

  fun getCancelNotificationBuilder(): NotificationCompat.Builder {
    return getBasicNotificationBuilder().apply {
      setContentTitle("Tải lên đã được huỷ bỏ")
      setOngoing(false)
      priority = NotificationCompat.PRIORITY_LOW
    }
  }

  fun getFailureNotificationBuilder(): NotificationCompat.Builder {
    return getBasicNotificationBuilder().apply {
      setContentTitle("Tải lên thất bại")
      setContentText("Vui lòng thử lại sau")
      setOngoing(false)
      priority = NotificationCompat.PRIORITY_LOW
    }
  }

  fun getSplitNotificationBuilder(): NotificationCompat.Builder {
    return getBasicNotificationBuilder().apply {
      setContentTitle("Chuẩn bị tập tin media để tải lên")
      setProgress(100, 0, true)
      setOngoing(true)
      priority = NotificationCompat.PRIORITY_LOW
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
