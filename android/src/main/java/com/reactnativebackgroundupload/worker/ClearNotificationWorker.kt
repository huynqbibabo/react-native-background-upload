package com.reactnativebackgroundupload.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.reactnativebackgroundupload.NotificationHelpers

class ClearNotificationWorker(appContext: Context, workerParams: WorkerParameters):
  Worker(appContext, workerParams) {
  override fun doWork(): Result {

    // clear upload notification.
    val mNotificationHelpers = NotificationHelpers(applicationContext)
    mNotificationHelpers.startNotify(
      mNotificationHelpers.getCompleteNotificationBuilder().build()
    )
    Log.i("PROGRESS", "complete")

    // Indicate whether the work finished successfully with the Result
    return Result.success()
  }
}
