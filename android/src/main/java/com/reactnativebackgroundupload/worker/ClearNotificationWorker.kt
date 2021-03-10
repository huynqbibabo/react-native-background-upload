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
import com.reactnativebackgroundupload.NotificationHelpers
import com.reactnativebackgroundupload.model.ModelClearNotification
import org.json.JSONArray
import org.json.JSONObject

class ClearNotificationWorker(
  context: Context,
  params: WorkerParameters
) : ListenableWorker(context, params) {
  private val mNotificationHelpers = NotificationHelpers(applicationContext)

  override fun startWork(): ListenableFuture<Result> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
      val url = inputData.getString(ModelClearNotification.KEY_CHAIN_URL)
      val method = inputData.getString(ModelClearNotification.KEY_METHOD)
      val notificationId = inputData.getInt(ModelClearNotification.KEY_NOTIFICATION_ID, 1)

      if (url != null && method != null) {
        val chainData = inputData.getString(ModelClearNotification.KEY_DATA)
        val authorization = inputData.getString(ModelClearNotification.KEY_AUTHORIZATION)
        val fileName = inputData.getString(ModelClearNotification.KEY_FILE_NAME)!!

        AndroidNetworking.post(url).apply {
          if (authorization != null) {
            addHeaders("Authorization", authorization)
          }
          if (chainData != null) {
            val videoObject = JSONObject()
            videoObject.put("name", fileName)
            val videoArray = JSONArray()
            videoArray.put(videoObject)
            val chainDataObject = JSONObject(chainData)
            chainDataObject.put("videos", videoArray)
            addJSONObjectBody(chainDataObject)
          }
        }.build().getAsJSONObject(object : JSONObjectRequestListener {
          override fun onResponse(response: JSONObject?) {
            Log.d("CHAIN", "$response")
            mNotificationHelpers.startNotify(
              notificationId,
              mNotificationHelpers.getCompleteNotificationBuilder().build()
            )
            completer.set(Result.success())
          }
          override fun onError(anError: ANError) {
            Log.wtf("CHAIN", "$anError")
            completer.set(Result.failure())
          }
        })
      } else {
        completer.set(Result.failure())
      }

      // clear upload notification.
      mNotificationHelpers.startNotify(
        notificationId,
        mNotificationHelpers.getCompleteNotificationBuilder().build()
      )
      Log.i("PROGRESS", "complete")
      completer.set(Result.success())
    }
  }
}
