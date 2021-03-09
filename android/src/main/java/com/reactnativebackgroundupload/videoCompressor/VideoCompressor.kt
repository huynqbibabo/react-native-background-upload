package com.reactnativebackgroundupload.videoCompressor

import com.reactnativebackgroundupload.videoCompressor.Compressor.compressVideo
import com.reactnativebackgroundupload.videoCompressor.Compressor.isRunning
import kotlinx.coroutines.*

object VideoQuality {
  val VERY_HIGH = 2000000
  val HIGH = 1500000
  val MEDIUM = 1000000
  val LOW = 900000
  val VERY_LOW = 800000
}

object VideoCompressor : CoroutineScope by MainScope() {

    private var job: Job? = null

    /**
     * This function compresses a given [srcPath] video file and writes the compressed video file at
     * [destPath]
     *
     * @param [srcPath] the path of the provided video file to be compressed
     * @param [destPath] the path where the output compressed video file should be saved
     * @param [listener] a compression listener that listens to compression [CompressionListener.onStart],
     * [CompressionListener.onProgress], [CompressionListener.onFailure], [CompressionListener.onSuccess]
     * and if the compression was [CompressionListener.onCancelled]
     * @param [quality] to allow choosing a video quality that can be [VideoQuality.LOW], [VideoQuality.VERY_LOW]
     * [VideoQuality.MEDIUM], [VideoQuality.HIGH], and [VideoQuality.VERY_HIGH].
     * This defaults to [VideoQuality.MEDIUM]
     * @param [isMinBitRateEnabled] to determine if the checking for a minimum bitrate threshold
     * before compression is enabled or not. This default to `true`
     * @param [keepOriginalResolution] to keep the original video height and width when compressing.
     * This defaults to `false`
     */
    @JvmStatic
    @JvmOverloads
    fun start(
        srcPath: String,
        destPath: String,
        listener: CompressionListener,
        quality: Int = VideoQuality.MEDIUM,
        isMinBitRateEnabled: Boolean = true,
        keepOriginalResolution: Boolean = false,
    ) {
        job = doVideoCompression(
            srcPath,
            destPath,
            quality,
            isMinBitRateEnabled,
            keepOriginalResolution,
            listener,
        )
    }

    /**
     * Call this function to cancel video compression process which will call [CompressionListener.onCancelled]
     */
    @JvmStatic
    fun cancel() {
        job?.cancel()
        isRunning = false
    }

    private fun doVideoCompression(
        srcPath: String,
        destPath: String,
        quality: Int,
        isMinBitRateEnabled: Boolean,
        keepOriginalResolution: Boolean,
        listener: CompressionListener,
    ) = launch {
        isRunning = true
        listener.onStart()
        val result = startCompression(
            srcPath,
            destPath,
            quality,
            isMinBitRateEnabled,
            keepOriginalResolution,
            listener,
        )

        // Runs in Main(UI) Thread
        if (result.success) {
            listener.onSuccess()
        } else {
            listener.onFailure(result.failureMessage ?: "An error has occurred!")
        }

    }

    private suspend fun startCompression(
        srcPath: String,
        destPath: String,
        quality: Int,
        isMinBitRateEnabled: Boolean,
        keepOriginalResolution: Boolean,
        listener: CompressionListener,
    ): Result = withContext(Dispatchers.IO) {
        return@withContext compressVideo(
            srcPath,
            destPath,
            quality,
            isMinBitRateEnabled,
            keepOriginalResolution,
            object : CompressionProgressListener {
                override fun onProgressChanged(percent: Float) {
                    listener.onProgress(percent)
                }

                override fun onProgressCancelled() {
                    listener.onCancelled()
                }
            },
        )
    }
}
