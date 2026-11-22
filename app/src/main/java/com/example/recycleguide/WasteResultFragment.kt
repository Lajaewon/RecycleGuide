
package com.example.recycleguide

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast

import androidx.fragment.app.Fragment

import com.bumptech.glide.Glide

import java.io.File
import java.util.Locale

import com.example.recycleguide.network.RetrofitClient
import com.example.recycleguide.network.WasteGuideRequest
import com.example.recycleguide.network.WasteGuideResponse
import com.example.recycleguide.network.WasteDetection

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.ContextCompat


class WasteResultFragment : Fragment() {

    // ==================================================
    // 전달받을 데이터
    // ==================================================

    private var serverFilename: String? = null

    private var photoPath: String? = null

    private var annotatedImageUrl: String? = null

    private var wasteType: String? = null

    private var confidence: Double? = null

    private var totalCount: Int = 0

    private var detected: Boolean = false

    private var analysisMessage: String? = null

    // YOLO가 탐지한 전체 객체 목록
    private var predictions: List<WasteDetection> = emptyList()


    // ==================================================
    // Fragment 생성 시 전달받은 데이터 저장
    // ==================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        arguments?.let { args ->

            photoPath = args.getString(ARG_PHOTO_PATH)

            annotatedImageUrl =
                args.getString(ARG_ANNOTATED_IMAGE_URL)

            wasteType =
                args.getString(ARG_WASTE_TYPE)

            confidence =
                if (args.containsKey(ARG_CONFIDENCE)) {
                    args.getDouble(ARG_CONFIDENCE)
                } else {
                    null
                }

            totalCount =
                args.getInt(ARG_TOTAL_COUNT, 0)

            detected =
                args.getBoolean(ARG_DETECTED, false)

            analysisMessage =
                args.getString(ARG_ANALYSIS_MESSAGE)

            serverFilename =
                args.getString(ARG_SERVER_FILENAME)

            // CameraFragment에서 전달한 YOLO 객체 목록 JSON
            val predictionsJson =
                args.getString(ARG_PREDICTIONS_JSON)

// JSON 문자열을 Kotlin 객체 목록으로 변환
            predictions = try {

                if (predictionsJson.isNullOrBlank()) {

                    emptyList()

                } else {

                    val listType =
                        object : TypeToken<List<WasteDetection>>() {}.type

                    Gson().fromJson<List<WasteDetection>>(
                        predictionsJson,
                        listType
                    ) ?: emptyList()
                }

            } catch (e: Exception) {

                Log.e(TAG, "YOLO 객체 목록 변환 실패", e)

                emptyList()
            }
        }
    }


    // ==================================================
    // 결과 화면 XML 연결
    // ==================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_waste_result,
            container,
            false
        )
    }


    // ==================================================
    // 화면 생성 후 실행
    // ==================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)


        // ----------------------------------------------
        // 1. XML View 연결
        // ----------------------------------------------

        val photoImageView =
            view.findViewById<ImageView>(R.id.iv_waste_photo)

        val wasteTypeView =
            view.findViewById<TextView>(R.id.tv_waste_type)

        val confidenceView =
            view.findViewById<TextView>(R.id.tv_waste_confidence)

        val analysisMessageView =
            view.findViewById<TextView>(R.id.tv_waste_analysis_message)

        val objectConfidenceContainer =
            view.findViewById<LinearLayout>(
                R.id.layout_object_confidence
            )


        // ----------------------------------------------
        // 2. YOLO 분석 이미지 표시
        // ----------------------------------------------

        displayPhoto(photoImageView)


        // ----------------------------------------------
        // 3. 폐기물 종류 표시
        // ----------------------------------------------

        // ----------------------------------------------
// 3. 인식된 폐기물 종류 전체 표시
// ----------------------------------------------

// 종류별 중복 제거
        val detectedTypes = predictions
            .mapNotNull { it.className }
            .filter { it.isNotBlank() }
            .distinct()

        wasteTypeView.text = when {

            detected && detectedTypes.isNotEmpty() -> {
                detectedTypes.joinToString(" · ")
            }

            detected && !wasteType.isNullOrBlank() -> {
                wasteType
            }

            detected -> {
                "폐기물 종류 확인 불가"
            }

            else -> {
                "폐기물 미감지"
            }
        }


