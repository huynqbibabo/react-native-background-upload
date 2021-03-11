package com.reactnativebackgroundupload.model

import androidx.work.Data
import androidx.work.workDataOf

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
