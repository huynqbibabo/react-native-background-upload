package com.reactnativebackgroundupload

import android.annotation.SuppressLint
import android.net.Uri
import androidx.work.*
import com.facebook.react.bridge.*
import com.reactnativebackgroundupload.model.ModelRequestMetadata
import com.reactnativebackgroundupload.model.ModelTranscodeInput
import com.reactnativebackgroundupload.util.RealPathUtil
import com.reactnativebackgroundupload.worker.CompressWorker
import com.reactnativebackgroundupload.worker.RequestMetadataWorker
import com.reactnativebackgroundupload.worker.SplitWorker

class BackgroundUploadModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String {
    return "BackgroundUpload"
  }

  @SuppressLint("EnqueueWork")
  @ReactMethod
  fun startBackgroundUploadVideo(uploadUrl: String, metadataUrl: String, filePath: String, chunkSize: Int, enableCompression: Boolean, chainTask: ReadableMap?) {
    NotificationHelpers(reactContext).createNotificationChannel()
    val notificationId = System.currentTimeMillis().toInt()  // get unique notification id

    // get file:// path
    val realFilePath = RealPathUtil.getRealPath(reactContext, Uri.parse(filePath))

    val workManager = WorkManager.getInstance(reactContext)
    var workContinuation: WorkContinuation?
    if (enableCompression) {
      // setup worker for video compression
      val compressRequest = OneTimeWorkRequestBuilder<CompressWorker>()
        .setInputData(ModelTranscodeInput().createInputDataForTranscode(realFilePath, chunkSize, notificationId))
        .build()
      workContinuation = workManager.beginWith(compressRequest)
      // add worker for split file to queue
      workContinuation = workContinuation.then(
        OneTimeWorkRequest.from(SplitWorker::class.java)
      )
    } else {
      // setup worker for metadata request
      val splitRequest = OneTimeWorkRequestBuilder<SplitWorker>()
        .setInputData(ModelTranscodeInput().createInputDataForTranscode(realFilePath, chunkSize, notificationId))
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
        setInputData(ModelRequestMetadata().createInputDataForRequestTask(notificationId, uploadUrl, metadataUrl, taskUrl, method, authorization, data))
      } else {
        setInputData(ModelRequestMetadata().createInputDataForUpload(notificationId, uploadUrl, metadataUrl))
      }
      setConstraints(metadataConstraints)
    }.build()
    workContinuation = workContinuation.then(metadataRequest)
    workContinuation.enqueue()
  }
}
