
package com.example.recycleguide.network

import okhttp3.MultipartBody

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface WasteApiService {

    // ==========================================
    // 1. 폐기물 사진 업로드 및 YOLO 분석
    // ==========================================

    @Multipart
    @POST("api/upload")
    fun uploadPhoto(
        @Part file: MultipartBody.Part
    ): Call<WasteUploadResponse>


    // ==========================================
    // 2. 사용자 응답 기반 분리배출 가이드 요청
    // ==========================================

    @POST("api/guide")
    fun generateGuide(
        @Body request: WasteGuideRequest
    ): Call<WasteGuideResponse>

}
