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
    const val HIGH_CHANNEL_ID = "UploadHighChannel"
    const val HIGH_CHANNEL_NAME = "Upload notification high channel"
    const val LOW_CHANNEL_ID = "UploadLowChannel"
    const val LOW_CHANNEL_NAME = "Upload notification low channel"
  }

  fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      // set up high importance channel
      val importanceHigh = NotificationManager.IMPORTANCE_HIGH
      val channelHigh = NotificationChannel(HIGH_CHANNEL_ID, HIGH_CHANNEL_NAME, importanceHigh).apply {
        description = HIGH_CHANNEL_NAME
      }
      //set up low importance channel
      val importanceLow = NotificationManager.IMPORTANCE_LOW
      val channelLow = NotificationChannel(LOW_CHANNEL_ID, LOW_CHANNEL_NAME, importanceLow).apply {
        description = LOW_CHANNEL_NAME
      }
      // Register the channel with the system
      val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channelLow)
      notificationManager.createNotificationChannel(channelHigh)
    }
  }

  private fun getBasicNotificationBuilder(channelId: String = HIGH_CHANNEL_ID): NotificationCompat.Builder {
    return NotificationCompat.Builder(context, channelId).apply {
      setSmallIcon(android.R.drawable.ic_menu_upload)
      setDefaults(NotificationCompat.DEFAULT_ALL)
      setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }
  }

  fun getProgressNotificationBuilder(progress: Int): NotificationCompat.Builder {
    return getBasicNotificationBuilder(LOW_CHANNEL_ID).apply {
      setContentTitle("Đang tải lên...")
      setContentText("$progress%")
      setOngoing(true)
      setProgress(100, progress, false)
      priority = NotificationCompat.PRIORITY_LOW
    }
  }

  fun getStartNotificationBuilder(): NotificationCompat.Builder {
    return getBasicNotificationBuilder().apply {
      setContentTitle("Tải lên hoàn tất")
      setOngoing(false)
      priority = NotificationCompat.PRIORITY_HIGH
    }
  }

  fun getCompleteNotificationBuilder(): NotificationCompat.Builder {
    return getBasicNotificationBuilder().apply {
      setContentTitle("Tải lên hoàn tất")
      setOngoing(false)
      priority = NotificationCompat.PRIORITY_HIGH
    }
  }

  fun getCancelNotificationBuilder(): NotificationCompat.Builder {
    return getBasicNotificationBuilder().apply {
      setContentTitle("Tải lên đã được huỷ bỏ")
      setOngoing(false)
      priority = NotificationCompat.PRIORITY_HIGH
    }
  }

  fun getFailureNotificationBuilder(): NotificationCompat.Builder {
    return getBasicNotificationBuilder().apply {
      setContentTitle("Tải lên thất bại")
      setContentText("Vui lòng thử lại sau")
      setOngoing(false)
      priority = NotificationCompat.PRIORITY_HIGH
    }
  }

  fun getSplitNotificationBuilder(): NotificationCompat.Builder {
    return getBasicNotificationBuilder(LOW_CHANNEL_ID).apply {
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
