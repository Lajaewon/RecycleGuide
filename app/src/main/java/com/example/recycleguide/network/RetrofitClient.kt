
package com.example.recycleguide.network

import android.net.Uri

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // 현재 정상 통신 중인 서버 주소 (그대로 유지)
    private const val BASE_URL =
        "http://172.17.94.30:8000/"

    // Retrofit 통신 객체 생성
    val api: WasteApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(WasteApiService::class.java)
    }


    // YOLO가 생성한 Annotated 이미지 URL 생성
    fun getAnnotatedImageUrl(filename: String): String {

        return BASE_URL +
                "api/annotated/" +
                Uri.encode(filename)
    }
}
