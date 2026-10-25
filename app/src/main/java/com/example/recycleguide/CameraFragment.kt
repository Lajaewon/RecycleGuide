
package com.example.recycleguide

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import java.io.File

class CameraFragment : Fragment(R.layout.fragment_camera) {

    private var previewView: PreviewView? = null
    private var messageView: TextView? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var flashButton: ImageButton? = null

    // 기본 카메라는 후면 카메라
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    // 플래시 상태
    private var flashEnabled = false

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

    // 앨범에서 이미지 선택
    private val galleryLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                Toast.makeText(
                    requireContext(),
                    "이미지를 선택했습니다.",
                    Toast.LENGTH_SHORT
                ).show()

                // 선택 이미지 전달 기능은 다음 단계에서 추가
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        previewView = view.findViewById(R.id.previewView)
        messageView = view.findViewById(R.id.tv_camera_message)
        flashButton = view.findViewById(R.id.btn_flash)

        // 카메라 화면에서는 하단 메뉴 숨기기
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottom_navigation)
            ?.visibility = View.GONE

        // 닫기 버튼
        view.findViewById<ImageButton>(R.id.btn_close_camera)
            .setOnClickListener {
                parentFragmentManager.popBackStack()
            }

        // 촬영 버튼
        view.findViewById<MaterialButton>(R.id.btn_capture)
            .setOnClickListener {
                takePhoto()
            }

        // 플래시 버튼
        flashButton?.setOnClickListener {
            toggleFlash()
        }

        // 앨범 버튼
        view.findViewById<View>(R.id.btn_gallery)
            .setOnClickListener {
                galleryLauncher.launch("image/*")
            }

        // 전면 / 후면 카메라 전환 버튼
        view.findViewById<View>(R.id.btn_switch_camera)
            .setOnClickListener {
                switchCamera()
            }

        // 카메라 권한 확인
        val hasPermission =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startCamera()
        } else {
            permissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    // CameraX 초기화
    private fun startCamera() {
        val context = context ?: return

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            val lifecycleOwner =
                if (view != null) viewLifecycleOwner else return@addListener

            if (!lifecycleOwner.lifecycle.currentState
                    .isAtLeast(Lifecycle.State.STARTED)
            ) {
                return@addListener
            }

            try {
                cameraProvider = cameraProviderFuture.get()
                bindCamera()
            } catch (e: Exception) {
                Log.e(TAG, "카메라 준비 실패", e)
                showMessage("카메라를 시작할 수 없습니다.")
            }

        }, ContextCompat.getMainExecutor(context))
    }

    // 미리보기와 촬영 기능 연결
    private fun bindCamera() {
        val provider = cameraProvider ?: return
        val previewView = previewView ?: return

        try {
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            if (!provider.hasCamera(selector)) {
                showMessage("선택한 카메라를 사용할 수 없습니다.")
                return
            }

            // 실시간 영상 미리보기
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // 사진 촬영 기능
            val captureUseCase = ImageCapture.Builder()
                .setCaptureMode(
                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                )
                .setFlashMode(
                    if (flashEnabled) {
                        ImageCapture.FLASH_MODE_ON
                    } else {
                        ImageCapture.FLASH_MODE_OFF
                    }
                )
                .build()

            // 기존 사용 사례 연결 해제
            provider.unbindAll()

            // 카메라 연결
            camera = provider.bindToLifecycle(
                viewLifecycleOwner,
                selector,
                previewUseCase,
                captureUseCase
            )

            imageCapture = captureUseCase
            messageView?.visibility = View.GONE

        } catch (e: Exception) {
            Log.e(TAG, "카메라 연결 실패", e)
            showMessage("카메라를 연결하지 못했습니다.")
        }
    }

    // 사진 촬영 및 앱 캐시에 저장
    private fun takePhoto() {
        val capture = imageCapture ?: run {
            showMessage("카메라를 준비하고 있습니다.")
            return
        }

        val context = context ?: return

        val photoFile = File(
            context.cacheDir,
            "waste_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(photoFile)
                .build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    outputFileResults: ImageCapture.OutputFileResults
                ) {
                    Toast.makeText(
                        context,
                        "사진 촬영 완료",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d(
                        TAG,
                        "사진 저장 위치: ${photoFile.absolutePath}"
                    )
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {
                    Log.e(TAG, "사진 촬영 실패", exception)
                    showMessage("사진을 촬영하지 못했습니다.")
                }
            }
        )
    }

    // 플래시 켜기 / 끄기
    private fun toggleFlash() {
        val currentCamera = camera

        if (currentCamera?.cameraInfo?.hasFlashUnit() != true) {
            Toast.makeText(
                requireContext(),
                "이 카메라는 플래시를 지원하지 않습니다.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        flashEnabled = !flashEnabled

        imageCapture?.flashMode =
            if (flashEnabled) {
                ImageCapture.FLASH_MODE_ON
            } else {
                ImageCapture.FLASH_MODE_OFF
            }

        flashButton?.setImageResource(
            if (flashEnabled) {
                R.drawable.ic_flash_on
            } else {
                R.drawable.ic_flash_off
            }
        )
    }

    // 전면 / 후면 카메라 전환
    private fun switchCamera() {
        val provider = cameraProvider ?: return

        val nextFacing =
            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }

        val nextSelector = CameraSelector.Builder()
            .requireLensFacing(nextFacing)
            .build()

        try {
            if (!provider.hasCamera(nextSelector)) {
                Toast.makeText(
                    requireContext(),
                    "전환할 카메라를 사용할 수 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            lensFacing = nextFacing
            bindCamera()

        } catch (e: Exception) {
            Log.e(TAG, "카메라 전환 실패", e)
            showMessage("카메라를 전환하지 못했습니다.")
        }
    }

    // 카메라 오류 메시지 표시
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
        flashButton = null
        camera = null
        imageCapture = null
        cameraProvider = null

        super.onDestroyView()
    }

    companion object {
        private const val TAG = "CameraFragment"
    }
}
