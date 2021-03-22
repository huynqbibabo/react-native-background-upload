package com.reactnativebackgroundupload.worker

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.reactnativebackgroundupload.model.ModelClearCache
import java.io.File

class ClearCacheWorker(
  context: Context,
  params: WorkerParameters
) : ListenableWorker(context, params) {
  override fun startWork(): ListenableFuture<Result> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
      val chunkPaths = inputData.getStringArray(ModelClearCache.KEY_CHUNK_PATH_ARRAY)!!
      chunkPaths.forEach { chunk ->
        val file = File(chunk)
        if (file.exists()) {
          file.delete()
        }
      }
      completer.set(Result.success())
    }
  }
}
