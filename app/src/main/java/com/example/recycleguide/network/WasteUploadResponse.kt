
package com.example.recycleguide.network

import com.google.gson.annotations.SerializedName

data class WasteUploadResponse(

    // 이미지 업로드 성공 여부
    val success: Boolean,

    // 서버에 저장된 파일명
    val filename: String,

    // 스마트폰에서 전송한 원본 파일명
    val originalFilename: String?,

    // 이미지 MIME 타입
    val contentType: String?,

    // 이미지 파일 크기
    val sizeBytes: Long,

    // YOLO 분석 결과
    @SerializedName("prediction")
    val prediction: WastePrediction?,

    // 서버 응답 메시지
    val message: String
)
