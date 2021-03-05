package com.reactnativebackgroundupload.model

import androidx.work.Data

class ModelUploadInput {

  val KEY_REQUEST_URL = "request_url"
  val KEY_FILE_NAME = "file_name"
  val KEY_FILE_PATH = "file_path"
  val KEY_HASH = "hash"
  val KEY_PRT = "prt"

  fun createInputDataForUpload(url: String, fileName: String, filePath: String, hash: String, prt: String): Data {
    val builder = Data.Builder()
    builder.putString(KEY_REQUEST_URL, url)
    builder.putString(KEY_FILE_NAME, fileName)
    builder.putString(KEY_FILE_PATH, filePath)
    builder.putString(KEY_HASH, hash)
    builder.putString(KEY_PRT, prt)
    return builder.build()
  }
}
