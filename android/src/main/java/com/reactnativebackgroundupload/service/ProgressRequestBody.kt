package com.reactnativebackgroundupload.service

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import java.io.IOException

open class ProgressRequestBody : RequestBody {

  private val mFile: File
  private val ignoreFirstNumberOfWriteToCalls : Int


  constructor(mFile: File) : super(){
    this.mFile = mFile
    ignoreFirstNumberOfWriteToCalls = 0
  }

  constructor(mFile: File, ignoreFirstNumberOfWriteToCalls : Int) : super(){
    this.mFile = mFile
    this.ignoreFirstNumberOfWriteToCalls = ignoreFirstNumberOfWriteToCalls
  }


  var numWriteToCalls = 0

  private val getProgressSubject: PublishSubject<Float> = PublishSubject.create<Float>()

  fun getProgressSubject(): Observable<Float> {
    return getProgressSubject
  }


  override fun contentType(): MediaType? {
//    return MediaType.parse("video/mp4")
    return MediaType.parse("application/octet-stream")
  }

  @Throws(IOException::class)
  override fun contentLength(): Long {
    return mFile.length()
  }

  @Throws(IOException::class)
  override fun writeTo(sink: BufferedSink) {
    numWriteToCalls++

    val fileLength = mFile.length()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    val fis = FileInputStream(mFile)
    var uploaded: Long = 0

    try {
      var read: Int
      var lastProgressPercentUpdate = 0.0f
      read = fis.read(buffer)
      while (read != -1) {

        uploaded += read.toLong()
        sink.write(buffer, 0, read)
        read = fis.read(buffer)

        // when using HttpLoggingInterceptor it calls writeTo and passes data into a local buffer just for logging purposes.
        // the second call to write to is the progress we actually want to track
        if (numWriteToCalls > ignoreFirstNumberOfWriteToCalls ) {
          val progress = (uploaded.toFloat() / fileLength.toFloat()) * 100f
          //prevent publishing too many updates, which slows upload, by checking if the upload has progressed by at least 1 percent
          if (progress - lastProgressPercentUpdate > 1 || progress == 100f) {
            // publish progress
            getProgressSubject.onNext(progress)
            lastProgressPercentUpdate = progress
          }
        }
      }
    } finally {
      fis.close()
    }
  }


  companion object {
    private val DEFAULT_BUFFER_SIZE = 2048
  }
}
