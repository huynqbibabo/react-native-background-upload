package com.reactnativebackgroundupload.model

import androidx.work.Data
import androidx.work.workDataOf

class ModelClearNotification {
  companion object {
    const val KEY_NOTIFICATION_ID = "notification_id"
  }

  fun createInputDataForClearNotification(notificationId: Int): Data {
    return workDataOf(
      KEY_NOTIFICATION_ID to notificationId
    )
  }
}
