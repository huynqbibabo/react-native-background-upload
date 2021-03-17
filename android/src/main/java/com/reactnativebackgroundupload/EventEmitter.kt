package com.reactnativebackgroundupload

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule

class EventEmitter {
  companion object {
    private var reactContext: ReactApplicationContext? = null
    const val onStart = "onStart"
    const val onTranscode = "onTranscode"
    const val onSplit = "onSplit"
    const val onRequestMetadata = "onRequestMetadata"
    const val onUpload = "onUpload"
    const val onChainTasks = "onChainTasks"
    const val onSuccess = "onSuccess"
    const val onFailure = "onFailure"
    const val onCancelled = "onCancelled"
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

  fun onStart(workId: Double) {
    val params = Arguments.createMap()
    params.putDouble("workId", workId)
    sendJSEvent(onStart, params)
  }

  fun onTranscode(workId: Double) {
    val params = Arguments.createMap()
    params.putDouble("workId", workId)
    sendJSEvent(onTranscode, params)
  }

  fun onSplit(workId: Double) {
    val params = Arguments.createMap()
    params.putDouble("workId", workId)
    sendJSEvent(onSplit, params)
  }

  fun onUpload(workId: Double) {
    val params = Arguments.createMap()
    params.putDouble("workId", workId)
    sendJSEvent(onUpload, params)
  }

  fun onRequestMetadata(workId: Double) {
    val params = Arguments.createMap()
    params.putDouble("workId", workId)
    sendJSEvent(onRequestMetadata, params)
  }

  fun onChainTasks(workId: Double) {
    val params = Arguments.createMap()
    params.putDouble("workId", workId)
    sendJSEvent(onChainTasks, params)
  }

  fun onSuccess(workId: Double) {
    val params = Arguments.createMap()
    params.putDouble("workId", workId)
    sendJSEvent(onSuccess, params)
  }

  fun onFailure(workId: Double) {
    val params = Arguments.createMap()
    params.putDouble("workId", workId)
    sendJSEvent(onFailure, params)
  }

  fun onCancelled(workId: Double) {
    val params = Arguments.createMap()
    params.putDouble("workId", workId)
    sendJSEvent(onCancelled, params)
  }
}
