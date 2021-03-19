package com.reactnativebackgroundupload.model

import androidx.work.Data
import androidx.work.workDataOf

class ModelTranscodeInput {
  companion object {
    const val KEY_WORK_ID = "work_id"
    const val KEY_EVENT_EMITTER_CHANNEL_ID = "event_emitter_channel_id"
    const val KEY_FILE_PATH = "file_path"
    const val KEY_CHUNK_SIZE = "chunk_size"
    const val DEFAULT_CHUNK_SIZE = 10485760
  }

  fun createInputDataForTranscode(channelId: Double, notificationId: Int, filePath: String, chunkSize: Int): Data {
    return workDataOf(
      KEY_WORK_ID to notificationId,
      KEY_EVENT_EMITTER_CHANNEL_ID to channelId,
      KEY_CHUNK_SIZE to chunkSize,
      KEY_FILE_PATH to filePath
    )
  }
}

class ModelRequestMetadata {
  companion object {
    const val KEY_WORK_ID = "work_id"
    const val KEY_EVENT_EMITTER_CHANNEL_ID = "event_emitter_channel_id"
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

  fun createInputDataForUpload(notificationId: Int, channelId: Double, uploadUrl: String, metadataUrl: String): Data {
    return workDataOf(
      KEY_WORK_ID to notificationId,
      KEY_EVENT_EMITTER_CHANNEL_ID to channelId,
      KEY_METADATA_URL to metadataUrl,
      KEY_UPLOAD_URL to uploadUrl
    )
  }

  fun createInputDataForRequestTask(notificationId: Int, channelId: Double, uploadUrl: String, metadataUrl: String, taskUrl: String?, method: String?, auth: String?, data: String?): Data {
    return workDataOf(
      KEY_WORK_ID to notificationId,
      KEY_EVENT_EMITTER_CHANNEL_ID to channelId,
      KEY_METADATA_URL to metadataUrl,
      KEY_UPLOAD_URL to uploadUrl,
      KEY_CHAIN_URL to taskUrl,
      KEY_METHOD to method,
      KEY_AUTHORIZATION to auth,
      KEY_DATA to data
    )
  }
}

class ModelUploadInput {
  companion object {
    const val KEY_WORK_ID = "work_id"
    const val KEY_EVENT_EMITTER_CHANNEL_ID = "event_emitter_channel_id"
    const val KEY_NUMBER_OF_CHUNKS = "number_of_chunks"
    const val KEY_REQUEST_URL = "request_url"
    const val KEY_FILE_NAME = "file_name"
    const val KEY_FILE_PATH = "file_path"
    const val KEY_HASH = "hash"
    const val KEY_PRT = "prt"
  }

  fun createInputDataForUpload(requestUrl: String, fileName: String, filePath: String, hash: String, prt: Int, numberOfChunks: Int, notificationId: Int, channelId: Double): Data {
    return workDataOf(
      KEY_WORK_ID to notificationId,
      KEY_EVENT_EMITTER_CHANNEL_ID to channelId,
      KEY_NUMBER_OF_CHUNKS to numberOfChunks,
      KEY_REQUEST_URL to requestUrl,
      KEY_FILE_NAME to fileName,
      KEY_FILE_PATH to filePath,
      KEY_HASH to hash,
      KEY_PRT to prt
    )
  }
}

class ModelClearCache {
  companion object {
    const val KEY_CHUNK_PATH_ARRAY = "chunk_part_array"
  }

  fun createInputDataForClearCache(chunkArray: Array<String>): Data {
    return workDataOf(
      KEY_CHUNK_PATH_ARRAY to chunkArray
    )
  }
}

class ModelClearTask {
  companion object {
    const val KEY_WORK_ID = "work_id"
    const val KEY_EVENT_EMITTER_CHANNEL_ID = "event_emitter_channel_id"
    const val KEY_FILE_NAME = "file_name"
    const val KEY_CHAIN_URL = "chain_url"
    const val KEY_METHOD = "method"
    const val KEY_AUTHORIZATION = "authorization"
    const val KEY_DATA = "data"
  }

  fun createInputDataForClearTask(workId: Int, channelId: Double, filename:String, url: String?, method: String?, auth: String?, data: String?): Data {
    return workDataOf(
      KEY_WORK_ID to workId,
      ModelUploadInput.KEY_EVENT_EMITTER_CHANNEL_ID to channelId,
      KEY_FILE_NAME to filename,
      KEY_CHAIN_URL to url,
      KEY_METHOD to method,
      KEY_AUTHORIZATION to auth,
      KEY_DATA to data
    )
  }
}
