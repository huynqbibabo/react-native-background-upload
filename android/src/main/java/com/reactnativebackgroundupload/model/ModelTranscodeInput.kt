package com.reactnativebackgroundupload.model

import androidx.work.Data
import androidx.work.workDataOf

class ModelTranscodeInput {
  companion object {
    const val KEY_FILE_PATH = "file_path"
    const val KEY_NOTIFICATION_ID = "notification_id"
    const val KEY_METADATA_URL = "metadata_url"
    const val KEY_UPLOAD_URL = "upload_url"
    const val KEY_CHUNK_SIZE = "chunk_size"
    const val DEFAULT_CHUNK_SIZE = 2621440
  }

  fun createInputDataForCompress(filePath: String, chunkSize: Int, uploadUrl: String, metadataUrl: String, notificationId: Int): Data {
    return workDataOf(
      KEY_UPLOAD_URL to uploadUrl,
      KEY_METADATA_URL to metadataUrl,
      KEY_NOTIFICATION_ID to notificationId,
      KEY_CHUNK_SIZE to chunkSize,
      KEY_FILE_PATH to filePath
    )
  }
}
