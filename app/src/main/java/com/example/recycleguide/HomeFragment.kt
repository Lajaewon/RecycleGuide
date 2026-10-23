package com.example.recycleguide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 메인 화면 XML 불러오기
        return inflater.inflate(
            R.layout.fragment_home,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val btnCamera =
            view.findViewById<MaterialButton>(R.id.btn_camera)

        val cardLocation =
            view.findViewById<MaterialCardView>(R.id.card_location)

        // 사진 촬영하기 버튼 클릭
        btnCamera.setOnClickListener {

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    CameraFragment()
                )
                .addToBackStack("camera")
                .commit()
        }

        // 현재 위치 카드 클릭
        cardLocation.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "위치 설정 클릭됨",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}