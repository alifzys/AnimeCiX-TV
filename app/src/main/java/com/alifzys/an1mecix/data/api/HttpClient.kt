package com.alifzys.an1mecix.data.api

import com.alifzys.an1mecix.BuildConfig
import com.alifzys.an1mecix.core.Constants
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object HttpClient {
    private const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

    /**
     * Chrome benzeri header'lar. Animecix Cloudflare ile korunuyor;
     * curl_cffi'nin TLS impersonation'ı kadar güçlü değil ama JA3 yumuşaksa geçer.
     * Geçmezse Cronet'e geçeriz (Chromium network stack, birebir Chrome).
     */
    private val chromeHeaders = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .header("User-Agent", UA)
            .header("Accept", "application/json, text/html;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "tr-TR,tr;q=0.9,en;q=0.8")
            // Accept-Encoding'i kasten YAZMIYORUZ — OkHttp BridgeInterceptor otomatik
            // gzip ekler ve cevabı şeffaf decompress eder. Manuel header koyunca
            // OkHttp "client kendi handle edecek" sanıp ham gzip'i return ediyor.
            .header("Sec-Ch-Ua", "\"Chromium\";v=\"130\", \"Google Chrome\";v=\"130\", \"Not?A_Brand\";v=\"99\"")
            .header("Sec-Ch-Ua-Mobile", "?0")
            .header("Sec-Ch-Ua-Platform", "\"Windows\"")
            .header("Referer", "${Constants.ANIMECIX_BASE}/")
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "same-origin")
            .build()
        chain.proceed(req)
    }

    /** /secure/ path'lerine X-E-H imzasını ekler. */
    private val xehInterceptor = Interceptor { chain ->
        val req = chain.request()
        if (!req.url.encodedPath.startsWith("/secure/")) return@Interceptor chain.proceed(req)
        val signed = req.newBuilder()
            .header(Xeh.HEADER_NAME, Xeh.build(req.url.toString()))
            .build()
        chain.proceed(signed)
    }

    val okHttp: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(chromeHeaders)
            .addInterceptor(xehInterceptor)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
        }
        builder.build()
    }
}
