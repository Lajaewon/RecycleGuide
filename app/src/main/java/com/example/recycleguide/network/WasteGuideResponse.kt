
package com.example.recycleguide.network

import com.google.gson.annotations.SerializedName

// FastAPI에서 반환하는 분리배출 안내 응답 데이터
data class WasteGuideResponse(

    // 요청 처리 성공 여부
    val success: Boolean,

    // 폐기물 종류
    @SerializedName("waste_type")
    val wasteType: String?,

    // 안내 제목
    @SerializedName("guide_title")
    val guideTitle: String?,

    // 단계별 분리배출 방법
    val steps: List<String>?,

    // 주의사항
    val cautions: List<String>?,

    // 처리 메시지
    val message: String?
)