// ----------------------------------------------
// 4. 전체 객체 탐지 개수 표시
// ----------------------------------------------

        val detectedObjectCount = predictions.size

        confidenceView.text = when {

            detectedObjectCount > 0 -> {
                "총 ${detectedObjectCount}개 객체 탐지"
            }

            detected && totalCount > 0 -> {
                "총 ${totalCount}개 객체 탐지"
            }

            else -> {
                "탐지된 객체 없음"
            }
        }


// ----------------------------------------------
// 5. 종류별 검출 개수 표시
// ----------------------------------------------

// 종류별 개수를 저장할 Map
        val typeCounts = linkedMapOf<String, Int>()

        predictions.forEach { detection ->

            val className = detection.className
                ?.takeIf { it.isNotBlank() }
                ?: return@forEach

            typeCounts[className] =
                (typeCounts[className] ?: 0) + 1
        }

// 종류별 검출 개수 문자열 생성
        analysisMessageView.text =

            if (typeCounts.isNotEmpty()) {

                typeCounts.entries.joinToString(" · ") {
                    "${it.key} ${it.value}개"
                }

            } else {

                analysisMessage
                    ?: "종류별 검출 정보를 확인할 수 없습니다."
            }


// ----------------------------------------------
// 6. 객체별 신뢰도 목록 생성
// ----------------------------------------------

        displayObjectConfidences(objectConfidenceContainer)

        // ----------------------------------------------
// 7. 다음 버튼
// ----------------------------------------------

        view.findViewById<View>(R.id.btn_next)
            .setOnClickListener {
                submitAnswers()
            }

        // ----------------------------------------------
        // 7. 뒤로 가기 버튼
        // ----------------------------------------------

        view.findViewById<View>(R.id.btn_back_result)
            ?.setOnClickListener {

                parentFragmentManager.popBackStack()
            }
    }

    // ==================================================
// YOLO 객체별 신뢰도 목록 생성
// ==================================================

    private fun displayObjectConfidences(
        container: LinearLayout
    ) {

        // 기존 목록 초기화
        container.removeAllViews()


        // 탐지 결과가 없는 경우
        if (predictions.isEmpty()) {

            val emptyView = TextView(requireContext()).apply {

                text = "탐지된 객체 정보가 없습니다."

                textSize = 12f

                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_sub
                    )
                )
            }

            container.addView(emptyView)

            return
        }


        // YOLO 탐지 결과를 하나씩 반복
        predictions.forEachIndexed { index, detection ->


            // ------------------------------------------
            // 1. 객체 한 개를 표시할 가로 레이아웃
            // ------------------------------------------

            val row = LinearLayout(requireContext()).apply {

                orientation = LinearLayout.HORIZONTAL

                gravity = Gravity.CENTER_VERTICAL

                setPadding(
                    0,
                    dpToPxForResult(5),
                    0,
                    dpToPxForResult(5)
                )

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }


            // ------------------------------------------
            // 2. 객체 번호와 폐기물 종류
            // ------------------------------------------

            val objectNameView = TextView(requireContext()).apply {

                text = "객체 ${index + 1} · ${
                    detection.className ?: "종류 확인 불가"
                }"

                textSize = 12f

                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_sub
                    )
                )

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }


            // ------------------------------------------
            // 3. 객체별 AI 신뢰도
            // ------------------------------------------

            val confidenceView = TextView(requireContext()).apply {

                text = detection.confidence?.let {

                    String.format(
                        Locale.KOREA,
                        "%.2f%%",
                        it * 100.0
                    )

                } ?: "확인 불가"

                textSize = 12f

                gravity = Gravity.END

                setTypeface(null, Typeface.BOLD)

                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_main
                    )
                )
            }


            // ------------------------------------------
            // 4. 가로 레이아웃에 TextView 추가
            // ------------------------------------------

            row.addView(objectNameView)

            row.addView(confidenceView)


            // ------------------------------------------
            // 5. 최종 목록에 추가
            // ------------------------------------------

            container.addView(row)


            // ------------------------------------------
            // 6. 객체 사이 구분선
            // ------------------------------------------

            if (index < predictions.lastIndex) {

                val divider = View(requireContext()).apply {

                    setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.bg_green_light
                        )
                    )
                }

                container.addView(
                    divider,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPxForResult(1)
                    )
                )
            }
        }
    }


