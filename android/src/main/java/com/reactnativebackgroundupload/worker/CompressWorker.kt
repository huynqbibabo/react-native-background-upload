package com.reactnativebackgroundupload.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.reactnativebackgroundupload.NotificationHelpers
import com.reactnativebackgroundupload.util.RealPathUtil
import com.reactnativebackgroundupload.model.ModelTranscodeInput
import com.reactnativebackgroundupload.videoCompressor.CompressionListener
import com.reactnativebackgroundupload.videoCompressor.Compressor
import com.reactnativebackgroundupload.videoCompressor.VideoCompressor
import com.reactnativebackgroundupload.videoCompressor.VideoQuality

internal interface CompressCallback {
  fun success(outputPath: String)
  fun failure()
  fun retry()
}

class CompressWorker(
  context: Context,
  params: WorkerParameters
) : ListenableWorker(context, params) {
  private val mNotificationHelpers = NotificationHelpers(applicationContext)

  override fun startWork(): ListenableFuture<Result> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
      val notificationId = inputData.getInt(ModelTranscodeInput.KEY_NOTIFICATION_ID, 1)
      val chunkSize = inputData.getInt(ModelTranscodeInput.KEY_CHUNK_SIZE, ModelTranscodeInput.DEFAULT_CHUNK_SIZE)
      val filePath = inputData.getString(ModelTranscodeInput.KEY_FILE_PATH)
      val uploadUrl = inputData.getString(ModelTranscodeInput.KEY_UPLOAD_URL)!!
      val metadataUrl = inputData.getString(ModelTranscodeInput.KEY_METADATA_URL)!!

      val callback: CompressCallback = object : CompressCallback {
        override fun success(outputPath: String) {
          completer.set(Result.success(
            ModelTranscodeInput().createInputDataForCompress(outputPath, chunkSize, uploadUrl, metadataUrl, notificationId)
          ))
        }

        override fun failure() {
          mNotificationHelpers.startNotify(
            notificationId,
            mNotificationHelpers.getFailureNotificationBuilder().build()
          )
          completer.set(Result.failure())
        }

        override fun retry() {
          if (runAttemptCount > 2) {
            completer.set(Result.failure())
          } else {
            completer.set(Result.retry())
          }
        }
      }
      compressVideo(filePath, notificationId, callback)
      callback
    }
  }

  private fun compressVideo(filePath: String?, notificationId: Int, callback: CompressCallback) {
    if (filePath == null) {
      callback.failure()
    } else {
      val inputPath = RealPathUtil.getRealPath(applicationContext, Uri.parse(filePath))
      val outputPath = "${applicationContext.getExternalFilesDir(null)}/${System.currentTimeMillis()}.mp4"

      VideoCompressor.start(
        inputPath,
        outputPath,
        object : CompressionListener {
          override fun onProgress(percent: Float) {
            // Update notification with progress value
            if (percent <= 100 && percent.toInt() % 5 == 0) {
//              Log.d("COMPRESSION", "Compression progress: ${percent.toInt()}")
              mNotificationHelpers.startNotify(
                notificationId,
                mNotificationHelpers.getProgressNotificationBuilder(percent.toInt()).setContentTitle("Đang nén video...").build()
              )
            }
          }
          override fun onStart() {
            // Compression start
            Log.d("COMPRESSION", "Compression start")
          }
          override fun onSuccess() {
            // On Compression success
            Log.d("COMPRESSION", "Compression success")
            callback.success(outputPath)
          }
          override fun onFailure(failureMessage: String) {
            // On Failure
            Log.wtf("COMPRESSION", failureMessage)
            if (failureMessage == Compressor.INVALID_BITRATE) {
              callback.success(inputPath)
            } else {
              callback.failure()
            }
          }
          override fun onCancelled() {
            // On Cancelled
            Log.d("COMPRESSION", "Compression cancelled")
            callback.failure()
          }
        }, VideoQuality.MEDIUM, isMinBitRateEnabled = true, keepOriginalResolution = false)
    }
  }
}
