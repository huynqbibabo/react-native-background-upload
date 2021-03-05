package com.reactnativebackgroundupload

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import java.io.*


class BackgroundUploadModule(private val reactContext: ReactApplicationContext, private val icon: Int) : ReactContextBaseJavaModule(reactContext) {
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
    startNotify()

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

  fun startNotify() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "Upload notification channel"
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel("UploadChannel", name, importance).apply {
        description = name
      }
      // Register the channel with the system
      val notificationManager: NotificationManager =
        reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }

    val mBuilder = NotificationCompat.Builder(reactContext, "UploadChannel")
      .setContentTitle("File Download")
      .setContentText("Download in progress")
      .setSmallIcon(icon)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    // Start a the operation in a background thread
    // Start a the operation in a background thread
    Thread {
      var incr: Int
      // Do the "lengthy" operation 20 times
      incr = 0
      while (incr <= 100) {

        // Sets the progress indicator to a max value, the current completion percentage and "determinate" state
        mBuilder.setProgress(100, incr, false)
        // Displays the progress bar for the first time.
        with(NotificationManagerCompat.from(reactContext)) {
          // notificationId is a unique int for each notification that you must define
          notify(1, mBuilder.build())
        }

        // Sleeps the thread, simulating an operation
        try {
          // Sleep for 1 second
          Thread.sleep((1 * 1000).toLong())
        } catch (e: InterruptedException) {
          Log.d("TAG", "sleep failure")
        }
        incr += 5
      }
      // When the loop is finished, updates the notification
      mBuilder.setContentText("Download completed") // Removes the progress bar
        .setProgress(0, 0, false)
      with(NotificationManagerCompat.from(reactContext)) {
        notify(1, mBuilder.build())
      }
    } // Starts the thread by calling the run() method in its Runnable
      .start()
  }
}
