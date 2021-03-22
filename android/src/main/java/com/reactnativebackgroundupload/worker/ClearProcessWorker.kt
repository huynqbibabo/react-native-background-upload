package com.reactnativebackgroundupload.worker

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.common.util.concurrent.ListenableFuture
import com.reactnativebackgroundupload.EventEmitter
import com.reactnativebackgroundupload.NotificationHelpers
import com.reactnativebackgroundupload.model.ModelClearTask
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal interface TaskCallback {
  fun success(response: String)
  fun failure(response: String)
  fun cancel()
}

class ClearProcessWorker(
  context: Context,
  params: WorkerParameters
) : ListenableWorker(context, params) {
  private val mNotificationHelpers = NotificationHelpers(applicationContext)
  private val workId = inputData.getInt(ModelClearTask.KEY_WORK_ID, 1)

  override fun startWork(): ListenableFuture<Result> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
      EventEmitter().onStateChange(workId, EventEmitter.STATE.CHAIN_TASK)
      val callback: TaskCallback = object : TaskCallback {
        override fun success(response: String) {
          EventEmitter().onStateChange(workId, EventEmitter.STATE.SUCCESS)
          EventEmitter().onSuccess(workId, response)
          mNotificationHelpers.startNotify(
            workId,
            mNotificationHelpers.getCompleteNotificationBuilder().build()
          )
          completer.set(Result.success())
        }
        override fun failure(response: String) {
          EventEmitter().onStateChange(workId, EventEmitter.STATE.FAILED)
          mNotificationHelpers.startNotify(
            workId,
            mNotificationHelpers.getFailureNotificationBuilder().build()
          )
          completer.set(Result.failure())
        }
        override fun cancel() {
          EventEmitter().onStateChange(workId, EventEmitter.STATE.CANCELLED)
          mNotificationHelpers.startNotify(
            workId,
            mNotificationHelpers.getCancelNotificationBuilder().build()
          )
          completer.set(Result.failure())
        }
      }

      val url = inputData.getString(ModelClearTask.KEY_CHAIN_URL)
      val method = inputData.getString(ModelClearTask.KEY_METHOD)

      if (url != null && method != null) {
        val chainData = inputData.getString(ModelClearTask.KEY_DATA)
        val authorization = inputData.getString(ModelClearTask.KEY_AUTHORIZATION)
        val fileName = inputData.getString(ModelClearTask.KEY_FILE_NAME)!!

        AndroidNetworking.post(url).apply {
          if (authorization != null) {
            addHeaders("Authorization", authorization)
          }
          if (chainData != null) {
            val videoObject = JSONObject().put("name", fileName)
            val videoArray = JSONArray().put(videoObject)
            val chainDataObject = JSONObject(chainData).put("videos", videoArray)
            addJSONObjectBody(chainDataObject)
          }
        }.build().getAsJSONObject(object : JSONObjectRequestListener {
          override fun onResponse(response: JSONObject?) {
            if (isStopped) {
              callback.cancel()
            } else {
              Log.d("CHAIN_TASK", "$response")
              try {
                val status = response?.get("status")
                if (status == 1) {
                  callback.success(response.toString())
                } else {
                  callback.failure("errorDetail: response status = 0")
                }
              } catch (e: JSONException) {
                Log.e("CHAIN_TASK", "JsonException", e)
                callback.failure("errorDetail: JSON conversion exception")
              }
            }
          }
          override fun onError(anError: ANError) {
            Log.wtf("CHAIN_TASK", "$anError")
            if (anError.errorCode != 0) {
              Log.d("CHAIN_TASK", "onError errorCode : " + anError.errorCode)
              Log.d("CHAIN_TASK", "onError errorBody : " + anError.errorBody)
              Log.d("CHAIN_TASK", "onError errorDetail : " + anError.errorDetail)
            } else {
              Log.d("CHAIN_TASK", "onError errorDetail : " + anError.errorDetail)
            }
            callback.failure("errorDetail : " + anError.errorDetail)
          }
        })
      } else {
        Log.d("CHAIN_TASK", "complete with no chain task")
        callback.success("complete with no chain task")
      }
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
}
