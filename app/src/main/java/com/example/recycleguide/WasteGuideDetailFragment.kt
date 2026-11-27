
package com.example.recycleguide

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.recycleguide.network.WasteGuideObjectResponse
import com.example.recycleguide.network.WasteGuideResponse
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import java.util.Locale

class WasteGuideDetailFragment :
    Fragment(R.layout.fragment_waste_guide_detail) {

    // 서버에서 전달받은 전체 Gemini 응답
    private var guideResponse: WasteGuideResponse? = null


    // ==================================================
    // 1. 전달받은 JSON 데이터 파싱
    // ==================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val responseJson =
            arguments?.getString(ARG_GUIDE_RESPONSE_JSON)

        guideResponse = try {

            if (responseJson.isNullOrBlank()) {

                null

            } else {

                Gson().fromJson(
                    responseJson,
                    WasteGuideResponse::class.java
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Gemini 응답 JSON 파싱 실패",
                e
            )

            null
        }
    }


    // ==================================================
    // 2. 화면 생성 후 실행
    // ==================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        // ----------------------------------------------
        // 뒤로 가기
        // ----------------------------------------------

        view.findViewById<ImageButton>(
            R.id.btn_back_guide_detail
        ).setOnClickListener {

            parentFragmentManager.popBackStack()
        }


        // ----------------------------------------------
        // 서버 응답 확인
        // ----------------------------------------------

        val response = guideResponse

        val guideObjects = response?.objects.orEmpty()

        if (response?.success != true || guideObjects.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "객체별 분리배출 안내를 불러오지 못했습니다.",
                Toast.LENGTH_LONG
            ).show()

            Log.e(
                TAG,
                "상세 안내 데이터가 없거나 응답에 실패했습니다."
            )

            return
        }


        // ----------------------------------------------
        // 전체 객체 개수 표시
        // ----------------------------------------------

        view.findViewById<TextView>(
            R.id.tv_guide_detail_count
        ).text = "총 ${guideObjects.size}개 객체 분석 완료"


        // ----------------------------------------------
        // 설명 표시
        // ----------------------------------------------

        view.findViewById<TextView>(
            R.id.tv_guide_detail_description
        ).text = "각 폐기물의 종류와 입력한 상태를 반영한 맞춤형 분리배출 안내입니다."


        // ----------------------------------------------
        // 객체별 안내 카드 생성
        // ----------------------------------------------

        val guideContainer =
            view.findViewById<LinearLayout>(
                R.id.layout_guide_detail_objects
            )

        displayGuideObjects(
            guideContainer,
            guideObjects
        )
    }


    // ==================================================
    // 3. 객체별 안내 카드 생성
    // ==================================================

    private fun displayGuideObjects(
        container: LinearLayout,
        objects: List<WasteGuideObjectResponse>
    ) {

        container.removeAllViews()

        objects.forEachIndexed { index, guide ->

            // ------------------------------------------
            // 3-1. 객체별 카드
            // ------------------------------------------

            val card = MaterialCardView(requireContext()).apply {

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    if (index > 0) {
                        topMargin = dpToPx(12)
                    }
                }

                radius = dpToPx(18).toFloat()

                cardElevation = 0f

                setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                )

                strokeColor = ContextCompat.getColor(
                    requireContext(),
                    R.color.home_card_stroke
                )

                strokeWidth = dpToPx(1)
            }


            // ------------------------------------------
            // 3-2. 카드 내부 레이아웃
            // ------------------------------------------

            val content = LinearLayout(requireContext()).apply {

                orientation = LinearLayout.VERTICAL

                setPadding(
                    dpToPx(18),
                    dpToPx(18),
                    dpToPx(18),
                    dpToPx(18)
                )
            }


            // ------------------------------------------
            // 3-3. 객체 이름
            // ------------------------------------------

            addGuideText(
                content,
                "객체 ${guide.predictionIndex + 1} · ${guide.wasteType}",
                18f,
                R.color.text_main,
                true
            )


            // ------------------------------------------
            // 3-4. YOLO 신뢰도
            // ------------------------------------------

            addGuideText(
                content,
                "AI 탐지 신뢰도: ${
                    String.format(
                        Locale.KOREA,
                        "%.2f%%",
                        guide.confidence * 100.0
                    )
                }",
                12f,
                R.color.text_sub,
                false,
                5
            )


            // ------------------------------------------
            // 3-5. 사용자 입력 상태
            // ------------------------------------------

            val contaminationText =
                if (guide.isContaminated) {
                    "이물질 오염 있음"
                } else {
                    "이물질 오염 없음"
                }

            val remainingText =
                if (guide.hasRemainingContents) {
                    "내용물 잔여 있음"
                } else {
                    "내용물 잔여 없음"
                }

            addGuideText(
                content,
                "$contaminationText · $remainingText",
                13f,
                R.color.primary_green,
                true,
                10
            )


            // ------------------------------------------
            // 3-6. 구분선
            // ------------------------------------------

            addGuideDivider(content)


            // ------------------------------------------
            // 3-7. Gemini 생성 안내 제목
            // ------------------------------------------

            addGuideText(
                content,
                guide.guideTitle,
                17f,
                R.color.text_main,
                true,
                14
            )


            // ------------------------------------------
            // 3-8. 분리배출 방법 제목
            // ------------------------------------------

            addGuideText(
                content,
                "분리배출 방법",
                15f,
                R.color.primary_green,
                true,
                18
            )


            // ------------------------------------------
            // 3-9. Gemini 생성 단계별 안내
            // ------------------------------------------

            if (guide.steps.isNotEmpty()) {

                guide.steps.forEachIndexed { stepIndex, step ->

                    addGuideText(
                        content,
                        "${stepIndex + 1}. $step",
                        14f,
                        R.color.text_sub,
                        false,
                        9
                    )
                }

            } else {

                addGuideText(
                    content,
                    "분리배출 방법이 제공되지 않았습니다.",
                    13f,
                    R.color.text_sub,
                    false,
                    8
                )
            }


            // ------------------------------------------
            // 3-10. 주의사항
            // ------------------------------------------

            if (guide.cautions.isNotEmpty()) {

                addGuideText(
                    content,
                    "주의사항",
                    15f,
                    R.color.primary_green,
                    true,
                    20
                )

                guide.cautions.forEach { caution ->

                    addGuideText(
                        content,
                        "• $caution",
                        13f,
                        R.color.text_sub,
                        false,
                        8
                    )
                }
            }


            // ------------------------------------------
            // 3-11. 최종 카드 추가
            // ------------------------------------------

            card.addView(content)

            container.addView(card)


            Log.d(
                TAG,
                "상세 안내 카드 생성: 객체 ${guide.predictionIndex + 1}, ${guide.wasteType}"
            )
        }


        Log.d(
            TAG,
            "전체 상세 안내 카드 생성 완료: ${objects.size}개"
        )
    }


    // ==================================================
    // 4. 안내문 TextView 생성
    // ==================================================

    private fun addGuideText(
        parent: LinearLayout,
        text: String,
        textSize: Float,
        textColor: Int,
        isBold: Boolean,
        topMargin: Int = 0
    ) {

        val textView = TextView(requireContext()).apply {

            this.text = text

            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                textSize
            )

            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    textColor
                )
            )

            if (isBold) {
                setTypeface(null, Typeface.BOLD)
            }

            setLineSpacing(
                dpToPx(4).toFloat(),
                1.0f
            )
        }


        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {

            this.topMargin = dpToPx(topMargin)
        }

        parent.addView(
            textView,
            params
        )
    }


    // ==================================================
    // 5. 안내 카드 구분선
    // ==================================================

    private fun addGuideDivider(
        parent: LinearLayout
    ) {

        val divider = View(requireContext()).apply {

            setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.home_card_stroke
                )
            )
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(1)
        ).apply {

            topMargin = dpToPx(12)
        }

        parent.addView(
            divider,
            params
        )
    }


    // ==================================================
    // 6. DP → PX 변환
    // ==================================================

    private fun dpToPx(dp: Int): Int {

        return (
                dp * resources.displayMetrics.density
                ).toInt()
    }


    // ==================================================
    // 7. Fragment 데이터 전달
    // ==================================================

    companion object {

        private const val TAG =
            "WasteGuideDetailFragment"

        private const val ARG_GUIDE_RESPONSE_JSON =
            "guide_response_json"


        fun newInstance(
            guideResponseJson: String
        ): WasteGuideDetailFragment {

            return WasteGuideDetailFragment().apply {

                arguments = Bundle().apply {

                    putString(
                        ARG_GUIDE_RESPONSE_JSON,
                        guideResponseJson
                    )
                }
            }
        }
    }
}
