package com.reactnativebackgroundupload.model

import com.google.gson.annotations.SerializedName

class ModelMetadataResponse {
  @SerializedName("status")
  var status: String? = null

  @SerializedName("message")
  var message: String? = null

  @SerializedName("data")
  var data: VideoMetadata? = null
}

class VideoMetadata {
  @SerializedName("filename")
  var filename: String? = null

  @SerializedName("url")
  var url: String? = null

  @SerializedName("hashes")
  var hashes: Map<String, String>? = null
}

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
