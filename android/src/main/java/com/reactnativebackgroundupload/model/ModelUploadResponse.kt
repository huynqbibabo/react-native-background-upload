package com.reactnativebackgroundupload.model

import com.google.gson.annotations.SerializedName;

class ModelUploadResponse {
  @SerializedName("status")
  var status: String? = null

  @SerializedName("message")
  var message: String? = null

  @SerializedName("data")
  var data: UploadFileData? = null
}

class UploadFileData {
  @SerializedName("allUploaded")
  var allUploaded: Number? = null

  @SerializedName("notUploaded")
  var notUploaded: List<Number>? = null
}
