package com.reactnativebackgroundupload.worker

import android.content.Context
import android.os.Build
import android.util.Log
import android.annotation.SuppressLint
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.*
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.reactnativebackgroundupload.EventEmitter
import com.reactnativebackgroundupload.NotificationHelpers
import com.reactnativebackgroundupload.model.*
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal interface RequestMetadataCallback {
  fun success()
  fun failure()
  fun cancel()
}

class RequestMetadataWorker(
  context: Context,
  params: WorkerParameters
) : ListenableWorker(context, params) {
  private val mNotificationHelpers = NotificationHelpers(applicationContext)
  private val workId = inputData.getInt(ModelRequestMetadata.KEY_WORK_ID, 1)

  override fun startWork(): ListenableFuture<Result> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
      EventEmitter().onStateChange(workId, EventEmitter.STATE.REQUEST_METADATA, "start", 0)
      val chunkPaths = inputData.getStringArray(ModelRequestMetadata.KEY_CHUNK_PATH_ARRAY)!!

      val callback: RequestMetadataCallback = object : RequestMetadataCallback {
        override fun success() {
          clearCache(chunkPaths)
          completer.set(Result.success())
        }
        override fun failure() {
          mNotificationHelpers.startNotify(
            workId,
            mNotificationHelpers.getFailureNotificationBuilder().build()
          )
          clearCache(chunkPaths)
          completer.set(Result.failure())
        }
        override fun cancel() {
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
      val uploadUrl = inputData.getString(ModelRequestMetadata.KEY_UPLOAD_URL)!!
      val metadataUrl = inputData.getString(ModelRequestMetadata.KEY_METADATA_URL)!!
      requestMetadata(metadataUrl, uploadUrl, chunkPaths, callback)
      callback
    }
  }

//  override fun onStopped() {
//    mNotificationHelpers.startNotify(
//      notificationId,
//      mNotificationHelpers.getFailureNotificationBuilder().build()
//    )
//    Log.d("METADATA", "stop")
//  }

  @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
  private fun clearCache(chunkPaths: Array<String>) {
    val workManager = WorkManager.getInstance(applicationContext)
    val clearCacheRequest = OneTimeWorkRequestBuilder<ClearCacheWorker>()
      .setInputData(ModelClearCache().createInputDataForClearCache(chunkPaths))
      .setInitialDelay(30, TimeUnit.MINUTES)
      .build()
    workManager.enqueue(clearCacheRequest)
  }

  private fun requestMetadata(metadataUrl: String, uploadUrl: String, chunkPaths: Array<String>, callback: RequestMetadataCallback) {
    val chunkSize = chunkPaths.size
    AndroidNetworking.post(metadataUrl).apply {
      addBodyParameter("cto", "$chunkSize")
      addBodyParameter("ext", "mp4")
    }.build().getAsJSONObject(object : JSONObjectRequestListener {
      override fun onResponse(response: JSONObject?) {
        if (isStopped) {
          EventEmitter().onStateChange(workId, EventEmitter.STATE.CANCELLED, "cancelled at request metadata state", 50)
          callback.cancel()
        } else {
          try {
            val metadata = response?.getJSONObject("data")
            val status = response?.get("status")
            if (metadata != null && status == 1) {
              val gson = Gson()
              val convertMetadata = gson.fromJson(metadata.toString(), VideoMetadata::class.java)
              Log.d("METADATA", "url: ${convertMetadata.url}")
              Log.d("METADATA", "filename: ${convertMetadata.filename}")
              Log.d("METADATA", "hash: ${convertMetadata.hashes}")
              EventEmitter().onStateChange(workId, EventEmitter.STATE.REQUEST_METADATA, "get metadata success: $response", 50)
              startUploadWorker(convertMetadata, uploadUrl, chunkPaths, chunkSize, callback)
              callback.success()
            } else {
              Log.d("METADATA", "$response")
              EventEmitter().onStateChange(workId, EventEmitter.STATE.FAILED, "get metadata success with response status = 0", 40)
              callback.failure()
            }
          } catch (e: JSONException) {
            Log.e("METADATA", "JsonException", e)
            EventEmitter().onStateChange(workId, EventEmitter.STATE.FAILED, "get metadata error: JSON conversion exception", 30)
            callback.failure()
          }
        }
      }
      override fun onError(anError: ANError) {
        if (anError.errorCode != 0) {
          Log.e("METADATA", "onError errorCode : " + anError.errorCode)
          Log.e("METADATA", "onError errorBody : " + anError.errorBody)
          Log.e("METADATA", "onError errorDetail : " + anError.errorDetail)
        } else {
          Log.e("METADATA", "onError errorDetail : " + anError.errorDetail)
        }
        EventEmitter().onStateChange(workId, EventEmitter.STATE.FAILED, "get metadata error: " + anError.errorDetail, 20)
        callback.failure()
      }
    })
  }

  @SuppressLint("EnqueueWork")
  private fun startUploadWorker(data: VideoMetadata, uploadUrl: String, chunkPaths: Array<String>, chunkSize: Int, callback: RequestMetadataCallback) {
    if (isStopped) {
      EventEmitter().onStateChange(workId, EventEmitter.STATE.CANCELLED, "cancelled at request metadata state", 60)
      callback.cancel()
    } else {
      try {
        val fileName = data.filename
        val hashMap = data.hashes
        if (hashMap != null && fileName != null) {
          // create work tag from unique id
          val workTag = workId.toString()
          // init work manager
          val workManager = WorkManager.getInstance(applicationContext)
          val workConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // constraints worker with network condition
            .build()
          var workContinuation: WorkContinuation? = null

          // add upload worker to queue
          for ((key, value) in hashMap) {
            val prt = key.toInt()
            val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>().apply {
              setConstraints(workConstraints)
              setInputData(
                ModelUploadInput().createInputDataForUpload(uploadUrl, fileName, chunkPaths[prt - 1], value, prt, chunkSize, workId)
              )
              addTag(workTag)
            }.build()
            workContinuation = workContinuation?.then(uploadRequest)
              ?: workManager.beginWith(uploadRequest)
          }
          // add clean up work
          val url = inputData.getString(ModelRequestMetadata.KEY_CHAIN_URL)
          val method = inputData.getString(ModelRequestMetadata.KEY_METHOD)
          val authorization = inputData.getString(ModelRequestMetadata.KEY_AUTHORIZATION)
          val chainData = inputData.getString(ModelRequestMetadata.KEY_DATA)
          val clearRequest = OneTimeWorkRequestBuilder<ClearProcessWorker>()
            .addTag(workTag)
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setInputData(
              ModelClearTask().createInputDataForClearTask(workId, fileName, url, method, authorization, chainData)
            ).build()
          workContinuation = workContinuation?.then(clearRequest)
          if (isStopped) {
            EventEmitter().onStateChange(workId, EventEmitter.STATE.CANCELLED, "create upload worker error: invalid file name and hashArray", 80)
            callback.cancel()
          } else {
            workContinuation?.enqueue()
            EventEmitter().onStateChange(workId, EventEmitter.STATE.REQUEST_METADATA, "create upload worker success", 100)
            callback.success()
          }
        } else {
          EventEmitter().onStateChange(workId, EventEmitter.STATE.FAILED, "create upload worker error: invalid file name and hashArray", 50)
          callback.failure()
        }
      } catch (e: Exception) {
        Log.e("METADATA", "startUploadWorker", e)
        EventEmitter().onStateChange(workId, EventEmitter.STATE.FAILED, "create upload worker error: $e", 50)
        callback.failure()
      }
    }
  }
}
