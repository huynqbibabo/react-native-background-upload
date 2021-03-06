package com.reactnativebackgroundupload.worker

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.reactnativebackgroundupload.EventEmitter
import com.reactnativebackgroundupload.NotificationHelpers
import com.reactnativebackgroundupload.model.ModelRequestMetadata
import com.reactnativebackgroundupload.model.ModelTranscodeInput
import java.io.*

class SplitWorker(
  context: Context,
  params: WorkerParameters
) : ListenableWorker(context, params) {
  private val mNotificationHelpers = NotificationHelpers(applicationContext)
  private val workId = inputData.getInt(ModelTranscodeInput.KEY_WORK_ID, 1)

//  override fun onStopped() {
//    mNotificationHelpers.startNotify(
//      notificationId,
//      mNotificationHelpers.getFailureNotificationBuilder().build()
//    )
//    Log.d("METADATA", "stop")
//  }

  override fun startWork(): ListenableFuture<Result> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
      try {
        EventEmitter().onStateChange(workId, EventEmitter.STATE.SPLIT, "start", 0)
        var chunkSize = inputData.getInt(ModelTranscodeInput.KEY_CHUNK_SIZE, ModelTranscodeInput.DEFAULT_CHUNK_SIZE)
        val filePath = inputData.getString(ModelTranscodeInput.KEY_FILE_PATH)!!

        val mNotificationHelpers = NotificationHelpers(applicationContext)
        mNotificationHelpers.startNotify(workId, mNotificationHelpers.getSplitNotificationBuilder().build())

        val file = File(filePath)
        val result: MutableList<String> = ArrayList()
        val currentTimeMillis = System.currentTimeMillis()

        // check if file size still too large after compression
        val fileSize = file.length().toInt()
        if (fileSize > ModelTranscodeInput.DEFAULT_CHUNK_SIZE * 10) {
          chunkSize = ModelTranscodeInput.DEFAULT_CHUNK_SIZE
        }

        // check whether to split video into chunks or not
        if (fileSize > chunkSize) {
          // split file into chunks and add paths to result array
          var partCounter = 1
          val buffer = ByteArray(chunkSize) // create a buffer of bytes sized as the one chunk size
          val fis = FileInputStream(file)
          val bis = BufferedInputStream(fis)
          var tmp = 0
          while (bis.read(buffer).also { tmp = it } > 0) {
            val newFile = File(applicationContext.getExternalFilesDir(null), "${currentTimeMillis}.${String.format("%03d", partCounter++)}") // naming files as <inputFileName>.001, <inputFileName>.002, ...
            val out = FileOutputStream(newFile)
            out.write(buffer, 0, tmp) //tmp is chunk size. Need it for the last chunk, which could be less then 1 mb.
            out.close()
            result.add(newFile.path)
          }
          bis.close()
          fis.close()
        } else {
          // use compressed video file path instead
          result.add(filePath)
        }
        if (isStopped) {
          EventEmitter().onStateChange(workId, EventEmitter.STATE.CANCELLED, "cancelled at split state", 0)
          mNotificationHelpers.startNotify(
            workId,
            mNotificationHelpers.getCancelNotificationBuilder().build()
          )
          completer.set(Result.failure())
        } else {
          completer.set(Result.success(
            ModelRequestMetadata().createInputDataForRequestMetadata(result.toTypedArray())
          ))
        }
      } catch (e: IOException) {
        Log.e("SPLIT", "IOException", e)
        EventEmitter().onStateChange(workId, EventEmitter.STATE.FAILED, "IOException at split state", 0)
        mNotificationHelpers.startNotify(
          workId,
          mNotificationHelpers.getFailureNotificationBuilder().build()
        )
        completer.set(Result.failure())
      }
    }
  }
}
