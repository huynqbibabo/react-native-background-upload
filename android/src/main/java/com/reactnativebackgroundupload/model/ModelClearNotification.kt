package com.reactnativebackgroundupload.model

import androidx.work.Data
import androidx.work.workDataOf

class ModelClearNotification {
  companion object {
    const val KEY_NOTIFICATION_ID = "notification_id"
    const val KEY_FILE_NAME = "file_name"
    const val KEY_CHAIN_URL = "chain_url"
    const val KEY_METHOD = "method"
    const val KEY_AUTHORIZATION = "authorization"
    const val KEY_DATA = "data"
  }

  fun createInputDataForClearNotification(notificationId: Int, filename:String, url: String?, method: String?, auth: String?, data: String?): Data {
    return workDataOf(
      KEY_NOTIFICATION_ID to notificationId,
      KEY_FILE_NAME to filename,
      KEY_CHAIN_URL to url,
      KEY_METHOD to method,
      KEY_AUTHORIZATION to auth,
      KEY_DATA to data
    )
  }
}
