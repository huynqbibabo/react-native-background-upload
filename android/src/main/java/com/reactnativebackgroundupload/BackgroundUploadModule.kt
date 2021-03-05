package com.reactnativebackgroundupload

import android.annotation.SuppressLint
import android.net.Uri
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkContinuation
import androidx.work.WorkManager
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.reactnativebackgroundupload.model.ModelUploadInput
import com.reactnativebackgroundupload.worker.UploadWorker
import java.io.*

class BackgroundUploadModule(private val reactContext: ReactApplicationContext, private val icon: Int) : ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String {
        return "BackgroundUpload"
    }

    @SuppressLint("EnqueueWork")
    @ReactMethod
    fun startBackgroundUpload(requestUrl: String, filePath: String, fileName: String, hash: ReadableMap) {
//      val videoBytes = FileInputStream(File(realPath)).use { input -> input.readBytes() }
      val realPath = RealPathUtil.getRealPath(reactContext, Uri.parse(filePath))
      val file = File(realPath);

      val chunks: List<String> = splitFile(file, 25);

      val workManager = WorkManager.getInstance(reactContext)
      var workContinuation: WorkContinuation? = null

      chunks.forEachIndexed { index, element ->
        val prt = (index + 1).toString();
        val hashValue = hash.getString(prt) ?: "";
        val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
          .setInputData(ModelUploadInput().createInputDataForUri(requestUrl, fileName, element, hashValue, prt))
          .build()

        workContinuation = workContinuation?.then(uploadRequest)
          ?: workManager.beginWith(uploadRequest)
      }
      workContinuation?.enqueue()
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
      val newFile = File(reactContext.externalCacheDir,  name + "." +  String.format("%03d", partCounter++)) // naming files as <inputFileName>.001, <inputFileName>.002, ...
      val out = FileOutputStream(newFile)
      out.write(buffer, 0, tmp) //tmp is chunk size. Need it for the last chunk, which could be less then 1 mb.
//      out.close()
      result.add(newFile.path)
    }
//    bis.close()
//    fis.close()
    return result
  }
}
