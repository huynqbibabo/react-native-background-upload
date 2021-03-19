package com.reactnativebackgroundupload.worker

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.reactnativebackgroundupload.EventEmitter
import com.reactnativebackgroundupload.NotificationHelpers
import com.reactnativebackgroundupload.model.ModelTranscodeInput
import com.reactnativebackgroundupload.videoCompressor.CompressionListener
import com.reactnativebackgroundupload.videoCompressor.Compressor
import com.reactnativebackgroundupload.videoCompressor.VideoCompressor
import com.reactnativebackgroundupload.videoCompressor.VideoQuality

internal interface CompressCallback {
  fun success(outputPath: String)
  fun failure()
  fun retry()
  fun cancel()
}

class CompressWorker(
  context: Context,
  params: WorkerParameters
) : ListenableWorker(context, params) {
  private val mNotificationHelpers = NotificationHelpers(applicationContext)
  private val workId = inputData.getInt(ModelTranscodeInput.KEY_WORK_ID, 1)
  private val channelId = inputData.getDouble(ModelTranscodeInput.KEY_EVENT_EMITTER_CHANNEL_ID, 1.0)

  override fun startWork(): ListenableFuture<Result> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
      EventEmitter().onStateChange(channelId, workId, EventEmitter.STATE.TRANSCODE)
      val chunkSize = inputData.getInt(ModelTranscodeInput.KEY_CHUNK_SIZE, ModelTranscodeInput.DEFAULT_CHUNK_SIZE)
      val filePath = inputData.getString(ModelTranscodeInput.KEY_FILE_PATH)

      val callback: CompressCallback = object : CompressCallback {
        override fun success(outputPath: String) {
          completer.set(Result.success(
            ModelTranscodeInput().createInputDataForTranscode(channelId, workId, outputPath, chunkSize)
          ))
        }
        override fun failure() {
          EventEmitter().onStateChange(channelId, workId, EventEmitter.STATE.FAILED)
          mNotificationHelpers.startNotify(
            workId,
            mNotificationHelpers.getFailureNotificationBuilder().build()
          )
          completer.set(Result.failure())
        }
        override fun cancel() {
          EventEmitter().onStateChange(channelId, workId, EventEmitter.STATE.CANCELLED)
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
      compressVideo(filePath, workId, callback)
      callback
    }
  }
  override fun onStopped() {
    Log.d("COMPRESSION", "stop")
    VideoCompressor.cancel()
  }

  private fun compressVideo(inputPath: String?, notificationId: Int, callback: CompressCallback) {
    if (inputPath == null) {
      callback.failure()
    } else {
      val outputPath = "${applicationContext.getExternalFilesDir(null)}/${System.currentTimeMillis()}.mp4"
      VideoCompressor.start(
        inputPath,
        outputPath,
        object : CompressionListener {
          override fun onProgress(percent: Float) {
            // Update notification with progress value
            val progress = percent.toInt()
            if (progress <= 100 && progress % 5 == 0) {
//              Log.d("COMPRESSION", "Compression progress: ${percent.toInt()}")
              EventEmitter().onTranscoding(channelId, workId, progress, "onProgress")
              mNotificationHelpers.startNotify(
                notificationId,
                mNotificationHelpers.getProgressNotificationBuilder(progress).setContentTitle("Đang nén video...").build()
              )
            }
          }
          override fun onStart() {
            // Compression start
            EventEmitter().onTranscoding(channelId, workId, 0, "onStart")
            Log.d("COMPRESSION", "Compression start")
          }
          override fun onSuccess() {
            // On Compression success
            if (isStopped) {
              callback.cancel()
            } else {
              EventEmitter().onTranscoding(channelId, workId, 100, "onSuccess")
              Log.d("COMPRESSION", "Compression success")
              callback.success(outputPath)
            }
          }
          override fun onFailure(failureMessage: String) {
            // On Failure
            if (isStopped) {
              callback.cancel()
            } else if (failureMessage == Compressor.INVALID_BITRATE) {
              EventEmitter().onTranscoding(channelId, workId, 100, "onSuccess")
              Log.wtf("COMPRESSION", failureMessage)
              callback.success(inputPath)
            } else {
              EventEmitter().onTranscoding(channelId, workId, 0, "onFailure")
              Log.wtf("COMPRESSION", failureMessage)
              callback.failure()
            }
          }
          override fun onCancelled() {
            // On Cancelled
            Log.d("COMPRESSION", "Compression cancelled")
            EventEmitter().onTranscoding(channelId, workId, 0, "onCancelled")
            callback.failure()
          }
        }, VideoQuality.VERY_HIGH, isMinBitRateEnabled = true, keepOriginalResolution = false)
    }
  }
}
