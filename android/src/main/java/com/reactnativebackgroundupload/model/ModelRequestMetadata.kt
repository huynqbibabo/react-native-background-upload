package com.reactnativebackgroundupload.model

import androidx.work.Data
import androidx.work.workDataOf

class ModelRequestMetadata {
  companion object {
    const val KEY_NOTIFICATION_ID = "notification_id"
    const val KEY_CHUNK_PATH_ARRAY = "chunk_part_array"
    const val KEY_METADATA_URL = "metadata_url"
    const val KEY_UPLOAD_URL = "upload_url"
    const val KEY_CHAIN_URL = "chain_url"
    const val KEY_METHOD = "method"
    const val KEY_AUTHORIZATION = "authorization"
    const val KEY_DATA = "data"
  }

  fun createInputDataForRequestMetadata(chunkArray: Array<String>): Data {
    return workDataOf(
      KEY_CHUNK_PATH_ARRAY to chunkArray
    )
  }

  fun createInputDataForUpload(notificationId: Int, uploadUrl: String, metadataUrl: String): Data {
    return workDataOf(
      KEY_NOTIFICATION_ID to notificationId,
      KEY_METADATA_URL to metadataUrl,
      KEY_UPLOAD_URL to uploadUrl
    )
  }

  fun createInputDataForRequestTask(notificationId: Int, uploadUrl: String, metadataUrl: String, taskUrl: String?, method: String?, auth: String?, data: String?): Data {
    return workDataOf(
      KEY_NOTIFICATION_ID to notificationId,
      KEY_METADATA_URL to metadataUrl,
      KEY_UPLOAD_URL to uploadUrl,
      KEY_CHAIN_URL to taskUrl,
      KEY_METHOD to method,
      KEY_AUTHORIZATION to auth,
      KEY_DATA to data
    )
  }
}
