plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.recycleguide"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.recycleguide"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    val cameraXVersion = "1.6.2"

    // 카메라의 핵심 기능
    implementation("androidx.camera:camera-core:$cameraXVersion")

    // Android 카메라 하드웨어 연결
    implementation("androidx.camera:camera-camera2:$cameraXVersion")

    // 카메라와 앱의 생명주기 연결
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")

    // 카메라 영상 미리보기
    implementation("androidx.camera:camera-view:$cameraXVersion")

    implementation("androidx.exifinterface:exifinterface:1.4.2")
    // Retrofit: HTTP 통신 및 API 요청
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

// Gson: 서버에서 반환하는 JSON 응답 변환
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.android.gms:play-services-location:21.4.0")

}