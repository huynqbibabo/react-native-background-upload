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
  fun failure(failureMessage: String, currentProgress: Int)
  fun cancel(currentProgress: Int)
//  fun retry()
}

class UploadWorker(
  context: Context,
  params: WorkerParameters
) : ListenableWorker(context, params) {
  private val mNotificationHelpers = NotificationHelpers(applicationContext)
  private val workId = inputData.getInt(ModelUploadInput.KEY_WORK_ID, 1)

  override fun startWork(): ListenableFuture<Result> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
      val callback = object : UploadCallback {
        override fun success() {
          completer.set(Result.success())
        }
        override fun failure(failureMessage: String, currentProgress: Int) {
          EventEmitter().onStateChange(workId, EventEmitter.STATE.FAILED, failureMessage, currentProgress)
          mNotificationHelpers.startNotify(
            workId,
            mNotificationHelpers.getFailureNotificationBuilder().build()
          )
          completer.set(Result.failure())
        }
        override fun cancel(currentProgress: Int) {
          EventEmitter().onStateChange(workId, EventEmitter.STATE.CANCELLED, "cancelled at upload state", currentProgress)
          mNotificationHelpers.startNotify(
            workId,
            mNotificationHelpers.getCancelNotificationBuilder().build()
          )
          completer.set(Result.failure())
        }
//        override fun retry() {
//          if (runAttemptCount > 1) {
//            completer.set(Result.failure())
//          } else {
//            completer.set(Result.retry())
//          }
//        }
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
    AndroidNetworking.cancel(workId)
    Log.d("UPLOAD", "stop")
  }

  @SuppressLint("CheckResult")
  private fun uploadVideo(requestUrl: String, fileName: String, filePath: String, hash: String, prt: Int, numberOfChunks: Int, callback: UploadCallback) {
    val file = File(filePath)
    val initialProgress = (prt - 1) * 100 / numberOfChunks
    var currentProgress = initialProgress

    // call upload api
    AndroidNetworking.upload(requestUrl).apply {
      setTag(workId)
      addMultipartFile("data", file)
      addMultipartParameter("filename", fileName)
      addMultipartParameter("hash", hash)
      addMultipartParameter("prt", prt.toString())
    }.build().apply {
      // upload progress
      uploadProgressListener = UploadProgressListener { bytesUploaded, totalBytes ->
        val percentage = (bytesUploaded * 100 / totalBytes).toDouble()
        val progress = (percentage / numberOfChunks + currentProgress).roundToInt()
//      Log.d("UPLOAD", "progress: $progress")
        if (progress <= 100 && progress % 5 == 0 && progress != currentProgress) {
          currentProgress = progress
          EventEmitter().onStateChange(workId, EventEmitter.STATE.UPLOAD, "progress uploading $prt/$numberOfChunks", progress)
          mNotificationHelpers.startNotify(workId, mNotificationHelpers.getProgressNotificationBuilder(progress).build())
        }
      }
      // upload response or error
      getAsJSONObject(object : JSONObjectRequestListener {
        override fun onResponse(response: JSONObject?) {
          if (isStopped) {
            callback.cancel(currentProgress)
          } else {
            Log.d("UPLOAD", "$response")
            try {
              val metadata = response?.get("data")
              val status = response?.get("status")
              if (metadata != null && status == 1) {
                callback.success()
              } else {
                callback.failure("upload $prt/$numberOfChunks success with response status = 0", currentProgress)
              }
            } catch (e: JSONException) {
              Log.e("UPLOAD", "JsonException", e)
              callback.failure("upload $prt/$numberOfChunks error: JSON conversion exception", currentProgress)
            }
          }
        }
        override fun onError(anError: ANError) {
          if (!isStopped) {
            if (anError.errorCode != 0) {
              Log.e("UPLOAD", "onError errorCode : " + anError.errorCode)
              Log.e("UPLOAD", "onError errorBody : " + anError.errorBody)
              Log.e("UPLOAD", "onError errorDetail : " + anError.errorDetail)
            } else {
              Log.e("UPLOAD", "onError errorDetail : " + anError.errorDetail)
            }
            callback.failure("upload $prt/$numberOfChunks error: " + anError.errorDetail, currentProgress)
          } else {
            callback.cancel(currentProgress)
          }
        }
      })
    }
  }
}
