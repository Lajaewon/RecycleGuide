package com.example.recycleguide

import com.example.recycleguide.network.WasteObjectConditionRequest
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
// 객체별 질문 및 답변 관리
// ==================================================

    // 객체별 질문 화면 정보
    private data class ObjectQuestionInput(

        // YOLO predictions 배열의 순서
        val predictionIndex: Int,

        // 질문 영역
        val detailsView: View,

        // 펼치기 / 접기 표시
        val expandIndicatorView: TextView,

        // 답변 완료 상태 표시
        val statusView: TextView,

        // 오염 여부 RadioGroup
        val contaminationGroup: RadioGroup,

        // 내용물 잔여 여부 RadioGroup
        val remainingGroup: RadioGroup,

        // 예 버튼 ID
        val contaminationYesId: Int,

        val remainingYesId: Int
    )

    // 서버로 전달할 객체별 답변 구조


    // 객체별 질문 목록
    private val objectQuestionInputs =
        mutableListOf<ObjectQuestionInput>()

    // 현재 펼쳐진 객체의 인덱스
    private var expandedObjectIndex: Int? = null


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

        // 객체별 질문 목록을 담을 컨테이너
        val objectQuestionContainer =
            view.findViewById<LinearLayout>(
                R.id.layout_object_questions
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
// 7. 객체별 상태 질문 목록 생성
// ----------------------------------------------

        displayObjectQuestions(objectQuestionContainer)


        // ----------------------------------------------
// 7. 다음 버튼
// ----------------------------------------------

        view.findViewById<View>(R.id.btn_next)
            .setOnClickListener {
                submitObjectAnswers()
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
// 객체별 상태 질문 목록 생성
// ==================================================

    private fun displayObjectQuestions(
        container: LinearLayout
    ) {

        // 기존 목록 초기화
        container.removeAllViews()

        objectQuestionInputs.clear()

        expandedObjectIndex = null


        // 탐지된 객체가 없는 경우
        if (predictions.isEmpty()) {

            val emptyView = TextView(requireContext()).apply {

                text = "탐지된 폐기물 객체가 없습니다."

                textSize = 13f

                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_sub
                    )
                )
            }

            container.addView(emptyView)

            updateNextButtonState()

            return
        }


        // YOLO가 탐지한 객체 수만큼 목록 생성
        predictions.forEachIndexed { index, detection ->

            // 1. 객체 목록 XML 불러오기
            val itemView = LayoutInflater.from(requireContext())
                .inflate(
                    R.layout.item_waste_object_question,
                    container,
                    false
                )


            // 2. 객체 번호
            val numberView =
                itemView.findViewById<TextView>(
                    R.id.tv_object_number
                )

            numberView.text = (index + 1).toString()


            // 3. 폐기물 종류
            val titleView =
                itemView.findViewById<TextView>(
                    R.id.tv_object_title
                )

            titleView.text =
                detection.className ?: "종류 확인 불가"


            // 4. AI 신뢰도
            val confidenceView =
                itemView.findViewById<TextView>(
                    R.id.tv_object_confidence
                )

            confidenceView.text =
                detection.confidence?.let {

                    String.format(
                        Locale.KOREA,
                        "AI 신뢰도 %.2f%%",
                        it * 100.0
                    )

                } ?: "AI 신뢰도 확인 불가"


            // 5. 객체 상태 표시
            val statusView =
                itemView.findViewById<TextView>(
                    R.id.tv_object_status
                )


            // 6. 펼치기 표시
            val expandIndicatorView =
                itemView.findViewById<TextView>(
                    R.id.tv_object_expand_indicator
                )


            // 7. 질문 영역
            val detailsView =
                itemView.findViewById<View>(
                    R.id.layout_object_details
                )


            // 8. 오염 여부 RadioGroup
            val contaminationGroup =
                itemView.findViewById<RadioGroup>(
                    R.id.rg_object_contamination
                )


            val contaminationYes =
                itemView.findViewById<View>(
                    R.id.rb_object_contamination_yes
                )


            // 9. 내용물 잔여 여부 RadioGroup
            val remainingGroup =
                itemView.findViewById<RadioGroup>(
                    R.id.rg_object_remaining
                )


            val remainingYes =
                itemView.findViewById<View>(
                    R.id.rb_object_remaining_yes
                )


            // 10. 객체별 입력 데이터 저장
            val input = ObjectQuestionInput(

                predictionIndex = index,

                detailsView = detailsView,

                expandIndicatorView = expandIndicatorView,

                statusView = statusView,

                contaminationGroup = contaminationGroup,

                remainingGroup = remainingGroup,

                contaminationYesId = contaminationYes.id,

                remainingYesId = remainingYes.id
            )

            objectQuestionInputs.add(input)


            // 11. 객체 목록 클릭 이벤트
            itemView.findViewById<View>(
                R.id.layout_object_header
            ).setOnClickListener {

                toggleObjectDetails(index)
            }


            // 12. 오염 여부 변경 감지
            contaminationGroup.setOnCheckedChangeListener {
                    _, _ ->

                updateObjectStatus(input)
            }


            // 13. 내용물 잔여 여부 변경 감지
            remainingGroup.setOnCheckedChangeListener {
                    _, _ ->

                updateObjectStatus(input)
            }


            // 14. 최종 목록에 추가
            container.addView(itemView)


            Log.d(
                TAG,
                "객체 ${index + 1} 목록 생성: ${detection.className}"
            )
        }


        // 첫 번째 객체 질문을 기본적으로 펼치기
        objectQuestionInputs.firstOrNull()?.let {

            it.detailsView.visibility = View.VISIBLE

            it.expandIndicatorView.text = "⌃"

            expandedObjectIndex = 0
        }


        // 다음 버튼 활성화 상태 갱신
        updateNextButtonState()


        Log.d(
            TAG,
            "전체 객체 목록 생성 완료: ${objectQuestionInputs.size}개"
        )
    }


