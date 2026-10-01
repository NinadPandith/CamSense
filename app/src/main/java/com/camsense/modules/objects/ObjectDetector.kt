package com.camsense.modules.objects

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import com.camsense.scoring.ScanConfig
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Runs the YOLO model (the .tflite file in assets/models) with LiteRT. Works with float and
 * INT8 exports, and with end-to-end (YOLO26) or raw (YOLO11) outputs. Loaded once
 * and shared; not thread-safe, so every call is synchronized.
 */
class ObjectDetector(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var post: YoloPostProcessor? = null
    private var labels: List<String> = emptyList()
    private var inputSize = 640
    private var inputType = DataType.FLOAT32
    private var inputScale = 1f
    private var inputZero = 0
    private var input: ByteBuffer? = null
    private var pixels = IntArray(0)
    private var loadFailed = false

    val isLoaded: Boolean get() = interpreter != null

    /** Loads the model once; false (and stays false) when it is missing or broken. */
    @Synchronized
    fun ensureLoaded(): Boolean {
        if (interpreter != null) return true
        if (loadFailed) return false
        return try {
            labels = context.assets.open(ScanConfig.LABELS_ASSET).bufferedReader().readLines()
                .map { it.trim() }.filter { it.isNotEmpty() }
            val model = context.assets.openFd(ScanConfig.MODEL_ASSET).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { ch ->
                    ch.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
            val interp = Interpreter(model, Interpreter.Options().setNumThreads(4))
            val inTensor = interp.getInputTensor(0)
            inputSize = inTensor.shape()[1]
            inputType = inTensor.dataType()
            inputScale = inTensor.quantizationParams().scale.takeIf { it > 0f } ?: 1f
            inputZero = inTensor.quantizationParams().zeroPoint
            val bytesPerChannel = if (inputType == DataType.FLOAT32) 4 else 1
            input = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * bytesPerChannel).order(ByteOrder.nativeOrder())
            pixels = IntArray(inputSize * inputSize)
            post = YoloPostProcessor(labels, ScanConfig.OBJECT_MIN_CONFIDENCE, ScanConfig.OBJECT_NMS_IOU,
                ScanConfig.OBJECT_MAX_DETECTIONS)
            interpreter = interp
            Log.i(TAG, "Model loaded: input $inputSize px $inputType, output ${interp.getOutputTensor(0).shape().contentToString()}, ${labels.size} labels")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Model failed to load", e)
            loadFailed = true
            false
        }
    }

    /** Detections in 0–1 fractions of [bitmap], which must be upright. */
    @Synchronized
    fun detect(bitmap: Bitmap): List<Detection> {
        val interp = interpreter ?: return emptyList()
        val lb = Letterbox.compute(bitmap.width, bitmap.height, inputSize)
        fillInput(bitmap, lb)

        val out = interp.getOutputTensor(0)
        val shape = out.shape()
        val count = shape.fold(1) { a, b -> a * b }
        val outType = out.dataType()
        val outBuf = ByteBuffer.allocateDirect(count * if (outType == DataType.FLOAT32) 4 else 1)
            .order(ByteOrder.nativeOrder())
        interp.run(input!!.apply { rewind() }, outBuf)
        outBuf.rewind()

        val values = FloatArray(count)
        if (outType == DataType.FLOAT32) {
            outBuf.asFloatBuffer().get(values)
        } else {
            val q = out.quantizationParams()
            for (i in 0 until count) {
                val raw = if (outType == DataType.UINT8) outBuf.get().toInt() and 0xFF else outBuf.get().toInt()
                values[i] = (raw - q.zeroPoint) * q.scale
            }
        }
        return post!!.process(values, shape, lb)
    }

    private fun fillInput(bitmap: Bitmap, lb: Letterbox) {
        val canvasBmp = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        Canvas(canvasBmp).apply {
            drawColor(Color.rgb(114, 114, 114))
            val m = Matrix().apply { postScale(lb.scale, lb.scale); postTranslate(lb.padX, lb.padY) }
            drawBitmap(bitmap, m, Paint(Paint.FILTER_BITMAP_FLAG))
        }
        canvasBmp.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        canvasBmp.recycle()
        val buf = input!!.apply { rewind() }
        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            when (inputType) {
                DataType.FLOAT32 -> { buf.putFloat(r / 255f); buf.putFloat(g / 255f); buf.putFloat(b / 255f) }
                DataType.UINT8 -> { buf.put(r.toByte()); buf.put(g.toByte()); buf.put(b.toByte()) }
                else -> { buf.put(quant(r)); buf.put(quant(g)); buf.put(quant(b)) }
            }
        }
    }

    private fun quant(channel: Int): Byte =
        ((channel / 255f) / inputScale + inputZero).toInt().coerceIn(-128, 127).toByte()

    @Synchronized
    fun close() {
        interpreter?.close()
        interpreter = null
    }

    private companion object { const val TAG = "CamSense/Objects" }
}
