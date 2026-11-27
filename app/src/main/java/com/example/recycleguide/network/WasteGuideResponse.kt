
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

    // 객체별 자연어 분리배출 안내 목록
    @SerializedName("objects")
    val objects: List<WasteGuideObjectResponse>?,

    // 서버 처리 메시지
    @SerializedName("message")
    val message: String?
)


// 개별 폐기물 객체의 자연어 안내 응답
data class WasteGuideObjectResponse(

    // YOLO predictions 배열의 객체 인덱스
    @SerializedName("prediction_index")
    val predictionIndex: Int,

    // 폐기물 종류
    @SerializedName("waste_type")
    val wasteType: String,

    // YOLO 탐지 신뢰도
    @SerializedName("confidence")
    val confidence: Double,

    // 사용자가 입력한 오염 여부
    @SerializedName("is_contaminated")
    val isContaminated: Boolean,

    // 사용자가 입력한 내용물 잔여 여부
    @SerializedName("has_remaining_contents")
    val hasRemainingContents: Boolean,

    // Gemini가 생성한 자연어 분리배출 안내문
    @SerializedName("answer")
    val answer: String?
)
