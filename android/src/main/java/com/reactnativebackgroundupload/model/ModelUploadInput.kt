package com.reactnativebackgroundupload.model

import androidx.work.Data
import androidx.work.workDataOf

class ModelUploadInput {
  companion object {
    val KEY_NUMBER_OF_CHUNKS = "number_of_chunks"
    val KEY_REQUEST_URL = "request_url"
    val KEY_FILE_NAME = "file_name"
    val KEY_FILE_PATH = "file_path"
    val KEY_HASH = "hash"
    val KEY_PRT = "prt"
  }

  fun createInputDataForUpload(requestUrl: String, fileName: String, filePath: String, hash: String, prt: Int, numberOfChunks: Int): Data {
    return workDataOf(
      KEY_NUMBER_OF_CHUNKS to numberOfChunks,
      KEY_REQUEST_URL to requestUrl,
      KEY_FILE_NAME to fileName,
      KEY_FILE_PATH to filePath,
      KEY_HASH to hash,
      KEY_PRT to prt
    )
  }
}
