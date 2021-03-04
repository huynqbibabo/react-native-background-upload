package com.reactnativebackgroundupload.service

import com.reactnativebackgroundupload.model.ModelUploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*


interface ApiService {
  @Streaming
  @Multipart
  @POST()
  fun uploadFile(
    @Url apiName: String,
    @Part("filename") filename: RequestBody,
    @Part("prt") prt: RequestBody,
    @Part("hash") hash: RequestBody,
    @Part file: MultipartBody.Part
  ): Call<ModelUploadResponse>
}
