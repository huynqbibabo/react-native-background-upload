package com.reactnativebackgroundupload.worker

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.reactnativebackgroundupload.NotificationHelpers

//class ClearNotificationWorker(appContext: Context, workerParams: WorkerParameters):
//  Worker(appContext, workerParams) {
//  override fun doWork(): Result {
//
//    // clear upload notification.
//    val mNotificationHelpers = NotificationHelpers(applicationContext)
//    mNotificationHelpers.startNotify(
//      mNotificationHelpers.getCompleteNotificationBuilder().build()
//    )
//    Log.i("PROGRESS", "complete")
//
//    // Indicate whether the work finished successfully with the Result
//    return Result.success()
//  }
//}

class ClearNotificationWorker(
  context: Context,
  params: WorkerParameters
) : ListenableWorker(context, params) {
  override fun startWork(): ListenableFuture<Result> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
      // clear upload notification.
      val mNotificationHelpers = NotificationHelpers(applicationContext)
      mNotificationHelpers.startNotify(
        mNotificationHelpers.getCompleteNotificationBuilder().build()
      )
        completer.set(Result.success())
      Log.i("PROGRESS", "complete")
    }
  }
}
