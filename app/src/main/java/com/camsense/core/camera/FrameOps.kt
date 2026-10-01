package com.camsense.core.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.camsense.modules.glint.Blob
import com.camsense.scoring.ScanConfig
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

/** OpenCV helpers shared by the camera modules (Tech Stack "Frame pipeline"). */
object FrameOps {

    /**
     * The Y (brightness) plane as an upright grayscale Mat. The caller must release
     * it, and must not close the ImageProxy before this returns.
     */
    fun grayUpright(image: ImageProxy): Mat {
        val plane = image.planes[0]
        val buffer = plane.buffer.apply { rewind() }
        val wrapped = Mat(image.height, image.width, CvType.CV_8UC1, buffer, plane.rowStride.toLong())
        val out = Mat()
        when (image.imageInfo.rotationDegrees) {
            90 -> Core.rotate(wrapped, out, Core.ROTATE_90_CLOCKWISE)
            180 -> Core.rotate(wrapped, out, Core.ROTATE_180)
            270 -> Core.rotate(wrapped, out, Core.ROTATE_90_COUNTERCLOCKWISE)
            else -> wrapped.copyTo(out)
        }
        wrapped.release()
        return out
    }

    /** The frame as an upright Bitmap, for the AI model and for snapshots. */
    fun bitmapUpright(image: ImageProxy): Bitmap {
        val bmp = image.toBitmap()
        val rotation = image.imageInfo.rotationDegrees
        if (rotation == 0) return bmp
        val m = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    /**
     * 4πA/P², corrected for small blobs. findContours runs through pixel centres, so
     * a 7 px disc measures 0.67 and would fail a 0.7 test; adding back the half-pixel
     * border (A + P/2 + 1, and P + π) gives 0.72–0.83 for discs of any size and about
     * 0.4 for streaks. Same formula as prototype/glint_core.py.
     */
    fun pixelCircularity(contourArea: Double, perimeter: Double): Double =
        if (perimeter <= 0) 1.0 else 4 * PI * (contourArea + perimeter / 2 + 1) / ((perimeter + PI) * (perimeter + PI))

    fun meanLuma(gray: Mat): Double = Core.mean(gray).`val`[0]

    /**
     * Small round spots that are much brighter than their surroundings (PRD FR-1.2).
     *
     * Uses local contrast (the pixel minus the mean of a [SpotParams.windowPx] box
     * around it) rather than a fixed brightness cut-off. With the torch on, auto
     * exposure darkens the frame, so a lens reflection may only reach ~220 while still
     * standing far above its background; large bright areas (glass, walls, lamps) have
     * almost no local contrast and drop out. Candidates are then filtered by size,
     * shape and roundness, and the strongest [SpotParams.maxSpots] are kept, so a
     * busy frame never hides the lens.
     */
    fun brightSpots(gray: Mat, p: SpotParams): SpotResult {
        val background = Mat()
        val contrast = Mat()
        val contrastMask = Mat()
        val lumaMask = Mat()
        val mask = Mat()
        val hierarchy = Mat()
        val contours = ArrayList<MatOfPoint>()
        try {
            Imgproc.blur(gray, background, Size(p.windowPx.toDouble(), p.windowPx.toDouble()))
            Core.subtract(gray, background, contrast) // 8-bit: saturates at 0
            Imgproc.threshold(contrast, contrastMask, p.minContrast, 255.0, Imgproc.THRESH_BINARY)
            Imgproc.threshold(gray, lumaMask, p.minLuma, 255.0, Imgproc.THRESH_BINARY)
            Core.bitwise_and(contrastMask, lumaMask, mask)
            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE)
            val w = gray.cols().toFloat()
            val h = gray.rows().toFloat()
            val spots = ArrayList<Blob>()
            for (c in contours) {
                val rect = Imgproc.boundingRect(c)
                val diameter = max(rect.width, rect.height).toFloat()
                if (diameter < p.minDiameter || diameter > p.maxDiameter) continue
                if (min(rect.width, rect.height).toFloat() / max(rect.width, rect.height) < p.minAspect) {
                    continue // streaks and smears, not a point
                }
                val area = max(Imgproc.contourArea(c), 1.0)
                if (area >= p.circularityMinArea) {
                    val c2f = MatOfPoint2f(*c.toArray())
                    val perimeter = Imgproc.arcLength(c2f, true)
                    c2f.release()
                    if (pixelCircularity(area, perimeter) < p.minCircularity) continue
                }
                val roi = contrast.submat(rect)
                val strength = Core.minMaxLoc(roi).maxVal.toFloat()
                roi.release()
                spots += Blob(
                    x = (rect.x + rect.width / 2f) / w,
                    y = (rect.y + rect.height / 2f) / h,
                    diameterPx = diameter,
                    strength = strength,
                )
            }
            return SpotResult(spots.sortedByDescending { it.strength }.take(p.maxSpots), contours.size)
        } finally {
            contours.forEach { it.release() }
            listOf(background, contrast, contrastMask, lumaMask, mask, hierarchy).forEach { it.release() }
        }
    }

    /** Brightest pixel within [radiusPx] of a point given in 0–1 frame fractions. */
    fun peakAround(gray: Mat, x: Float, y: Float, radiusPx: Int): Double {
        val cx = (x * gray.cols()).toInt()
        val cy = (y * gray.rows()).toInt()
        val left = (cx - radiusPx).coerceIn(0, gray.cols() - 1)
        val top = (cy - radiusPx).coerceIn(0, gray.rows() - 1)
        val right = (cx + radiusPx).coerceIn(left + 1, gray.cols())
        val bottom = (cy + radiusPx).coerceIn(top + 1, gray.rows())
        val roi = gray.submat(top, bottom, left, right)
        return try { Core.minMaxLoc(roi).maxVal } finally { roi.release() }
    }
}

/** Mean frame-to-frame change of a tiny copy of the frame: high means "moving too fast". */
class MotionMeter(private val size: Int) {
    private var previous: Mat? = null

    fun update(gray: Mat): Double {
        val small = Mat()
        val h = (size * gray.rows().toDouble() / gray.cols()).coerceAtLeast(1.0)
        Imgproc.resize(gray, small, Size(size.toDouble(), h), 0.0, 0.0, Imgproc.INTER_AREA)
        val prev = previous
        previous = small
        if (prev == null || prev.size() != small.size()) {
            prev?.release()
            return 0.0
        }
        val diff = Mat()
        Core.absdiff(small, prev, diff)
        val m = Core.mean(diff).`val`[0]
        diff.release()
        prev.release()
        return m
    }

    fun reset() {
        previous?.release()
        previous = null
    }
}
