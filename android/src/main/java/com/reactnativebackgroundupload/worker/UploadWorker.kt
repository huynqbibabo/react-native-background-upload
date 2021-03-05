package com.reactnativebackgroundupload.worker

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.reactnativebackgroundupload.model.ModelUploadInput
import com.reactnativebackgroundupload.model.ModelUploadResponse
import com.reactnativebackgroundupload.service.RetrofitClient
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

internal interface UploadCallback {
  fun onSuccess()
  fun onError()
}

class UploadWorker(
  context: Context,
  params: WorkerParameters
) : ListenableWorker(context, params) {
  override fun startWork(): ListenableFuture<Result> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
      val callback: UploadCallback = object : UploadCallback {
        override fun onSuccess() {
          completer.set(Result.success())
        }

        override fun onError() {
          completer.set(Result.retry())
        }
      }

      val requestUrl = inputData.getString(ModelUploadInput.KEY_REQUEST_URL)!!
      val fileName = inputData.getString(ModelUploadInput.KEY_FILE_NAME)!!
      val filePath = inputData.getString(ModelUploadInput.KEY_FILE_PATH)!!
      val hash = inputData.getString(ModelUploadInput.KEY_HASH)!!
      val prt = inputData.getString(ModelUploadInput.KEY_PRT)!!

      uploadVideo(requestUrl, fileName, filePath, hash, prt, callback)
      callback
    }
  }

  private fun uploadVideo(requestUrl: String, fileName: String, filePath: String, hash: String, prt: String, callback: UploadCallback) {
    val apiService = RetrofitClient().getApiService();
    if (apiService != null) {
      val file = File(filePath)

      val prtBody = RequestBody.create(MultipartBody.FORM, prt);
      val hashBody = RequestBody.create(MultipartBody.FORM, hash);
      val fileNameBody = RequestBody.create(MultipartBody.FORM, fileName);

      val fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
      val filePart = MultipartBody.Part.createFormData("data", fileName, fileBody)

      val call: Call<ModelUploadResponse> = apiService.uploadFile(requestUrl, fileNameBody, prtBody, hashBody, filePart);
      call.enqueue(object : Callback<ModelUploadResponse> {
        override fun onResponse(call: Call<ModelUploadResponse>, response: Response<ModelUploadResponse>) {
          Log.d("call api success", Gson().toJson(response.body()))
          callback.onSuccess()
        }
        override fun onFailure(call: Call<ModelUploadResponse>, t: Throwable) {
          Log.d("call api fail", Gson().toJson(t))
          callback.onError()
        }
      })
    }
  }
}
