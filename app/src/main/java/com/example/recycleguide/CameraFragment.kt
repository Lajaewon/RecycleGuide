
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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

import com.example.recycleguide.network.RetrofitClient
import com.example.recycleguide.network.WasteUploadResponse

import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.io.File


class CameraFragment : Fragment(R.layout.fragment_camera) {

    // 카메라 화면의 뷰
    private var previewView: PreviewView? = null
    private var messageView: TextView? = null
    private var flashButton: ImageButton? = null

    // CameraX 객체
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null

    // 기본 카메라는 후면 카메라
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    // 플래시 상태
    private var flashEnabled = false


    // ==================================================
    // [추가] 스마트폰 시스템 하단바 숨기기
    // ==================================================

    private fun hideSystemNavigationBar() {

        val window = activity?.window ?: return

        val controller = WindowInsetsControllerCompat(
            window,
            window.decorView
        )

        // 화면 가장자리를 스와이프하면
        // 시스템 바를 일시적으로 표시하도록 설정
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // 시스템 내비게이션 바 숨기기
        controller.hide(
            WindowInsetsCompat.Type.navigationBars()
        )
    }


    // ==================================================
    // [추가] 스마트폰 시스템 하단바 다시 표시
    // ==================================================

    private fun showSystemNavigationBar() {

        val window = activity?.window ?: return

        val controller = WindowInsetsControllerCompat(
            window,
            window.decorView
        )

        // 시스템 내비게이션 바 표시
        controller.show(
            WindowInsetsCompat.Type.navigationBars()
        )
    }


    // ==================================================
    // 카메라 권한 요청
    // ==================================================

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


    // ==================================================
    // 앨범에서 이미지 선택
    // ==================================================

