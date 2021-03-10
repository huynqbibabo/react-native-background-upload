package com.reactnativebackgroundupload.model

import androidx.work.Data
import androidx.work.workDataOf

class ModelRequestMetadata {
  companion object {
    const val KEY_NOTIFICATION_ID = "notification_id"
    const val KEY_CHUNK_PATH_ARRAY = "chunk_part_array"
    const val KEY_METADATA_URL = "metadata_url"
    const val KEY_UPLOAD_URL = "upload_url"
  }

  fun createInputDataForRequestMetadata(notificationId: Int, chunkArray: Array<String>, uploadUrl: String, metadataUrl: String): Data {
    return workDataOf(
      KEY_NOTIFICATION_ID to notificationId,
      KEY_CHUNK_PATH_ARRAY to chunkArray,
      KEY_METADATA_URL to metadataUrl,
      KEY_UPLOAD_URL to uploadUrl
    )
  }
}
