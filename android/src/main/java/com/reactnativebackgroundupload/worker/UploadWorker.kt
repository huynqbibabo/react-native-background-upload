package com.reactnativebackgroundupload.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.ParsedRequestListener
import com.androidnetworking.interfaces.UploadProgressListener
import com.google.common.util.concurrent.ListenableFuture
import com.reactnativebackgroundupload.NotificationHelpers
import com.reactnativebackgroundupload.model.ModelClearNotification
import com.reactnativebackgroundupload.model.ModelUploadInput
import com.reactnativebackgroundupload.model.ModelUploadResponse
import java.io.File
import kotlin.math.roundToInt

internal interface UploadCallback {
  fun success()
  fun failure()
  fun retry()
}

class UploadWorker(
  context: Context,
  params: WorkerParameters
) : ListenableWorker(context, params) {
  private val mNotificationHelpers = NotificationHelpers(applicationContext)

  override fun startWork(): ListenableFuture<Result> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
      val notificationId = inputData.getInt(ModelUploadInput.KEY_NOTIFICATION_ID, 1)

      val callback: UploadCallback = object : UploadCallback {
        override fun success() {
          completer.set(Result.success())
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

      val requestUrl = inputData.getString(ModelUploadInput.KEY_REQUEST_URL)!!
      val fileName = inputData.getString(ModelUploadInput.KEY_FILE_NAME)!!
      val filePath = inputData.getString(ModelUploadInput.KEY_FILE_PATH)!!
      val hash = inputData.getString(ModelUploadInput.KEY_HASH)!!
      val prt = inputData.getInt(ModelUploadInput.KEY_PRT, 0)
      val numberOfChunks = inputData.getInt(ModelUploadInput.KEY_NUMBER_OF_CHUNKS, 1)

      uploadVideo(requestUrl, fileName, filePath, hash, prt, numberOfChunks, notificationId, callback)
      callback
    }
  }

  @SuppressLint("CheckResult")
  private fun uploadVideo(requestUrl: String, fileName: String, filePath: String, hash: String, prt: Int, numberOfChunks: Int, notificationId: Int, callback: UploadCallback) {
    val file = File(filePath)

    val requestBuilder = AndroidNetworking.upload(requestUrl).apply {
      addMultipartFile("data", file)
      addMultipartParameter("filename", fileName)
      addMultipartParameter("hash", hash)
      addMultipartParameter("prt", prt.toString())
    }.build()
    requestBuilder.uploadProgressListener = UploadProgressListener { bytesUploaded, totalBytes ->
      val progress = (bytesUploaded * 100 / totalBytes).toDouble()
//      Log.d("UPLOAD", "progress: $progress")
      mNotificationHelpers.startNotify(
        notificationId,
        mNotificationHelpers.getProgressNotificationBuilder(((progress + (prt - 1) * 100) / numberOfChunks).roundToInt()).build()
      )
    }
    requestBuilder.getAsObject(ModelUploadResponse::class.java, object : ParsedRequestListener<ModelUploadResponse> {
      override fun onResponse(response: ModelUploadResponse) {
        val metadata = response.data
        if (metadata != null && response.status == "1") {
          callback.success()
        } else {
          Log.wtf("METADATA:", "no metadata")
          callback.failure()
        }
      }
      override fun onError(error: ANError) {
        // handle error
        Log.wtf("UPLOAD:", "$error")
        callback.failure()
      }
    })
  }
}
