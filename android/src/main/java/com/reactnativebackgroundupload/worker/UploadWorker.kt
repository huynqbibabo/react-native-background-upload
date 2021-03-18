package com.reactnativebackgroundupload.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.androidnetworking.interfaces.UploadProgressListener
import com.google.common.util.concurrent.ListenableFuture
import com.reactnativebackgroundupload.EventEmitter
import com.reactnativebackgroundupload.NotificationHelpers
import com.reactnativebackgroundupload.model.ModelUploadInput
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

internal interface UploadCallback {
  fun success()
  fun failure()
  fun retry()
  fun cancel()
}

class UploadWorker(
  context: Context,
  params: WorkerParameters
) : ListenableWorker(context, params) {
  private val mNotificationHelpers = NotificationHelpers(applicationContext)
  private val workId = inputData.getInt(ModelUploadInput.KEY_WORK_ID, 1)
  private val channelId = inputData.getDouble(ModelUploadInput.KEY_EVENT_EMITTER_CHANNEL_ID, 1.0)

  override fun startWork(): ListenableFuture<Result> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
      EventEmitter().onStateChange(channelId, workId, EventEmitter.STATE.UPLOAD)
      val callback: UploadCallback = object : UploadCallback {
        override fun success() {
          completer.set(Result.success())
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
          completer.set(Result.failure())
        }
        override fun retry() {
          if (runAttemptCount > 1) {
            completer.set(Result.failure())
          } else {
            EventEmitter().onStateChange(channelId, workId, EventEmitter.STATE.RETRY)
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

      uploadVideo(requestUrl, fileName, filePath, hash, prt, numberOfChunks, callback)
      callback
    }
  }

  override fun onStopped() {
//    mNotificationHelpers.startNotify(
//      notificationId,
//      mNotificationHelpers.getFailureNotificationBuilder().build()
//    )
    Log.d("METADATA", "stop")
  }

  @SuppressLint("CheckResult")
  private fun uploadVideo(requestUrl: String, fileName: String, filePath: String, hash: String, prt: Int, numberOfChunks: Int, callback: UploadCallback) {
    EventEmitter().onUpload(channelId, workId, "onStart", 0, "start upload $prt/$numberOfChunks")
    val file = File(filePath)
    val currentProgress = (prt - 1) * 100 / numberOfChunks

    val requestBuilder = AndroidNetworking.upload(requestUrl).apply {
      addMultipartFile("data", file)
      addMultipartParameter("filename", fileName)
      addMultipartParameter("hash", hash)
      addMultipartParameter("prt", prt.toString())
    }.build()
    requestBuilder.uploadProgressListener = UploadProgressListener { bytesUploaded, totalBytes ->
      val percentage = (bytesUploaded * 100 / totalBytes).toDouble()
      val progress = (percentage / numberOfChunks + currentProgress).roundToInt()
//      Log.d("UPLOAD", "progress: $progress")
      if (isStopped) {
//        requestBuilder.cancel()
        callback.cancel()
      } else if (progress <= 100 && progress % 5 == 0) {
        EventEmitter().onUpload(channelId, workId, "onProgress", progress, "uploading $prt/$numberOfChunks")
        mNotificationHelpers.startNotify(workId, mNotificationHelpers.getProgressNotificationBuilder(progress).build())
      }
    }
    requestBuilder.getAsJSONObject(object : JSONObjectRequestListener {
      override fun onResponse(response: JSONObject?) {
        EventEmitter().onUpload(channelId, workId, "onResponse", 100, response.toString())
        if (isStopped) {
          callback.cancel()
        } else {
          Log.d("UPLOAD", "$response")
          try {
            val metadata = response?.get("data")
            val status = response?.get("status")
            if (metadata != null && status == 1) {
              EventEmitter().onUpload(channelId, workId, "onSuccess", 100, "successfully upload $prt/$numberOfChunks")
              callback.success()
            } else {
              EventEmitter().onUpload(channelId, workId, "onError", 100, "errorDetail: response status = 0")
              callback.failure()
            }
          } catch (e: JSONException) {
            Log.e("UPLOAD", "JsonException", e)
            EventEmitter().onUpload(channelId, workId, "onError", 100, "errorDetail: JSON conversion exception")
            callback.failure()
          }
        }
      }
      override fun onError(anError: ANError) {
        if (anError.errorCode != 0) {
          Log.e("UPLOAD", "onError errorCode : " + anError.errorCode)
          Log.e("UPLOAD", "onError errorBody : " + anError.errorBody)
          Log.e("UPLOAD", "onError errorDetail : " + anError.errorDetail)
        } else {
          Log.e("UPLOAD", "onError errorDetail : " + anError.errorDetail)
        }
        EventEmitter().onUpload(channelId, workId, "onError", 0, "errorDetail: " + anError.errorDetail)
        callback.failure()
      }
    })
  }
}
