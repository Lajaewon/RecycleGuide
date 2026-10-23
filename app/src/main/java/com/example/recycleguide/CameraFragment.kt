package com.example.recycleguide

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class CameraFragment : Fragment(R.layout.fragment_camera) {

    private var previewView: PreviewView? = null
    private var messageView: TextView? = null

    // 카메라 권한 요청
    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startCamera()
            } else {
                showMessage("카메라 권한을 허용해야 사용할 수 있습니다.")
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        previewView = view.findViewById(R.id.previewView)
        messageView = view.findViewById(R.id.tv_camera_message)

        // 카메라 화면에서는 하단 메뉴 숨기기
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottom_navigation)
            ?.visibility = View.GONE

        // 닫기 버튼
        view.findViewById<MaterialButton>(R.id.btn_close_camera)
            .setOnClickListener {
                parentFragmentManager.popBackStack()
            }

        // 카메라 권한 확인
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val preview = previewView ?: return
        val context = context ?: return
        val lifecycleOwner = viewLifecycleOwner

        messageView?.visibility = View.GONE

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            // 이미 화면을 떠났다면 카메라를 연결하지 않음
            if (!lifecycleOwner.lifecycle.currentState
                    .isAtLeast(Lifecycle.State.STARTED)
            ) {
                return@addListener
            }

            try {
                val cameraProvider = cameraProviderFuture.get()

                // 카메라 미리보기 설정
                val previewUseCase = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(preview.surfaceProvider)
                    }

                // 후면 카메라 선택
                val cameraSelector =
                    CameraSelector.DEFAULT_BACK_CAMERA

                // 이전 카메라 연결 해제
                cameraProvider.unbindAll()

                // 카메라를 프래그먼트 생명주기에 연결
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    previewUseCase
                )

            } catch (e: Exception) {
                Log.e("CameraFragment", "카메라 실행 실패", e)
                showMessage("카메라를 시작할 수 없습니다.")
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun showMessage(message: String) {
        messageView?.apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        // 화면을 벗어나면 하단 메뉴 복구
        activity
            ?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            ?.visibility = View.VISIBLE

        previewView = null
        messageView = null

        super.onDestroyView()
    }
}
