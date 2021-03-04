package com.reactnativebackgroundupload.service

//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class RetrofitClient() {
  private var retrofit: Retrofit? = null

  fun getApiService(): ApiService? {
    if (retrofit == null) {
//      val logging = HttpLoggingInterceptor()
//      logging.level = HttpLoggingInterceptor.Level.HEADERS
//
//      val client = OkHttpClient.Builder()
//        .addInterceptor(logging)
//        .build()

      retrofit = Retrofit.Builder()
//        .client(client)
        .baseUrl("https://localhost/")
        .addConverterFactory(GsonConverterFactory.create())
//        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
    }
    return retrofit!!.create(ApiService::class.java)
  }

}
