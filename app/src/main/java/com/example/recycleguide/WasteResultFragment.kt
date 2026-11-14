
package com.example.recycleguide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.fragment.app.Fragment

import com.bumptech.glide.Glide

import java.io.File
import java.util.Locale


class WasteResultFragment : Fragment() {

    // ==================================================
    // 전달받을 데이터
    // ==================================================

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

                moveToRegionInfo()
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


        // ----------------------------------------------
        // 1. Annotated 이미지 URL이 존재하는 경우
        // ----------------------------------------------

        if (!imageUrl.isNullOrBlank()) {

            // 다운로드 실패 시 사용할 원본 이미지 요청
            val originalRequest =
                originalFile?.let {
                    Glide.with(this)
                        .load(it)
                        .fitCenter()
                }


            // 서버에서 Annotated 이미지 다운로드
            val imageRequest =
                Glide.with(this)
                    .load(imageUrl)
                    .fitCenter()


            // Annotated 이미지 다운로드 실패 시 원본 이미지 표시
            if (originalRequest != null) {

                imageRequest.error(originalRequest)
            }


            imageRequest.into(imageView)

        }

        // ----------------------------------------------
        // 2. Annotated 이미지가 없으면 원본 사진 표시
        // ----------------------------------------------

        else if (originalFile != null) {

            Glide.with(this)
                .load(originalFile)
                .fitCenter()
                .into(imageView)
        }
    }


    // ==================================================
    // 지역 정보 화면으로 이동
    // ==================================================

    private fun moveToRegionInfo() {

        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                RegionInfoFragment()
            )
            .addToBackStack(null)
            .commit()
    }


    // ==================================================
    // Fragment 생성 및 데이터 전달
    // ==================================================

    companion object {

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


        fun newInstance(
            photoPath: String,
            annotatedImageUrl: String?,
            wasteType: String?,
            confidence: Double?,
            totalCount: Int,
            detected: Boolean,
            analysisMessage: String
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
                }
            }
        }
    }
}
