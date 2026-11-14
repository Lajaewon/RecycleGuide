
package com.example.recycleguide.network

import com.google.gson.annotations.SerializedName

// YOLO 전체 분석 결과
data class WastePrediction(

    @SerializedName("detected")
    val detected: Boolean?,

    @SerializedName("total_count")
    val totalCount: Int?,

    @SerializedName("top_prediction")
    val topPrediction: WasteDetection?,

    @SerializedName("predictions")
    val predictions: List<WasteDetection>?,

    @SerializedName("annotated_filename")
    val annotatedFilename: String?,

    // YOLO 추론 실패 시 반환되는 정보
    @SerializedName("error")
    val error: String?,

    @SerializedName("details")
    val details: String?
)


// 개별 폐기물 탐지 결과
data class WasteDetection(

    @SerializedName("class_id")
    val classId: Int?,

    @SerializedName("class_name")
    val className: String?,

    @SerializedName("confidence")
    val confidence: Double?,

    @SerializedName("box")
    val box: WasteBoundingBox?
)


// 바운딩 박스 좌표
data class WasteBoundingBox(

    val xmin: Double?,
    val ymin: Double?,
    val xmax: Double?,
    val ymax: Double?
)