    private val galleryLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                context?.let {

                    Toast.makeText(
                        it,
                        "이미지를 선택했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()

                }

                // 선택한 이미지 전달 기능은 추후 구현
            }
        }


    // ==================================================
    // 화면이 생성된 후 실행
    // ==================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        // XML 뷰 연결
        previewView = view.findViewById(R.id.previewView)
        messageView = view.findViewById(R.id.tv_camera_message)
        flashButton = view.findViewById(R.id.btn_flash)


        // 카메라 화면에서는 앱 하단 메뉴 숨기기
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


        // 전면 / 후면 카메라 전환
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


    // ==================================================
    // [추가] 카메라 화면이 활성화될 때
    // ==================================================

    override fun onResume() {

        super.onResume()

        // 스마트폰 시스템 하단바 숨기기
        hideSystemNavigationBar()
    }


    // ==================================================
    // [추가] 카메라 화면이 일시적으로 중단될 때
    // ==================================================

    override fun onPause() {

        // 시스템 하단바 다시 표시
        showSystemNavigationBar()

        super.onPause()
    }


    // ==================================================
    // CameraX 초기화
    // ==================================================

    private fun startCamera() {

        val context = context ?: return

        // 카메라 공급자 요청
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            // 프래그먼트 화면이 이미 제거되었다면 중단
            val lifecycleOwner =
                viewLifecycleOwnerLiveData.value
                    ?: return@addListener

            if (!lifecycleOwner.lifecycle.currentState
                    .isAtLeast(Lifecycle.State.STARTED)
            ) {
                return@addListener
            }

            try {

                cameraProvider = cameraProviderFuture.get()

                // 카메라 연결 함수 실행
                bindCamera()

            } catch (e: Exception) {

                Log.e(TAG, "카메라 준비 실패", e)

                showMessage("카메라를 시작할 수 없습니다.")
            }

        }, ContextCompat.getMainExecutor(context))
    }


    // ==================================================
    // 미리보기와 사진 촬영 기능 연결
    // ==================================================

    private fun bindCamera() {

        val provider = cameraProvider ?: return
        val cameraPreview = previewView ?: return

        try {

            // 사용할 카메라 선택
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()


            if (!provider.hasCamera(selector)) {

                showMessage("선택한 카메라를 사용할 수 없습니다.")

                return
            }


            // 1. 실시간 영상 미리보기 설정
            val previewUseCase = Preview.Builder()
                .build()
                .also {

                    it.setSurfaceProvider(
                        ContextCompat.getMainExecutor(
                            cameraPreview.context
                        ),
                        cameraPreview.surfaceProvider
                    )
                }


            // 2. 사진 촬영 기능 설정
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


            // 기존 카메라 사용 사례 연결 해제
            provider.unbindAll()


            // 3. 카메라와 프래그먼트 생명주기 연결
            camera = provider.bindToLifecycle(
                viewLifecycleOwner,
                selector,
                previewUseCase,
                captureUseCase
            )


            // 생성한 촬영 기능 저장
            imageCapture = captureUseCase

            messageView?.visibility = View.GONE

        } catch (e: Exception) {

            Log.e(TAG, "카메라 연결 실패", e)

            showMessage("카메라를 연결하지 못했습니다.")
        }
    }


    // ==================================================
    // 사진 촬영 및 앱 캐시에 저장
    // ==================================================

    private fun takePhoto() {

        // ImageCapture 준비 여부 확인
        val capture = imageCapture ?: run {

            showMessage("카메라를 준비하고 있습니다.")

            return
        }


        val context = context ?: return


        // 저장할 사진 파일 생성
        val photoFile = File(
            context.cacheDir,
            "waste_${System.currentTimeMillis()}.jpg"
        )


        // 저장 위치 설정
        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(photoFile)
                .build()


        // 실제 사진 촬영
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),

            object : ImageCapture.OnImageSavedCallback {

                // 사진 촬영 및 저장 성공
                override fun onImageSaved(
                    outputFileResults: ImageCapture.OutputFileResults
                ) {

                    // 화면이 이미 제거되었다면 중단
                    if (!isAdded || view == null) return


                    // 촬영한 사진을 서버로 업로드
                    showMessage("사진을 서버에 업로드하고 있습니다.")

                    uploadPhoto(photoFile)
                }


                // 사진 촬영 실패
                override fun onError(
                    exception: ImageCaptureException
                ) {

                    Log.e(
                        TAG,
                        "사진 촬영 실패",
                        exception
                    )

                    showMessage("사진을 촬영하지 못했습니다.")
                }
            }
        )
    }


    // ==================================================
    // FastAPI 서버로 사진 업로드
    // ==================================================

    private fun uploadPhoto(photoFile: File) {

        // 1. 사진 파일을 HTTP 전송용 RequestBody로 변환
        val requestFile =
            photoFile.asRequestBody(
                "image/jpeg".toMediaType()
            )


        // 2. multipart/form-data의 file 항목 생성
        val photoPart =
            MultipartBody.Part.createFormData(
                "file",
                photoFile.name,
                requestFile
            )


        // 3. Retrofit을 이용해 서버에 업로드 요청
        RetrofitClient.api.uploadPhoto(photoPart)
            .enqueue(object : Callback<WasteUploadResponse> {


                // 서버로부터 응답을 받은 경우
                override fun onResponse(
                    call: Call<WasteUploadResponse>,
                    response: Response<WasteUploadResponse>
                ) {

                    // 화면이 이미 제거되었다면 중단
                    if (!isAdded || view == null) return


                    if (response.isSuccessful) {

                        val result = response.body()

                        if (result?.success == true) {

                            Log.d(
                                TAG,
                                "사진 업로드 성공: ${result.filename}"
                            )

                            Log.d(
                                TAG,
                                "파일 크기: ${result.sizeBytes} bytes"
                            )


                            Toast.makeText(
                                requireContext(),
                                "서버에 사진 업로드 성공!",
                                Toast.LENGTH_SHORT
                            ).show()


                            // 업로드 성공 후 결과 화면으로 이동
                            navigateToResult(photoFile)

                        } else {

                            Log.e(
                                TAG,
                                "서버 응답 실패: ${result?.message}"
                            )

                            showMessage(
                                result?.message
                                    ?: "사진 업로드에 실패했습니다."
                            )
                        }

                    } else {

                        val errorMessage =
                            response.errorBody()?.string()

                        Log.e(
                            TAG,
                            "HTTP 오류: ${response.code()}, $errorMessage"
                        )

                        showMessage(
                            "사진 업로드 실패 (HTTP ${response.code()})"
                        )
                    }
                }


                // 서버 연결 자체에 실패한 경우
                override fun onFailure(
                    call: Call<WasteUploadResponse>,
                    t: Throwable
                ) {

                    if (!isAdded || view == null) return


                    Log.e(
                        TAG,
                        "서버 통신 실패",
                        t
                    )


                    showMessage(
                        "서버에 연결할 수 없습니다."
                    )


                    Toast.makeText(
                        requireContext(),
                        "서버 연결을 확인해 주세요.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }


    // ==================================================
    // 사진 업로드 성공 후 결과 화면으로 이동
    // ==================================================

    private fun navigateToResult(photoFile: File) {

        if (!isAdded || view == null) return


        // 결과 프래그먼트 생성 및 사진 경로 전달
        val resultFragment =
            WasteResultFragment.newInstance(
                photoFile.absolutePath
            )


        // 카메라 화면을 결과 화면으로 교체
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                resultFragment
            )
            .addToBackStack("waste_result")
            .commit()
    }


    // ==================================================
    // 플래시 켜기 / 끄기
    // ==================================================

    private fun toggleFlash() {

        val currentCamera = camera


        // 카메라에 플래시가 있는지 확인
        if (currentCamera?.cameraInfo?.hasFlashUnit() != true) {

            Toast.makeText(
                requireContext(),
                "이 카메라는 플래시를 지원하지 않습니다.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // 플래시 상태 반전
        flashEnabled = !flashEnabled


        // 촬영 시 사용할 플래시 모드 변경
        imageCapture?.flashMode =
            if (flashEnabled) {
                ImageCapture.FLASH_MODE_ON
            } else {
                ImageCapture.FLASH_MODE_OFF
            }


        // 플래시 아이콘 변경
        flashButton?.setImageResource(
            if (flashEnabled) {
                R.drawable.ic_flash_on
            } else {
                R.drawable.ic_flash_off
            }
        )
    }


    // ==================================================
    // 전면 / 후면 카메라 전환
    // ==================================================

    private fun switchCamera() {

        val provider = cameraProvider ?: return


        // 현재 카메라 반대 방향 선택
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

            // 전환할 카메라가 존재하는지 확인
            if (!provider.hasCamera(nextSelector)) {

                Toast.makeText(
                    requireContext(),
                    "전환할 카메라를 사용할 수 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }


            // 사용할 카메라 변경
            lensFacing = nextFacing


            // 변경된 카메라로 다시 연결
            bindCamera()

        } catch (e: Exception) {

            Log.e(TAG, "카메라 전환 실패", e)

            showMessage("카메라를 전환하지 못했습니다.")
        }
    }


    // ==================================================
    // 카메라 오류 메시지 표시
    // ==================================================

    private fun showMessage(message: String) {

        messageView?.apply {

            text = message

            visibility = View.VISIBLE
        }
    }


    // ==================================================
    // 프래그먼트 화면이 제거될 때
    // ==================================================

    override fun onDestroyView() {

        // 시스템 하단바 다시 표시
        showSystemNavigationBar()


        // 기존 하단 메뉴 다시 표시
        activity
            ?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            ?.visibility = View.VISIBLE


        // 뷰 참조 정리
        previewView = null
        messageView = null
        flashButton = null


        // CameraX 참조 정리
        camera = null
        imageCapture = null
        cameraProvider = null


        super.onDestroyView()
    }


    // ==================================================
    // 로그 태그
    // ==================================================

    companion object {
        private const val TAG = "CameraFragment"
    }
}
