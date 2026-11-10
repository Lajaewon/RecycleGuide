
package com.example.recycleguide

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

import java.io.IOException
import java.util.Locale


class HomeFragment : Fragment() {

    // ==================================================
    // 1. 위치 관련 변수
    // ==================================================

    // 스마트폰 위치 정보를 가져오는 객체
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // XML의 현재 위치 TextView
    private var locationNameView: TextView? = null

    // 위치 요청 취소 토큰
    private var locationCancellationToken: CancellationTokenSource? = null


    // ==================================================
    // 2. 위치 권한 요청
    // ==================================================

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            // 정확한 위치 권한 확인
            val fineGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            // 대략적인 위치 권한 확인
            val coarseGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true


            if (fineGranted || coarseGranted) {

                // 위치 권한이 허용되면 현재 위치 가져오기
                getCurrentLocation()

            } else {

                // 위치 권한이 거부된 경우
                setLocationName("위치 권한을 허용해 주세요.")
            }
        }


    // ==================================================
    // 3. 메인 화면 XML 불러오기
    // ==================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_home,
            container,
            false
        )
    }


    // ==================================================
    // 4. 화면 생성 후 실행
    // ==================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)


        // 사진 촬영 버튼 연결
        val btnCamera =
            view.findViewById<MaterialButton>(R.id.btn_camera)


        // 위치 카드 연결
        val cardLocation =
            view.findViewById<MaterialCardView>(R.id.card_location)


        // 위치 이름 TextView 연결
        locationNameView =
            view.findViewById(R.id.tv_location_name)


        // 위치 서비스를 제공하는 객체 초기화
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(
                requireActivity()
            )


        // 위치 확인 중이라는 문구 표시
        setLocationName("현재 위치 확인 중...")


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


        // 위치 권한 확인 및 현재 위치 조회
        checkLocationPermission()
    }


    // ==================================================
    // 5. 현재 위치 권한 확인
    // ==================================================

    private fun checkLocationPermission() {

        // 정확한 위치 권한 확인
        val fineGranted =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        // 대략적인 위치 권한 확인
        val coarseGranted =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        if (fineGranted || coarseGranted) {

            // 이미 권한이 있다면 위치 조회
            getCurrentLocation()

        } else {

            // 권한이 없다면 사용자에게 요청
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }


    // ==================================================
    // 6. 스마트폰 현재 위치 가져오기
    // ==================================================

    private fun getCurrentLocation() {

        // 정확한 위치 권한 확인
        val fineGranted =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        // 권한에 따라 위치 정확도 설정
        val priority =
            if (fineGranted) {

                Priority.PRIORITY_HIGH_ACCURACY

            } else {

                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }


        try {

            // 이전 요청이 있다면 취소
            locationCancellationToken?.cancel()


            // 새로운 위치 요청 토큰 생성
            val cancellationTokenSource =
                CancellationTokenSource()

            locationCancellationToken =
                cancellationTokenSource


            // 현재 위치 요청
            fusedLocationClient.getCurrentLocation(
                priority,
                cancellationTokenSource.token

            ).addOnSuccessListener { location ->

                // 화면이 이미 제거되었다면 중단
                if (!isAdded || locationNameView == null) {
                    return@addOnSuccessListener
                }


                if (location != null) {

                    // 위도와 경도 가져오기
                    val latitude = location.latitude
                    val longitude = location.longitude


                    Log.d(
                        TAG,
                        "위도: $latitude, 경도: $longitude"
                    )


                    // 좌표를 주소로 변환
                    convertCoordinatesToAddress(
                        latitude,
                        longitude
                    )

                } else {

                    // 현재 위치를 가져오지 못하면
                    // 마지막으로 저장된 위치 확인
                    getLastKnownLocation()
                }

            }.addOnFailureListener { exception ->

                Log.e(
                    TAG,
                    "현재 위치 가져오기 실패",
                    exception
                )

                getLastKnownLocation()
            }

        } catch (e: SecurityException) {

            Log.e(
                TAG,
                "위치 권한 오류",
                e
            )

            setLocationName("위치 정보를 가져올 수 없습니다.")
        }
    }


    // ==================================================
    // 7. 마지막으로 저장된 위치 가져오기
    // ==================================================

    private fun getLastKnownLocation() {

        // 현재 위치 권한 확인
        val fineGranted =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        if (!fineGranted && !coarseGranted) {

            setLocationName("위치 권한을 허용해 주세요.")

            return
        }


        try {

            fusedLocationClient.lastLocation

                .addOnSuccessListener { location ->

                    if (!isAdded || locationNameView == null) {
                        return@addOnSuccessListener
                    }


                    if (location != null) {

                        // 마지막 위치가 5분 이내인지 확인
                        val locationAge =
                            System.currentTimeMillis() - location.time

                        val isRecent =
                            locationAge in 0..300_000L


                        if (isRecent) {

                            convertCoordinatesToAddress(
                                location.latitude,
                                location.longitude
                            )

                        } else {

                            setLocationName(
                                "현재 위치를 확인할 수 없습니다."
                            )
                        }

                    } else {

                        setLocationName(
                            "현재 위치를 확인할 수 없습니다."
                        )
                    }
                }

                .addOnFailureListener { exception ->

                    Log.e(
                        TAG,
                        "마지막 위치 확인 실패",
                        exception
                    )

                    setLocationName(
                        "현재 위치를 확인할 수 없습니다."
                    )
                }

        } catch (e: SecurityException) {

            Log.e(TAG, "위치 접근 권한 오류", e)

            setLocationName(
                "위치 정보를 가져올 수 없습니다."
            )
        }
    }


    // ==================================================
    // 8. 위도와 경도를 주소로 변환
    // ==================================================

    private fun convertCoordinatesToAddress(
        latitude: Double,
        longitude: Double
    ) {

        val context = context ?: return

        // 한국어 주소를 반환하는 Geocoder 생성
        val geocoder = Geocoder(
            context,
            Locale.KOREA
        )


        // 주소 변환 서비스 사용 가능 여부 확인
        if (!Geocoder.isPresent()) {

            setLocationName(
                "주소 서비스를 사용할 수 없습니다."
            )

            return
        }


        // Android 13(API 33) 이상
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            try {

                // 비동기 방식으로 주소 변환
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,

                    object : Geocoder.GeocodeListener {

                        // 주소 변환 성공
                        override fun onGeocode(
                            addresses: MutableList<Address>
                        ) {

                            val address =
                                addresses.firstOrNull()


                            val locationName =
                                address?.let {
                                    formatLocationAddress(it)
                                } ?: "주소를 확인할 수 없습니다."


                            updateLocationName(locationName)
                        }


                        // 주소 변환 실패
                        override fun onError(errorMessage: String?) {

                            Log.e(
                                TAG,
                                "주소 변환 실패: $errorMessage"
                            )

                            updateLocationName(
                                "주소를 확인할 수 없습니다."
                            )
                        }
                    }
                )

            } catch (e: IOException) {

                Log.e(TAG, "Geocoder 오류", e)

                setLocationName("주소를 확인할 수 없습니다.")

            } catch (e: IllegalArgumentException) {

                Log.e(TAG, "잘못된 좌표", e)

                setLocationName("주소를 확인할 수 없습니다.")
            }

        } else {

            // Android 12 이하
            // 동기식 주소 변환은 별도 스레드에서 실행
            Thread {

                val locationName = try {

                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1
                    )?.firstOrNull()?.let {

                        formatLocationAddress(it)
                    }

                } catch (e: IOException) {

                    Log.e(TAG, "주소 변환 오류", e)

                    null

                } catch (e: IllegalArgumentException) {

                    Log.e(TAG, "잘못된 좌표", e)

                    null
                }


                updateLocationName(
                    locationName ?: "주소를 확인할 수 없습니다."
                )

            }.start()
        }
    }


    // ==================================================
    // 9. 행정구역 주소 정리
    // ==================================================

    private fun formatLocationAddress(
        address: Address
    ): String {

        // 시·도 정보
        val province =
            address.adminArea
                ?.trim()
                ?.takeIf { it.isNotEmpty() }


        // 시·군·구 정보
        // 한국 주소 특성을 고려해 우선순위 설정
        val district =
            sequenceOf(
                address.subAdminArea,
                address.locality,
                address.subLocality
            ).firstOrNull {

                !it.isNullOrBlank() &&
                        it.trim() != province
            }?.trim()


        // 시·도 + 시·군·구 조합
        val result =
            listOfNotNull(
                province,
                district
            ).distinct().joinToString(" ")


        if (result.isNotBlank()) {

            return result
        }


        // 행정구역 정보가 부족한 경우
        // 전체 주소에서 앞부분 두 단어 추출
        val fullAddress =
            address.getAddressLine(0)
                ?.removePrefix("대한민국")
                ?.trim()


        return fullAddress
            ?.split(Regex("\\s+"))
            ?.take(2)
            ?.joinToString(" ")
            ?.takeIf { it.isNotBlank() }
            ?: "주소를 확인할 수 없습니다."
    }


    // ==================================================
    // 10. 위치 카드에 주소 표시
    // ==================================================

    private fun setLocationName(
        locationName: String
    ) {

        locationNameView?.text = locationName
    }


    // ==================================================
    // 11. 비동기 작업 결과를 UI에 반영
    // ==================================================

    private fun updateLocationName(
        locationName: String
    ) {

        activity?.runOnUiThread {

            // 화면이 제거되었는지 확인
            if (!isAdded || locationNameView == null) {
                return@runOnUiThread
            }

            setLocationName(locationName)
        }
    }


    // ==================================================
    // 12. 화면이 제거될 때 리소스 정리
    // ==================================================

    override fun onDestroyView() {

        // 진행 중인 위치 요청 취소
        locationCancellationToken?.cancel()
        locationCancellationToken = null

        // XML 뷰 참조 정리
        locationNameView = null

        super.onDestroyView()
    }


    companion object {

        private const val TAG = "HomeFragment"
    }
}
