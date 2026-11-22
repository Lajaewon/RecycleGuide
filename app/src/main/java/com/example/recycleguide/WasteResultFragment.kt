
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

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


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


        // ----------------------------------------------
        // 2. YOLO 분석 이미지 표시
        // ----------------------------------------------

        displayPhoto(photoImageView)


        // ----------------------------------------------
        // 3. 폐기물 종류 표시
        // ----------------------------------------------

        wasteTypeView.text = when {

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
        // 4. AI 신뢰도 표시
        // ----------------------------------------------

        confidenceView.text =
            if (detected && confidence != null) {

                String.format(
                    Locale.KOREA,
                    "AI 신뢰도: %.2f%%",
                    confidence!! * 100.0
                )

            } else {

                "AI 신뢰도: 확인 불가"
            }


        // ----------------------------------------------
        // 5. 분석 메시지 표시
        // ----------------------------------------------

        analysisMessageView.text =
            analysisMessage ?: "분석 결과를 확인할 수 없습니다."


        // ----------------------------------------------
        // 6. 다음 버튼
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


        fun newInstance(
            photoPath: String,
            annotatedImageUrl: String?,
            wasteType: String?,
            confidence: Double?,
            totalCount: Int,
            detected: Boolean,
            analysisMessage: String,
            serverFilename: String
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
                }
            }
        }
    }
}