// ==================================================
// 객체별 질문 펼치기 / 접기
// ==================================================

    private fun toggleObjectDetails(index: Int) {

        val selectedInput =
            objectQuestionInputs.getOrNull(index) ?: return


        // 이미 펼쳐진 객체를 다시 누른 경우
        if (expandedObjectIndex == index) {

            selectedInput.detailsView.visibility = View.GONE

            selectedInput.expandIndicatorView.text = "⌄"

            expandedObjectIndex = null

            return
        }


        // 기존에 펼쳐진 객체 접기
        expandedObjectIndex?.let { previousIndex ->

            val previousInput =
                objectQuestionInputs.getOrNull(previousIndex)

            previousInput?.detailsView?.visibility = View.GONE

            previousInput?.expandIndicatorView?.text = "⌄"
        }


        // 선택한 객체 펼치기
        selectedInput.detailsView.visibility = View.VISIBLE

        selectedInput.expandIndicatorView.text = "⌃"

        expandedObjectIndex = index
    }


// ==================================================
// 객체별 답변 완료 상태 갱신
// ==================================================

    private fun updateObjectStatus(
        input: ObjectQuestionInput
    ) {

        val contaminationAnswered =
            input.contaminationGroup.checkedRadioButtonId != -1

        val remainingAnswered =
            input.remainingGroup.checkedRadioButtonId != -1


        when {

            // 질문 두 개 모두 답변한 경우
            contaminationAnswered && remainingAnswered -> {

                input.statusView.text = "완료"

                input.statusView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primary_green
                    )
                )
            }

            // 질문 중 하나만 답변한 경우
            contaminationAnswered || remainingAnswered -> {

                input.statusView.text = "입력 중"

                input.statusView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_sub
                    )
                )
            }

            // 아직 답변하지 않은 경우
            else -> {

                input.statusView.text = "미입력"

                input.statusView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_sub
                    )
                )
            }
        }


        // 전체 객체의 답변 상태 확인
        updateNextButtonState()
    }


