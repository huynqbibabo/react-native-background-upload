package com.reactnativebackgroundupload

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule

class EventEmitter {
  companion object {
    var reactContext: ReactApplicationContext? = null
    var stateMap = HashMap<Int, WritableMap>()
  }

  object EVENT {
    const val onStateChange = "onStateChange"
  }

  object STATE {
    const val IDLE = "idle"
    const val TRANSCODE = "transcoding"
    const val SPLIT = "splitting"
    const val REQUEST_METADATA = "requestMetadata"
    const val UPLOAD = "uploading"
    const val SUCCESS = "success"
    const val FAILED = "failed"
    const val CANCELLED = "cancelled"
  }

//  private fun sendJSEvent(
//    eventName: String,
//    params: WritableMap
//  ) {
//    reactContext
//      ?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
//      ?.emit(eventName, params)
//  }

  fun setReactContext(context: ReactApplicationContext) {
    reactContext = context
  }

  fun getCurrentState(workId: Int): WritableMap? {
    return stateMap[workId]
  }

  fun onStateChange(workId: Int, state: String, response: String, progress: Int) {
    val params = Arguments.createMap()
    params.putInt("workId", workId)
    params.putString("state", state)
    params.putString("response", response)
    params.putInt("progress", progress)
//    sendJSEvent(EVENT.onStateChange, params)
    stateMap[workId] = params
    reactContext
      ?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      ?.emit(EVENT.onStateChange, params)
  }
}
