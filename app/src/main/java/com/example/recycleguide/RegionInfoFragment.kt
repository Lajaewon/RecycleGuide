package com.example.recycleguide // ← 첫 줄은 다른 Fragment 파일과 같은 패키지 이름으로 바꾸세요

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class RegionInfoFragment : Fragment(R.layout.fragment_region_info) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로 가기
        view.findViewById<ImageButton>(R.id.btn_back_region).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 다시 촬영하기 → 카메라 화면으로 이동
        view.findViewById<View>(R.id.btn_retake).setOnClickListener {
            moveToCamera()
        }
    }

    private fun moveToCamera() {
        val fragmentManager = parentFragmentManager

        // 카메라 → 결과 화면으로 갈 때 저장한 "waste_result" 기록까지 되돌림
        // → 지역 정보, 결과 화면이 닫히고 카메라 화면이 다시 나타남
        val returnedToCamera = fragmentManager.popBackStackImmediate(
            BACK_STACK_WASTE_RESULT,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        // 되돌릴 기록이 없을 때만 카메라 화면을 새로 띄움
        if (!returnedToCamera) {
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CameraFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        // CameraFragment의 addToBackStack("waste_result")와 같은 이름
        private const val BACK_STACK_WASTE_RESULT = "waste_result"
    }
}
