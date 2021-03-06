package com.reactnativebackgroundupload

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timer


class BackgroundUploadModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String {
    return "BackgroundUpload"
  }

  @ReactMethod
  fun startEncodingVideo(filePath: String) {
    val realPath = RealPathUtil.getRealPath(reactContext, Uri.parse(filePath))
    val outputPath = "${reactContext.externalCacheDir}${System.currentTimeMillis()}.mp4"

    // take too long
    val rc = FFmpeg.execute("-i $realPath -vcodec h264 -b:v 1000k -acodec aac $outputPath")

    Config.enableStatisticsCallback { newStatistics -> Log.d(Config.TAG, String.format("frame: %d, time: %d", newStatistics.videoFrameNumber, newStatistics.time))
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
  fun startBackgroundUpload(requestUrl: String, filePath: String, fileName: String, hash: ReadableMap) {
    val mNotificationHelpers = NotificationHelpers(reactContext)
    mNotificationHelpers.createNotificationChannel()

    var progress = 0
    val timer = Timer()
    timer.scheduleAtFixedRate(object : TimerTask() {
      override fun run() {
        if (progress > 100) {
          mNotificationHelpers.cancelNotification()
          timer.cancel()
        } else {
          mNotificationHelpers.startNotify(
            mNotificationHelpers.getProgressNotificationBuilder(progress).build()
          )
          progress += 5
        }
      }
    }, 0, 200)

//    val realPath = RealPathUtil.getRealPath(reactContext, Uri.parse(filePath))
//    val file = File(realPath)
//
//    val chunks: List<String> = splitFile(file, 5);
//
//    val workManager = WorkManager.getInstance(reactContext)
//    var workContinuation: WorkContinuation? = null
//
//    chunks.forEachIndexed { index, element ->
//      val prt = (index + 1).toString();
//      val hashValue = hash.getString(prt) ?: "";
//      val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
//        .setInputData(ModelUploadInput().createInputDataForUpload(requestUrl, fileName, element, hashValue, prt))
//        .build()
//
//      workContinuation = workContinuation?.then(uploadRequest)
//        ?: workManager.beginWith(uploadRequest)
//    }
//    workContinuation?.enqueue()

  }

  @Throws(IOException::class)
  fun splitFile(f: File, chunkSize: Int): List<String> {
    var partCounter = 1
    val result: MutableList<String> = ArrayList()
    val sizeOfFiles = 1024 * 1024 * chunkSize
    val buffer = ByteArray(sizeOfFiles) // create a buffer of bytes sized as the one chunk size
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
