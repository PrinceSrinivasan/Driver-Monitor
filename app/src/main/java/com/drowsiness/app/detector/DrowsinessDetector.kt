package com.drowsiness.app.detector

import android.graphics.PointF
import kotlin.math.sqrt

object DrowsinessDetector {

    const val EAR_THRESHOLD = 0.25f

    /**
     * Calculate Eye Aspect Ratio from 16-point eye contour (ML Kit).
     * Contour points go clockwise: 0=left corner, 8=right corner,
     * top points at ~12-15, bottom points at ~3-5.
     */
    fun calculateEAR(eye: List<PointF>): Float {
        if (eye.size < 16) return 0.3f
        val v1 = dist(eye[3], eye[13])
        val v2 = dist(eye[5], eye[11])
        val h = dist(eye[0], eye[8])
        if (h < 0.001f) return 0.3f
        return (v1 + v2) / (2f * h)
    }

    fun getStatus(ear: Float): String = when {
        ear <= 0f -> "WAITING"
        ear < EAR_THRESHOLD -> "DROWSY"
        else -> "AWAKE"
    }

    private fun dist(a: PointF, b: PointF): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}
