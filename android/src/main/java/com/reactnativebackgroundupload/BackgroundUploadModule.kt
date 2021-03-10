package com.reactnativebackgroundupload

import android.annotation.SuppressLint
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.reactnativebackgroundupload.model.ModelTranscodeInput
import com.reactnativebackgroundupload.worker.CompressWorker
import com.reactnativebackgroundupload.worker.RequestMetadataWorker
import com.reactnativebackgroundupload.worker.SplitWorker

class BackgroundUploadModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  companion object {
    private const val BACKGROUND_TRANSCODE_WORK_NAME = "background_transcode"
  }

  override fun getName(): String {
    return "BackgroundUpload"
  }

  @SuppressLint("EnqueueWork")
  @ReactMethod
  fun startBackgroundUploadVideo(uploadUrl: String, metadataUrl: String, filePath: String, chunkSize: Int) {
    NotificationHelpers(reactContext).createNotificationChannel()
    val notificationId = System.currentTimeMillis().toInt()

    val workManager = WorkManager.getInstance(reactContext)
    val compressRequest = OneTimeWorkRequestBuilder<CompressWorker>()
      .setInputData(ModelTranscodeInput().createInputDataForCompress(filePath, chunkSize, uploadUrl, metadataUrl, notificationId))
      .addTag(BACKGROUND_TRANSCODE_WORK_NAME)
      .build()
    var workContinuation = workManager.beginWith(compressRequest)
    workContinuation = workContinuation.then(
      OneTimeWorkRequest.from(SplitWorker::class.java)
    )
    workContinuation = workContinuation.then(
      OneTimeWorkRequest.from(RequestMetadataWorker::class.java)
    )
    workContinuation.enqueue()
  }
}
