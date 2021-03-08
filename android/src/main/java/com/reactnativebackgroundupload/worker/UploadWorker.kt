package com.reactnativebackgroundupload.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.reactnativebackgroundupload.NotificationHelpers
import com.reactnativebackgroundupload.model.ModelUploadInput
import com.reactnativebackgroundupload.model.ModelUploadResponse
import com.reactnativebackgroundupload.service.ProgressRequestBody
import com.reactnativebackgroundupload.service.RetrofitClient
import io.reactivex.schedulers.Schedulers
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.net.HttpURLConnection
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
      val callback: UploadCallback = object : UploadCallback {
        override fun success() {
          completer.set(Result.success())
        }

        override fun failure() {
          mNotificationHelpers.startNotify(
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

      uploadVideo(requestUrl, fileName, filePath, hash, prt, numberOfChunks, callback)
      callback
    }
  }

  @SuppressLint("CheckResult")
  private fun uploadVideo(requestUrl: String, fileName: String, filePath: String, hash: String, prt: Int, numberOfChunks: Int, callback: UploadCallback) {
    val apiService = RetrofitClient().getApiService()
    if (apiService != null) {
      val file = File(filePath)

      val prtBody = RequestBody.create(MultipartBody.FORM, prt.toString());
      val hashBody = RequestBody.create(MultipartBody.FORM, hash);
      val fileNameBody = RequestBody.create(MultipartBody.FORM, fileName);

      val fileBody = ProgressRequestBody(file);
      val filePart = MultipartBody.Part.createFormData("data", fileName, fileBody)

      fileBody.getProgressSubject()
        .subscribeOn(Schedulers.io())
        .subscribe { progress ->
//          Log.i("PROGRESS", "${(progress * prt / numberOfChunks).roundToInt()}%")
          mNotificationHelpers.startNotify(
            mNotificationHelpers.getProgressNotificationBuilder(((progress + (prt - 1) * 100) / numberOfChunks).roundToInt()).build()
          )
        }
      val call: Call<ModelUploadResponse> = apiService.uploadFile(requestUrl, fileNameBody, prtBody, hashBody, filePart);
      call.enqueue(object : Callback<ModelUploadResponse> {
        override fun onResponse(call: Call<ModelUploadResponse>, response: Response<ModelUploadResponse>) {
          Log.d("call api response", Gson().toJson(response.body()))
          Log.d("call api response", Gson().toJson(response.code()))
          if (response.code() == HttpURLConnection.HTTP_OK) {
            callback.success()
          } else {
            callback.retry()
          }
        }
        override fun onFailure(call: Call<ModelUploadResponse>, t: Throwable) {
          Log.d("call api fail", Gson().toJson(t))
          callback.failure()
        }
      })
    }
  }
}