// ==================================================
// DP를 PX로 변환
// ==================================================

    private fun dpToPxForResult(dp: Int): Int {

        return (
                dp * resources.displayMetrics.density
                ).toInt()
    }



    // ==================================================
    // Annotated 이미지 표시
    // ==================================================

    private fun displayPhoto(imageView: ImageView) {

        // 원본 사진 파일 확인
        val originalFile =
            photoPath?.let { File(it) }
                ?.takeIf { it.exists() }


        // 서버에서 받은 Annotated 이미지 URL
        val imageUrl = annotatedImageUrl


        // Annotated 이미지 URL이 존재하는 경우
        if (!imageUrl.isNullOrBlank()) {

            val originalRequest =
                originalFile?.let {
                    Glide.with(this)
                        .load(it)
                        .fitCenter()
                }

            val imageRequest =
                Glide.with(this)
                    .load(imageUrl)
                    .fitCenter()


            // 이미지 다운로드 실패 시 원본 사진 표시
            if (originalRequest != null) {
                imageRequest.error(originalRequest)
            }

            imageRequest.into(imageView)

        } else if (originalFile != null) {

            // Annotated 이미지가 없으면 원본 사진 표시
            Glide.with(this)
                .load(originalFile)
                .fitCenter()
                .into(imageView)
        }
    }


    // ==================================================
    // 라디오 버튼 답변 수집
    // ==================================================


    private fun submitAnswers() {

        val currentView = view ?: return

        // ==========================================
        // 1. RadioGroup 가져오기
        // ==========================================

        val contaminationGroup =
            currentView.findViewById<RadioGroup>(
                R.id.rg_contamination
            )

        val remainingContentsGroup =
            currentView.findViewById<RadioGroup>(
                R.id.rg_remaining_contents
            )


        // ==========================================
        // 2. 선택 여부 확인
        // ==========================================

        val contaminationChoice =
            contaminationGroup.checkedRadioButtonId

        if (contaminationChoice == -1) {

            Toast.makeText(
                requireContext(),
                "첫 번째 질문에 답변해 주세요.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        val remainingContentsChoice =
            remainingContentsGroup.checkedRadioButtonId

        if (remainingContentsChoice == -1) {

            Toast.makeText(
                requireContext(),
                "두 번째 질문에 답변해 주세요.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // 3. Boolean으로 변환
        // ==========================================

        val isContaminated =
            when (contaminationChoice) {

                R.id.rb_contamination_yes -> true

                R.id.rb_contamination_no -> false

                else -> return
            }


        val hasRemainingContents =
            when (remainingContentsChoice) {

                R.id.rb_remaining_yes -> true

                R.id.rb_remaining_no -> false

                else -> return
            }


        // ==========================================
        // 4. 서버 파일명 확인
        // ==========================================

        val filename = serverFilename

        if (filename.isNullOrBlank()) {

            Toast.makeText(
                requireContext(),
                "서버 이미지 파일명을 확인할 수 없습니다.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // 5. FastAPI 전송 데이터 생성
        // ==========================================

        val request = WasteGuideRequest(

            filename = filename,

            isContaminated = isContaminated,

            hasRemainingContents = hasRemainingContents,

            region = null
        )


        // ==========================================
        // 6. 중복 요청 방지
        // ==========================================

        val nextButton =
            currentView.findViewById<View>(R.id.btn_next)

        nextButton.isEnabled = false


        Toast.makeText(
            requireContext(),
            "분리배출 안내 요청 중...",
            Toast.LENGTH_SHORT
        ).show()


        // ==========================================
        // 7. FastAPI /api/guide 요청
        // ==========================================

        Log.d(TAG, "가이드 요청 시작")
        Log.d(TAG, "서버 파일명: $filename")
        Log.d(TAG, "오염 여부: $isContaminated")
        Log.d(TAG, "내용물 잔여 여부: $hasRemainingContents")


        RetrofitClient.api.generateGuide(request)
            .enqueue(object : Callback<WasteGuideResponse> {

                // 서버 응답 수신
                override fun onResponse(
                    call: Call<WasteGuideResponse>,
                    response: Response<WasteGuideResponse>
                ) {

                    if (!isAdded || view == null) return

                    nextButton.isEnabled = true


                    if (response.isSuccessful) {

                        val result = response.body()

                        if (result?.success == true) {

                            // 통신 성공
                            Log.d(
                                TAG,
                                "가이드 API 통신 성공"
                            )

                            Log.d(
                                TAG,
                                "폐기물 종류: ${result.wasteType}"
                            )

                            Log.d(
                                TAG,
                                "서버 메시지: ${result.message}"
                            )

                            Toast.makeText(
                                requireContext(),
                                result.message
                                    ?: "서버 통신 성공!",
                                Toast.LENGTH_LONG
                            ).show()

                        } else {

                            Log.e(
                                TAG,
                                "서버 처리 실패: ${result?.message}"
                            )

                            Toast.makeText(
                                requireContext(),
                                "가이드 요청 처리에 실패했습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    } else {

                        val errorBody =
                            response.errorBody()?.string()

                        Log.e(
                            TAG,
                            "HTTP 오류: ${response.code()}, $errorBody"
                        )

                        Toast.makeText(
                            requireContext(),
                            "서버 요청 실패 (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


                // 서버 연결 실패
                override fun onFailure(
                    call: Call<WasteGuideResponse>,
                    t: Throwable
                ) {

                    if (!isAdded || view == null) return

                    nextButton.isEnabled = true

                    Log.e(
                        TAG,
                        "가이드 서버 통신 실패",
                        t
                    )

                    Toast.makeText(
                        requireContext(),
                        "서버에 연결할 수 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }



    // ==================================================
    // Fragment 생성 및 데이터 전달
    // ==================================================

    companion object {

        private const val TAG = "WasteResultFragment"

        private const val ARG_PHOTO_PATH =
            "photo_path"

        private const val ARG_ANNOTATED_IMAGE_URL =
            "annotated_image_url"

        private const val ARG_WASTE_TYPE =
            "waste_type"

        private const val ARG_CONFIDENCE =
            "confidence"

        private const val ARG_TOTAL_COUNT =
            "total_count"

        private const val ARG_DETECTED =
            "detected"

        private const val ARG_ANALYSIS_MESSAGE =
            "analysis_message"

        private const val ARG_SERVER_FILENAME =
            "server_filename"

        private const val ARG_PREDICTIONS_JSON =
            "predictions_json"


        fun newInstance(
            photoPath: String,
            annotatedImageUrl: String?,
            wasteType: String?,
            confidence: Double?,
            totalCount: Int,
            detected: Boolean,
            analysisMessage: String,
            serverFilename: String,
            predictionsJson: String = "[]"
        ): WasteResultFragment {

            return WasteResultFragment().apply {

                arguments = Bundle().apply {

                    putString(
                        ARG_PHOTO_PATH,
                        photoPath
                    )

                    putString(
                        ARG_ANNOTATED_IMAGE_URL,
                        annotatedImageUrl
                    )

                    putString(
                        ARG_WASTE_TYPE,
                        wasteType
                    )

                    confidence?.let {
                        putDouble(ARG_CONFIDENCE, it)
                    }

                    putInt(
                        ARG_TOTAL_COUNT,
                        totalCount
                    )

                    putBoolean(
                        ARG_DETECTED,
                        detected
                    )

                    putString(
                        ARG_ANALYSIS_MESSAGE,
                        analysisMessage
                    )
                    putString(
                        ARG_SERVER_FILENAME,
                        serverFilename
                    )

                    putString(
                        ARG_PREDICTIONS_JSON,
                        predictionsJson
                    )
                }
            }
        }
    }
}
