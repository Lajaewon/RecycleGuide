package com.example.recycleguide

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import java.io.File
import java.io.IOException

class WasteResultFragment : Fragment() {

    private var photoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        photoPath = arguments?.getString(ARG_PHOTO_PATH)
    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val photoImageView =
            view.findViewById<ImageView>(R.id.iv_waste_photo)

        displayPhoto(photoImageView)
    }

    // 촬영한 사진을 읽고 회전 정보를 적용하는 함수
    private fun displayPhoto(imageView: ImageView) {

        val path = photoPath ?: return
        val photoFile = File(path)

        if (!photoFile.exists()) return

        // 이미지 파일 읽기
        val originalBitmap =
            BitmapFactory.decodeFile(photoFile.absolutePath) ?: return

        // EXIF에 저장된 회전 정보 읽기
        val rotationDegrees = try {
            ExifInterface(photoFile).rotationDegrees
        } catch (e: IOException) {
            0
        }

        // 회전 정보가 있다면 실제 이미지에 적용
        val correctedBitmap: Bitmap

        if (rotationDegrees != 0) {

            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
            }

            correctedBitmap = Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.width,
                originalBitmap.height,
                matrix,
                true
            )

        } else {
            correctedBitmap = originalBitmap
        }

        // 사진 전체가 보이도록 설정
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        // 최종 이미지 표시
        imageView.setImageBitmap(correctedBitmap)
    }

    companion object {

        private const val ARG_PHOTO_PATH = "photo_path"

        fun newInstance(photoPath: String): WasteResultFragment {

            return WasteResultFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHOTO_PATH, photoPath)
                }
            }
        }
    }
}