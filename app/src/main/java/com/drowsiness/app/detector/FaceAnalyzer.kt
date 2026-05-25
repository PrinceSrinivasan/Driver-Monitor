package com.drowsiness.app.detector

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceAnalyzer(
    private val onResult: (ear: Float, status: String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(options)

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    processFace(faces[0])
                } else {
                    onResult(0f, "WAITING")
                }
            }
            .addOnFailureListener {
                onResult(0f, "WAITING")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun processFace(face: Face) {
        val leftEye = face.getContour(FaceContour.LEFT_EYE)?.points ?: emptyList()
        val rightEye = face.getContour(FaceContour.RIGHT_EYE)?.points ?: emptyList()

        val leftEAR = if (leftEye.size >= 16) DrowsinessDetector.calculateEAR(leftEye) else 0f
        val rightEAR = if (rightEye.size >= 16) DrowsinessDetector.calculateEAR(rightEye) else 0f

        val ear = when {
            leftEAR > 0f && rightEAR > 0f -> (leftEAR + rightEAR) / 2f
            leftEAR > 0f -> leftEAR
            rightEAR > 0f -> rightEAR
            else -> 0f
        }

        onResult(ear, DrowsinessDetector.getStatus(ear))
    }
}
