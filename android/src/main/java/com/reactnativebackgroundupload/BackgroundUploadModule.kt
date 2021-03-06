package com.reactnativebackgroundupload

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.reactnativebackgroundupload.model.ModelUploadInput
import com.reactnativebackgroundupload.worker.ClearNotificationWorker
import com.reactnativebackgroundupload.worker.UploadWorker
import java.io.*
import java.util.*
import kotlin.collections.ArrayList


class BackgroundUploadModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String {
    return "BackgroundUpload"
  }

  @ReactMethod
  fun startEncodingVideo(filePath: String) {
    val inputPath = RealPathUtil.getRealPath(reactContext, Uri.parse(filePath))
    val outputPath = "${reactContext.externalCacheDir}${System.currentTimeMillis()}.mp4"

    // take too long
    val rc = FFmpeg.execute("-i $inputPath -vcodec h264 -b:v 1000k -acodec aac $outputPath")

    Config.enableStatisticsCallback {
      newStatistics -> Log.d(Config.TAG, String.format("frame: %d, time: %d", newStatistics.videoFrameNumber, newStatistics.time))
    }

    when (rc) {
      Config.RETURN_CODE_SUCCESS -> Log.i(Config.TAG, "Command execution completed successfully.")
      Config.RETURN_CODE_CANCEL -> Log.i(Config.TAG, "Command execution cancelled by user.")
      else -> {
        Log.i(Config.TAG, String.format("Command execution failed with rc=%d and the output below.", rc))
        Config.printLastCommandOutput(Log.INFO)
      }
    }
  }

  @SuppressLint("EnqueueWork")
  @ReactMethod
  fun startBackgroundUpload(requestUrl: String, filePath: String, fileName: String, hash: ReadableMap, chunkSize: Int) {
    NotificationHelpers(reactContext).createNotificationChannel()

    val realPath = RealPathUtil.getRealPath(reactContext, Uri.parse(filePath))
    val file = File(realPath)

    val chunks: List<String> = splitFile(file, chunkSize)

    val workManager = WorkManager.getInstance(reactContext)
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED) // constraints worker with network condition
      .build()
    var workContinuation: WorkContinuation? = null

    chunks.forEachIndexed { index, element ->
      val prt = index + 1
      val hashValue = hash.getString(prt.toString())!!
      val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>().apply {
        setConstraints(constraints)
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

  fun runNotification() {
    val mNotificationHelpers = NotificationHelpers(reactContext)
    mNotificationHelpers.createNotificationChannel()

    var progress = 0
    val timer = Timer()
    timer.scheduleAtFixedRate(object : TimerTask() {
      override fun run() {
        if (progress > 100) {
          mNotificationHelpers.startNotify(
            mNotificationHelpers.getCompleteNotificationBuilder().build()
          )
          timer.cancel()
        } else {
          mNotificationHelpers.startNotify(
            mNotificationHelpers.getProgressNotificationBuilder(progress).build()
          )
          progress += 5
        }
      }
    }, 0, 200)
  }
}
