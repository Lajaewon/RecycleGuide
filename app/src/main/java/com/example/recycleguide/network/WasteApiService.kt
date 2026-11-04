package com.example.recycleguide.network

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface WasteApiService {

    @Multipart
    @POST("api/upload")
    fun uploadPhoto(
        @Part file: MultipartBody.Part
    ): Call<WasteUploadResponse>

}