// ==================================================
// 다음 버튼 활성화 상태 관리
// ==================================================

    private fun updateNextButtonState() {

        val nextButton =
            view?.findViewById<View>(R.id.btn_next)
                ?: return


        // 모든 객체의 질문에 답변했는지 확인
        val allAnswered =
            objectQuestionInputs.isNotEmpty() &&
                    objectQuestionInputs.all { input ->

                        input.contaminationGroup.checkedRadioButtonId != -1 &&
                                input.remainingGroup.checkedRadioButtonId != -1
                    }


        nextButton.isEnabled = allAnswered
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
// 객체별 답변 수집 및 검증
// ==================================================


    private fun submitObjectAnswers() {

        val currentView = view ?: return

        // ==========================================
        // 1. 모든 객체의 답변 여부 확인
        // ==========================================

        val incompleteInput = objectQuestionInputs.firstOrNull { input ->

            input.contaminationGroup.checkedRadioButtonId == -1 ||
                    input.remainingGroup.checkedRadioButtonId == -1
        }

        if (incompleteInput != null) {

            toggleObjectDetails(incompleteInput.predictionIndex)

            Toast.makeText(
                requireContext(),
                "모든 객체의 상태를 입력해 주세요.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ==========================================
        // 2. 서버 파일명 확인
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
        // 3. 객체별 요청 데이터 생성
        // ==========================================

        val requestObjects = objectQuestionInputs.map { input ->

            WasteObjectConditionRequest(

                predictionIndex = input.predictionIndex,

                isContaminated =
                    input.contaminationGroup.checkedRadioButtonId ==
                            input.contaminationYesId,

                hasRemainingContents =
                    input.remainingGroup.checkedRadioButtonId ==
                            input.remainingYesId
            )
        }


        // ==========================================
        // 4. 전체 요청 데이터 생성
        // ==========================================

        val request = WasteGuideRequest(

            filename = filename,

            objects = requestObjects,

            region = null
        )


        // ==========================================
        // 5. 중복 요청 방지
        // ==========================================

        val nextButton =
            currentView.findViewById<View>(R.id.btn_next)

        nextButton.isEnabled = false


        // ==========================================
        // 6. 전송 데이터 로그
        // ==========================================

        Log.d(TAG, "가이드 요청 시작")
        Log.d(TAG, "서버 파일명: $filename")
        Log.d(TAG, "전송 객체 수: ${request.objects.size}")

        request.objects.forEach { answer ->

            Log.d(
                TAG,
                """
            prediction_index: ${answer.predictionIndex}
            오염 여부: ${answer.isContaminated}
            내용물 잔여 여부: ${answer.hasRemainingContents}
            """.trimIndent()
            )
        }


        Toast.makeText(
            requireContext(),
            "분리배출 안내 요청 중...",
            Toast.LENGTH_SHORT
        ).show()


        // ==========================================
        // 7. FastAPI 서버에 실제 요청
        // ==========================================

        RetrofitClient.api.generateGuide(request)

            .enqueue(object : Callback<WasteGuideResponse> {

                override fun onResponse(
                    call: Call<WasteGuideResponse>,
                    response: Response<WasteGuideResponse>
                ) {

                    if (!isAdded || view == null) return

                    if (response.isSuccessful) {

                        val result = response.body()

                        if (
                            result != null &&
                            result.success &&
                            !result.objects.isNullOrEmpty()
                        ) {

                            // ======================================
                            // 1. 전체 Gemini 응답 JSON으로 변환
                            // ======================================

                            val responseJson = Gson().toJson(result)

                            Log.d(
                                TAG,
                                "Gemini 안내 생성 성공"
                            )

                            Log.d(
                                TAG,
                                "객체별 안내문 개수: ${result.objects.size}"
                            )

                            // ======================================
                            // 2. 객체별 응답 확인
                            // ======================================

                            result.objects.forEach { guide ->

                                Log.d(
                                    TAG,
                                    "객체 ${guide.predictionIndex + 1}: ${guide.wasteType}"
                                )
                                Log.d(
                                    TAG,
                                    "Gemini 자연어 답변: ${guide.answer}"
                                )
                            }

                            // ======================================
                            // 3. RegionInfoFragment로 화면 이동
                            // ======================================

                            parentFragmentManager.beginTransaction()
                                .setReorderingAllowed(true)
                                .replace(
                                    R.id.fragment_container,
                                    RegionInfoFragment.newInstance(responseJson)
                                )
                                .addToBackStack("region_info")
                                .commit()

                        } else {

                            // 서버 처리 실패 또는 객체별 응답 누락
                            nextButton.isEnabled = true

                            Log.e(
                                TAG,
                                "가이드 응답이 비어 있거나 처리에 실패했습니다."
                            )

                            Toast.makeText(
                                requireContext(),
                                result?.message ?: "분리배출 안내 생성에 실패했습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    } else {

                        // HTTP 오류
                        nextButton.isEnabled = true

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
                        "네트워크 오류로 분리배출 안내를 불러오지 못했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        objectQuestionInputs.clear()
        expandedObjectIndex = null
    }

    companion object {
        private const val TAG = "WasteResultFragment"

        const val ARG_PHOTO_PATH = "arg_photo_path"
        const val ARG_ANNOTATED_IMAGE_URL = "arg_annotated_image_url"
        const val ARG_WASTE_TYPE = "arg_waste_type"
        const val ARG_CONFIDENCE = "arg_confidence"
        const val ARG_TOTAL_COUNT = "arg_total_count"
        const val ARG_DETECTED = "arg_detected"
        const val ARG_ANALYSIS_MESSAGE = "arg_analysis_message"
        const val ARG_SERVER_FILENAME = "arg_server_filename"
        const val ARG_PREDICTIONS_JSON = "arg_predictions_json"

        fun newInstance(
            photoPath: String? = null,
            annotatedImageUrl: String? = null,
            wasteType: String? = null,
            confidence: Double? = null,
            totalCount: Int = 0,
            detected: Boolean = false,
            analysisMessage: String? = null,
            serverFilename: String? = null,
            predictionsJson: String? = null
        ): WasteResultFragment {
            return WasteResultFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHOTO_PATH, photoPath)
                    putString(ARG_ANNOTATED_IMAGE_URL, annotatedImageUrl)
                    putString(ARG_WASTE_TYPE, wasteType)
                    confidence?.let { putDouble(ARG_CONFIDENCE, it) }
                    putInt(ARG_TOTAL_COUNT, totalCount)
                    putBoolean(ARG_DETECTED, detected)
                    putString(ARG_ANALYSIS_MESSAGE, analysisMessage)
                    putString(ARG_SERVER_FILENAME, serverFilename)
                    putString(ARG_PREDICTIONS_JSON, predictionsJson)
                }
            }
        }
    }
}