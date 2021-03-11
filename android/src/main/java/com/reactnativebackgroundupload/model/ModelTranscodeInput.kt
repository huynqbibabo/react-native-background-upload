package com.reactnativebackgroundupload.model

import androidx.work.Data
import androidx.work.workDataOf

class ModelTranscodeInput {
  companion object {
    const val KEY_FILE_PATH = "file_path"
    const val KEY_NOTIFICATION_ID = "notification_id"
    const val KEY_CHUNK_SIZE = "chunk_size"
    const val DEFAULT_CHUNK_SIZE = 2621440
  }

  fun createInputDataForTranscode(filePath: String, chunkSize: Int, notificationId: Int): Data {
    return workDataOf(
      KEY_NOTIFICATION_ID to notificationId,
      KEY_CHUNK_SIZE to chunkSize,
      KEY_FILE_PATH to filePath
    )
  }
}
