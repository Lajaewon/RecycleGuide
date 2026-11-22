
package com.example.recycleguide.network

import com.google.gson.annotations.SerializedName

// FastAPI 분리배출 가이드 요청 데이터
data class WasteGuideRequest(

    // 서버에 저장된 원본 이미지 파일명
    @SerializedName("filename")
    val filename: String,

    // 음식물 및 이물질 오염 여부
    @SerializedName("is_contaminated")
    val isContaminated: Boolean,

    // 내용물 잔여 여부
    @SerializedName("has_remaining_contents")
    val hasRemainingContents: Boolean,

    // 사용자 지역 정보 (추후 연동)
    @SerializedName("region")
    val region: String? = null
)
