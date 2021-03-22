package com.reactnativebackgroundupload

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.facebook.react.bridge.*
import com.reactnativebackgroundupload.model.ModelRequestMetadata
import com.reactnativebackgroundupload.model.ModelTranscodeInput
import com.reactnativebackgroundupload.util.RealPathUtil
import com.reactnativebackgroundupload.worker.CompressWorker
import com.reactnativebackgroundupload.worker.RequestMetadataWorker
import com.reactnativebackgroundupload.worker.SplitWorker
import kotlin.math.roundToInt

class BackgroundUploadModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String {
    return "BackgroundUpload"
  }

  @ReactMethod
  fun stopBackgroundUpload(workId: String, promise: Promise) {
    try {
      Log.d("BACKGROUND_UPLOAD", "stop: $workId")
      WorkManager.getInstance(reactContext).cancelAllWorkByTag(workId)
      promise.resolve(workId)
    } catch(e: Exception) {
      promise.reject(e)
    }
  }

  @ReactMethod
  fun getCurrentState(workId: Int, promise: Promise) {
    try {
      val state = EventEmitter().getCurrentState(workId)
      promise.resolve(state)
    } catch(e: Exception) {
      promise.reject(e)
    }
  }

  @SuppressLint("EnqueueWork")
  @ReactMethod
  fun startBackgroundUploadVideo(workId: Int, uploadUrl: String, metadataUrl: String, filePath: String, chunkSize: Int, enableCompression: Boolean, chainTask: ReadableMap?, promise: Promise) {
    try {
      Log.d("BACKGROUND_UPLOAD", "start: $workId")
      // set up event emitter
      EventEmitter().setReactContext(reactContext)
      // set up notification and work tag
      NotificationHelpers(reactContext).createNotificationChannel()

      val workTag = workId.toString() // get unique work tag
      EventEmitter().onStateChange(workId, EventEmitter.STATE.IDLE)

      // get file:// path
      val realFilePath = RealPathUtil.getRealPath(reactContext, Uri.parse(filePath))

      val workManager = WorkManager.getInstance(reactContext)
      var workContinuation: WorkContinuation?
      if (enableCompression) {
        // setup worker for video compression
        val compressRequest = OneTimeWorkRequestBuilder<CompressWorker>()
          .setInputData(ModelTranscodeInput().createInputDataForTranscode(workId, realFilePath, chunkSize))
          .addTag(workTag)
          .build()
        workContinuation = workManager.beginWith(compressRequest)
        // add worker for split file to queue
        val splitRequest = OneTimeWorkRequestBuilder<SplitWorker>().addTag(workTag).build()
        workContinuation = workContinuation.then(splitRequest)
      } else {
        // setup worker for split request
        val splitRequest = OneTimeWorkRequestBuilder<SplitWorker>()
          .setInputData(ModelTranscodeInput().createInputDataForTranscode(workId, realFilePath, chunkSize))
          .addTag(workTag)
          .build()
        workContinuation = workManager.beginWith(splitRequest)
      }
      // setup worker for metadata request
      val metadataConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED) // constraints worker with network condition
        .build()
      val metadataRequest = OneTimeWorkRequestBuilder<RequestMetadataWorker>().apply {
        if (chainTask != null) {
          val taskUrl = chainTask.getString("url")
          val method = chainTask.getString("method")
          val authorization = chainTask.getString("authorization")
          val data = chainTask.getString("data")
          setInputData(ModelRequestMetadata().createInputDataForRequestTask(workId, uploadUrl, metadataUrl, taskUrl, method, authorization, data))
        } else {
          setInputData(ModelRequestMetadata().createInputDataForUpload(workId, uploadUrl, metadataUrl))
        }
        setConstraints(metadataConstraints)
        addTag(workTag)
      }.build()
      workContinuation = workContinuation.then(metadataRequest)
      workContinuation.enqueue()
      promise.resolve(workId)
    } catch (e: Exception) {
      promise.reject(e)
    }
  }
}
