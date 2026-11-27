
package com.example.recycleguide

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.recycleguide.network.WasteGuideResponse
import com.google.gson.Gson

class RegionInfoFragment : Fragment(R.layout.fragment_region_info) {

    private var guideResponse: WasteGuideResponse? = null

    private var guideResponseJson: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        guideResponseJson =
            arguments?.getString(ARG_GUIDE_RESPONSE_JSON)

        guideResponse = try {
            guideResponseJson?.let {
                Gson().fromJson(
                    it,
                    WasteGuideResponse::class.java
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "가이드 응답 파싱 실패", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로 가기
        view.findViewById<ImageButton>(
            R.id.btn_back_region
        ).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 다시 촬영하기
        view.findViewById<View>(
            R.id.btn_retake
        ).setOnClickListener {
            moveToCamera()
        }

        // 서버 응답 표시
        displayGuideSummary(view)

        // 자세히 보기
        view.findViewById<View>(
            R.id.btn_view_guide_detail
        ).setOnClickListener {

            val json = guideResponseJson ?: return@setOnClickListener

            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                    R.id.fragment_container,
                    WasteGuideDetailFragment.newInstance(json)
                )
                .addToBackStack("guide_detail")
                .commit()
        }
    }

    // ==========================================
    // Gemini 안내 요약 표시
    // ==========================================

    private fun displayGuideSummary(view: View) {

        val response = guideResponse
        val objects = response?.objects.orEmpty()

        if (response?.success != true || objects.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "분리배출 안내를 불러오지 못했습니다.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // 폐기물 종류별 개수 계산
        val typeCounts = linkedMapOf<String, Int>()

        objects.forEach { guide ->
            typeCounts[guide.wasteType] =
                (typeCounts[guide.wasteType] ?: 0) + 1
        }

        // 탐지된 폐기물 종류와 개수
        val itemNameView =
            view.findViewById<TextView>(R.id.tv_item_name)

        itemNameView.text =
            typeCounts.entries.joinToString(" · ") {
                "${it.key} ${it.value}개"
            }

        // 전체 객체 개수
        view.findViewById<TextView>(
            R.id.tv_item_category
        ).text = "총 ${objects.size}개"

        // 첫 번째 객체의 Gemini 안내문을 요약 표시
        val firstGuide = objects.first()

        val preview = buildString {

            append(firstGuide.guideTitle)

            firstGuide.steps.firstOrNull()?.let {
                append("\n$it")
            }

            if (objects.size > 1) {
                append("\n외 ${objects.size - 1}개 객체의 맞춤 안내가 있습니다.")
            }
        }

        view.findViewById<TextView>(
            R.id.tv_disposal_method
        ).text = preview

        Log.d(
            TAG,
            "요약 화면 표시 완료: ${objects.size}개 객체"
        )
    }

    // ==========================================
    // 다시 촬영하기
    // ==========================================

    private fun moveToCamera() {

        val fragmentManager = parentFragmentManager

        val returnedToCamera =
            fragmentManager.popBackStackImmediate(
                BACK_STACK_WASTE_RESULT,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )

        if (!returnedToCamera) {

            fragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    CameraFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {

        private const val TAG = "RegionInfoFragment"

        private const val ARG_GUIDE_RESPONSE_JSON =
            "guide_response_json"

        private const val BACK_STACK_WASTE_RESULT =
            "waste_result"

        fun newInstance(
            guideResponseJson: String
        ): RegionInfoFragment {

            return RegionInfoFragment().apply {

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
