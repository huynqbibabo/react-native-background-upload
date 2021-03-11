package com.reactnativebackgroundupload.model

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

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

//  var mStringNumberMap: Map<String?, Number?>? = null
//
//  fun getStringNumberMap(): Map<String?, Number?>? {
//    return mStringNumberMap
//  }
//
//  fun setStringNumberMap(stringNumberMap: Map<String?, Number?>?) {
//    mStringNumberMap = stringNumberMap
//  }
//
//  class NumberResponseDeserializer : JsonDeserializer<Metadata?> {
//    @Throws(JsonParseException::class)
//    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Metadata {
//      val response: Metadata = Gson().fromJson(json, Metadata::class.java)
//      var map: Map<String?, Number?> = HashMap()
//      map = Gson().fromJson(json, map.javaClass) as Map<String?, Number?>
//      response.setStringNumberMap(map)
//      return response
//    }
//  }
}
