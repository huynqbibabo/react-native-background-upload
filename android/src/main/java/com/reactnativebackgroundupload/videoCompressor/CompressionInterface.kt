package com.reactnativebackgroundupload.videoCompressor

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread

/**
 * Created by AbedElaziz Shehadeh on 27 Jan, 2020
 * elaziz.shehadeh@gmail.com
 */
interface CompressionListener {
    @WorkerThread
    fun onStart()

    @WorkerThread
    fun onSuccess()

    @WorkerThread
    fun onFailure(failureMessage: String)

    @WorkerThread
    fun onProgress(percent: Float)

    @WorkerThread
    fun onCancelled()
}

interface CompressionProgressListener {
    fun onProgressChanged(percent: Float)
    fun onProgressCancelled()
}
