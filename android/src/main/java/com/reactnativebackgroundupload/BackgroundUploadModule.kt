package com.reactnativebackgroundupload

import android.net.Uri
import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.google.gson.Gson
import com.reactnativebackgroundupload.model.ModelUploadResponse
import com.reactnativebackgroundupload.service.RetrofitClient
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*


class BackgroundUploadModule(private val reactContext: ReactApplicationContext, private val icon: Int) : ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String {
        return "BackgroundUpload"
    }

    @ReactMethod
    fun startBackgroundUpload(requestUrl: String, filePath: String, fileName: String, hash: ReadableMap) {

      val realPath = RealPathUtil.getRealPath(reactContext, Uri.parse(filePath))
//      val videoBytes = FileInputStream(File(realPath)).use { input -> input.readBytes() }
      val file = File(realPath);

      val chunks: List<File> = splitFile(file, 3);

      val apiService = RetrofitClient().getApiService();
      val fileNameBody = RequestBody.create(MultipartBody.FORM, fileName);

      if (apiService !== null) {
        chunks.forEachIndexed { index, element ->
          val prt = (index + 1).toString();
          val hashValue = hash.getString(prt);
          val hashBody = RequestBody.create(MultipartBody.FORM, hashValue);
          val prtBody = RequestBody.create(MultipartBody.FORM, prt);
          val fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), element);

          val filePart = MultipartBody.Part.createFormData("data", fileName, fileBody)

          val call: Call<ModelUploadResponse> = apiService.uploadFile(requestUrl, fileNameBody, prtBody, hashBody, filePart);
          call.enqueue(object : Callback<ModelUploadResponse> {
            override fun onResponse(call: Call<ModelUploadResponse>, response: Response<ModelUploadResponse>) {
              Log.d("call api success", Gson().toJson(response.body()))
              element.delete()
            }
            override fun onFailure(call: Call<ModelUploadResponse>, t: Throwable) {
              Log.d("call api fail", Gson().toJson(t))
            }
          })
        }
      }
    }

  @Throws(IOException::class)
  fun splitFile(f: File, chunkSize: Int): List<File> {
    var partCounter = 1
    val result: MutableList<File> = ArrayList()
    val sizeOfFiles = 1024 * 1024 * chunkSize
    val buffer = ByteArray(sizeOfFiles) // create a buffer of bytes sized as the one chunk size
    val fis = FileInputStream(f)
    val bis = BufferedInputStream(fis)
    val name = f.name
    var tmp = 0
    while (bis.read(buffer).also { tmp = it } > 0) {
      val newFile = File(reactContext.externalCacheDir,  name + "." +  String.format("%03d", partCounter++)) // naming files as <inputFileName>.001, <inputFileName>.002, ...
      val out = FileOutputStream(newFile)
      out.write(buffer, 0, tmp) //tmp is chunk size. Need it for the last chunk, which could be less then 1 mb.
//      out.close()
      result.add(newFile)
    }
//    bis.close()
//    fis.close()
    return result
  }
}
