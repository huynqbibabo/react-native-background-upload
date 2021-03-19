package com.reactnativebackgroundupload

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule

class EventEmitter {
  companion object {
    var reactContext: ReactApplicationContext? = null
    var stateMap = HashMap<Int, String>()
  }

  object EVENT {
    const val onStateChange = "onStateChange"
    const val onTranscoding = "onTranscoding"
    const val onRequestMetadata = "onRequestMetadata"
    const val onUploading = "onUploading"
    const val onChainTask = "onChainTask"
    const val onSuccess = "onSuccess"
    const val onFailure = "onFailure"
    const val onCancelled = "onCancelled"
  }

  object STATE {
    const val IDLE = "idle"
    const val TRANSCODE = "transcoding"
    const val SPLIT = "splitting"
    const val REQUEST_METADATA = "requestMetadata"
    const val UPLOAD = "uploading"
    const val CHAIN_TASK = "chainTaskProcessing"
    const val SUCCESS = "success"
    const val FAILED = "failed"
    const val RETRY = "retry"
    const val CANCELLED = "cancelled"
  }

  private fun sendJSEvent(
    eventName: String,
    params: WritableMap
  ) {
    reactContext
      ?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      ?.emit(eventName, params)
  }

  fun setReactContext(context: ReactApplicationContext) {
    reactContext = context
  }

  fun getCurrentState(channelId: Double, workId: Int): String? {
    return stateMap[workId]
  }

  fun onStateChange(channelId: Double, workId: Int, state: String) {
    val prevState = stateMap[workId]
    stateMap[workId] = state
    val params = Arguments.createMap()
    params.putDouble("channelId", channelId)
    params.putInt("workId", workId)
    params.putString("state", state)
    sendJSEvent(EVENT.onStateChange, params)
    when (state) {
      STATE.SUCCESS -> onSuccess(channelId, workId)
      STATE.CANCELLED -> prevState?.let { onCancelled(channelId, workId, it) }
      STATE.FAILED -> onFailure(channelId, workId)
    }
  }

  fun onTranscoding(channelId: Double, workId: Int, progress: Int, status: String) {
    val params = Arguments.createMap()
    params.putDouble("channelId", channelId)
    params.putInt("workId", workId)
    params.putInt("progress", progress)
    params.putString("status", status)
    sendJSEvent(EVENT.onTranscoding, params)
  }

  fun onRequestMetadata(channelId: Double, workId: Int, status: String, response: String) {
    val params = Arguments.createMap()
    params.putDouble("channelId", channelId)
    params.putInt("workId", workId)
    params.putString("status", status)
    params.putString("response", response)
    sendJSEvent(EVENT.onRequestMetadata, params)
  }

  fun onUpload(channelId: Double, workId: Int, status: String, progress: Int, response: String) {
    val params = Arguments.createMap()
    params.putDouble("channelId", channelId)
    params.putInt("workId", workId)
    params.putInt("progress", progress)
    params.putString("status", status)
    params.putString("response", response)
    sendJSEvent(EVENT.onUploading, params)
  }

  fun onChainTask(channelId: Double, workId: Int, status: String, response: String) {
    val params = Arguments.createMap()
    params.putDouble("channelId", channelId)
    params.putInt("workId", workId)
    params.putString("status", status)
    params.putString("response", response)
    sendJSEvent(EVENT.onChainTask, params)
  }

  private fun onSuccess(channelId: Double, workId: Int) {
    val params = Arguments.createMap()
    params.putDouble("channelId", channelId)
    params.putInt("workId", workId)
    sendJSEvent(EVENT.onSuccess, params)
  }

  private fun onFailure(channelId: Double, workId: Int) {
    val params = Arguments.createMap()
    params.putDouble("channelId", channelId)
    params.putInt("workId", workId)
    sendJSEvent(EVENT.onFailure, params)
  }

  private fun onCancelled(channelId: Double, workId: Int, prevState: String) {
    val params = Arguments.createMap()
    params.putDouble("channelId", channelId)
    params.putInt("workId", workId)
    params.putString("previousState", prevState)
    sendJSEvent(EVENT.onCancelled, params)
  }
}
