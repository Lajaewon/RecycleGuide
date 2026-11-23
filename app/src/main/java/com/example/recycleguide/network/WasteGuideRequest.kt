
package com.example.recycleguide.network

import com.google.gson.annotations.SerializedName

// FastAPI 객체별 분리배출 가이드 요청
data class WasteGuideRequest(

    // 서버에 저장된 원본 이미지 파일명
    @SerializedName("filename")
    val filename: String,

    // 객체별 상태 응답 목록
    @SerializedName("objects")
    val objects: List<WasteObjectConditionRequest>,

    // 사용자 지역 정보
    @SerializedName("region")
    val region: String? = null
)


// 개별 객체의 상태 응답
data class WasteObjectConditionRequest(

    // YOLO predictions 배열에서의 객체 순서
    @SerializedName("prediction_index")
    val predictionIndex: Int,

    // 음식물 및 이물질 오염 여부
    @SerializedName("is_contaminated")
    val isContaminated: Boolean,

    // 내용물 잔여 여부
    @SerializedName("has_remaining_contents")
    val hasRemainingContents: Boolean
)
