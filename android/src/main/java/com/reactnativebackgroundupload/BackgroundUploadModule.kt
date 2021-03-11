package com.reactnativebackgroundupload

import android.annotation.SuppressLint
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.facebook.react.bridge.*
import com.reactnativebackgroundupload.model.ModelRequestMetadata
import com.reactnativebackgroundupload.model.ModelTranscodeInput
import com.reactnativebackgroundupload.worker.CompressWorker
import com.reactnativebackgroundupload.worker.RequestMetadataWorker
import com.reactnativebackgroundupload.worker.SplitWorker

class BackgroundUploadModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String {
    return "BackgroundUpload"
  }

  @SuppressLint("EnqueueWork")
  @ReactMethod
  fun startBackgroundUploadVideo(uploadUrl: String, metadataUrl: String, filePath: String, chunkSize: Int, chainTask: ReadableMap?) {
    NotificationHelpers(reactContext).createNotificationChannel()
    val notificationId = System.currentTimeMillis().toInt()

    val workManager = WorkManager.getInstance(reactContext)
    val compressRequest = OneTimeWorkRequestBuilder<CompressWorker>()
      .setInputData(ModelTranscodeInput().createInputDataForCompress(filePath, chunkSize, uploadUrl, metadataUrl, notificationId))
      .build()
    var workContinuation = workManager.beginWith(compressRequest)
    workContinuation = workContinuation.then(
      OneTimeWorkRequest.from(SplitWorker::class.java)
    )
    //
    val metadataRequest = OneTimeWorkRequestBuilder<RequestMetadataWorker>().apply {
      if (chainTask != null) {
        val url = chainTask.getString("url")
        val method = chainTask.getString("method")
        val authorization = chainTask.getString("authorization")
        val data = chainTask.getString("data")
        setInputData(ModelRequestMetadata().createInputDataForRequestTask(url, method, authorization, data))
      }
    }.build()
    workContinuation = workContinuation.then(metadataRequest)
    workContinuation.enqueue()
  }
}
