package com.reactnativebackgroundupload

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.work.*
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.reactnativebackgroundupload.model.ModelUploadInput
import com.reactnativebackgroundupload.videoCompressor.CompressionListener
import com.reactnativebackgroundupload.videoCompressor.VideoCompressor
import com.reactnativebackgroundupload.videoCompressor.VideoQuality
import com.reactnativebackgroundupload.worker.ClearNotificationWorker
import com.reactnativebackgroundupload.worker.UploadWorker
import java.io.*
import kotlin.collections.ArrayList

class BackgroundUploadModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String {
    return "BackgroundUpload"
  }

  @ReactMethod
  fun startCompressVideo(filePath: String) {
    val inputPath = RealPathUtil.getRealPath(reactContext, Uri.parse(filePath))
    val outputPath = "${reactContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES)}/${System.currentTimeMillis()}.mp4"

    Log.d("inputPath", inputPath)
    Log.d("outputPath", outputPath)

    val mNotificationHelpers = NotificationHelpers(reactContext)
    mNotificationHelpers.createNotificationChannel()

    VideoCompressor.start(
      inputPath,
      outputPath,
      object : CompressionListener {
        override fun onProgress(percent: Float) {
          // Update UI with progress value
          Log.d("COMPRESSION", "Compression progress: ${percent.toInt()}")
          mNotificationHelpers.startNotify(
            mNotificationHelpers.getProgressNotificationBuilder(percent.toInt()).build()
          )
        }
        override fun onStart() {
          // Compression start
          Log.d("COMPRESSION", "Compression start")
        }
        override fun onSuccess() {
          // On Compression success
          Log.d("COMPRESSION", "Compression success")
          mNotificationHelpers.startNotify(
            mNotificationHelpers.getCompleteNotificationBuilder().build()
          )
        }
        override fun onFailure(failureMessage: String) {
          // On Failure
          Log.d("COMPRESSION", "Compression failure")
          mNotificationHelpers.startNotify(
            mNotificationHelpers.getFailureNotificationBuilder().build()
          )
        }
        override fun onCancelled() {
          // On Cancelled
          Log.d("COMPRESSION", "Compression cancelled")
        }

      }, VideoQuality.MEDIUM, isMinBitRateEnabled = true, keepOriginalResolution = false)
  }

  @SuppressLint("EnqueueWork")
  @ReactMethod
  fun startBackgroundUpload(requestUrl: String, filePath: String, fileName: String, hash: ReadableMap, chunkSize: Int) {
    NotificationHelpers(reactContext).createNotificationChannel()

    val realPath = RealPathUtil.getRealPath(reactContext, Uri.parse(filePath))
    val file = File(realPath)

    val chunks: List<String> = splitFile(file, chunkSize)

    val workManager = WorkManager.getInstance(reactContext)
    val workConstraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED) // constraints worker with network condition
      .build()
    var workContinuation: WorkContinuation? = null

    chunks.forEachIndexed { index, element ->
      val prt = index + 1
      val hashValue = hash.getString(prt.toString())!!
      val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>().apply {
        setConstraints(workConstraints)
        setInputData(
          ModelUploadInput().createInputDataForUpload(requestUrl, fileName, element, hashValue, prt, chunks.size)
        )
      }.build()

      workContinuation = workContinuation?.then(uploadRequest)
        ?: workManager.beginWith(uploadRequest)
    }
    workContinuation?.then(
      OneTimeWorkRequest.from(ClearNotificationWorker::class.java)
    )
    workContinuation?.enqueue()
  }

  @Throws(IOException::class)
  fun splitFile(f: File, chunkSize: Int): List<String> {
    var partCounter = 1
    val result: MutableList<String> = ArrayList()
    val buffer = ByteArray(chunkSize) // create a buffer of bytes sized as the one chunk size
    val fis = FileInputStream(f)
    val bis = BufferedInputStream(fis)
    val name = f.name
    var tmp = 0
    while (bis.read(buffer).also { tmp = it } > 0) {
      val newFile = File(reactContext.externalCacheDir, name + "." + String.format("%03d", partCounter++)) // naming files as <inputFileName>.001, <inputFileName>.002, ...
      val out = FileOutputStream(newFile)
      out.write(buffer, 0, tmp) //tmp is chunk size. Need it for the last chunk, which could be less then 1 mb.
      out.close()
      result.add(newFile.path)
    }
    bis.close()
    fis.close()
    return result
  }
}
