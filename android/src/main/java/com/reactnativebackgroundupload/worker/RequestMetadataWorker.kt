package com.reactnativebackgroundupload.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.*
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.androidnetworking.interfaces.ParsedRequestListener
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.reactnativebackgroundupload.NotificationHelpers
import com.reactnativebackgroundupload.model.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal interface RequestMetadataCallback {
  fun success()
  fun failure()
}

class RequestMetadataWorker(
  context: Context,
  params: WorkerParameters
) : ListenableWorker(context, params) {
  private val mNotificationHelpers = NotificationHelpers(applicationContext)

  override fun startWork(): ListenableFuture<Result> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
      val notificationId = inputData.getInt(ModelRequestMetadata.KEY_NOTIFICATION_ID, 1)
      val chunkPaths = inputData.getStringArray(ModelRequestMetadata.KEY_CHUNK_PATH_ARRAY)!!

      val callback: RequestMetadataCallback = object : RequestMetadataCallback {
        override fun success() {
          clearCache(chunkPaths)
          completer.set(Result.success())
        }
        override fun failure() {
          mNotificationHelpers.startNotify(
            notificationId,
            mNotificationHelpers.getFailureNotificationBuilder().build()
          )
          clearCache(chunkPaths)
          completer.set(Result.failure())
        }
//        override fun retry() {
//          if (runAttemptCount > 2) {
//            completer.set(Result.failure())
//          } else {
//            completer.set(Result.retry())
//          }
//        }
      }
      val uploadUrl = inputData.getString(ModelRequestMetadata.KEY_UPLOAD_URL)!!
      val metadataUrl = inputData.getString(ModelRequestMetadata.KEY_METADATA_URL)!!
      requestMetadata(metadataUrl, uploadUrl, chunkPaths, notificationId, callback)
      callback
    }
  }

  private fun clearCache(chunkPaths: Array<String>) {
    val workManager = WorkManager.getInstance(applicationContext)
    val clearCacheRequest = OneTimeWorkRequestBuilder<ClearCacheWorker>()
      .setInputData(ModelClearCache().createInputDataForClearCache(chunkPaths))
      .setInitialDelay(6, TimeUnit.HOURS)
      .build()
    workManager.enqueue(clearCacheRequest)
  }

  private fun requestMetadata(metadataUrl: String, uploadUrl: String, chunkPaths: Array<String>, notificationId: Int, callback: RequestMetadataCallback) {
    val chunkSize = chunkPaths.size
    AndroidNetworking.post(metadataUrl).apply {
      addBodyParameter("cto", "$chunkSize")
      addBodyParameter("ext", "mp4")
    }.build()
      .getAsJSONObject(object : JSONObjectRequestListener {
        override fun onResponse(response: JSONObject?) {
          Log.d("METADATA", "${Gson().fromJson(Gson().toJson(response), ModelMetadataResponse::class.java)}")
          callback.success()
        }
        override fun onError(anError: ANError) {
          Log.wtf("METADATA", "$anError")
          if (anError.errorCode != 0) {
            Log.d("METADATA", "onError errorCode : " + anError.errorCode)
            Log.d("METADATA", "onError errorBody : " + anError.errorBody)
            Log.d("METADATA", "onError errorDetail : " + anError.errorDetail)
          } else {
            Log.d("METADATA", "onError errorDetail : " + anError.errorDetail)
          }
          callback.failure()
        }
      })
//    .getAsObject(ModelMetadataResponse::class.java, object : ParsedRequestListener<ModelMetadataResponse> {
//      override fun onResponse(response: ModelMetadataResponse) {
//        val metadata = response.data
//        if (metadata != null && response.status == "1") {
//          Log.d("METADATA:", "fileName: ${metadata.filename}")
//          Log.d("METADATA:", "url: ${metadata.url}")
//          startUploadWorker(metadata, uploadUrl, chunkPaths, chunkSize, notificationId, callback)
//        } else {
//          Log.wtf("METADATA:", "no metadata")
//          callback.failure()
//        }
//      }
//      override fun onError(error: ANError) {
//        // handle error
//        Log.wtf("METADATA:", "$error")
//        callback.failure()
//      }
//    })
  }

  @SuppressLint("EnqueueWork")
  private fun startUploadWorker(data: VideoMetadata, uploadUrl: String, chunkPaths: Array<String>, chunkSize: Int, notificationId: Int, callback: RequestMetadataCallback) {
    val fileName = data.filename
    val hashMap = data.hashes
    if (hashMap != null && fileName != null) {
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
            ModelUploadInput().createInputDataForUpload(uploadUrl, fileName, chunkPaths[prt - 1], value, prt, chunkSize, notificationId)
          )
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
        .setInputData(
          ModelClearTask().createInputDataForClearTask(notificationId, fileName, url, method, authorization, chainData)
        ).build()
      workContinuation = workContinuation?.then(clearRequest)
      workContinuation?.enqueue()
      callback.success()
    } else {
      callback.failure()
    }
  }
}
