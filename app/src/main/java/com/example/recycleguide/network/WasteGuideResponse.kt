
package com.example.recycleguide.network

import com.google.gson.annotations.SerializedName

// FastAPI에서 반환하는 전체 분리배출 안내 응답
data class WasteGuideResponse(

    // 요청 처리 성공 여부
    @SerializedName("success")
    val success: Boolean,

    // 전체 폐기물 종류
    @SerializedName("waste_type")
    val wasteType: String?,

    // 객체별 분리배출 안내 목록
    @SerializedName("objects")
    val objects: List<WasteGuideObjectResponse>?,

    // 하위 호환성을 위한 대표 안내 제목
    @SerializedName("guide_title")
    val guideTitle: String?,

    // 하위 호환성을 위한 대표 분리배출 단계
    @SerializedName("steps")
    val steps: List<String>?,

    // 하위 호환성을 위한 대표 주의사항
    @SerializedName("cautions")
    val cautions: List<String>?,

    // 서버 처리 메시지
    @SerializedName("message")
    val message: String?
)


// 개별 폐기물 객체의 분리배출 안내 응답
data class WasteGuideObjectResponse(

    // YOLO 탐지 배열의 객체 인덱스
    @SerializedName("prediction_index")
    val predictionIndex: Int,

    // 폐기물 종류
    @SerializedName("waste_type")
    val wasteType: String,

    // YOLO 신뢰도
    @SerializedName("confidence")
    val confidence: Double,

    // 이물질 오염 여부
    @SerializedName("is_contaminated")
    val isContaminated: Boolean,

    // 내용물 잔여 여부
    @SerializedName("has_remaining_contents")
    val hasRemainingContents: Boolean,

    // Gemini 생성 안내 제목
    @SerializedName("guide_title")
    val guideTitle: String,

    // 단계별 분리배출 방법
    @SerializedName("steps")
    val steps: List<String>,

    // 객체별 주의사항
    @SerializedName("cautions")
    val cautions: List<String>
)
