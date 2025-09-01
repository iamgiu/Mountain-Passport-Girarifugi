package com.example.mountainpassport_girarifugi.ui.profile.settings

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val overlayPaint = Paint().apply {
        color = Color.BLACK
        alpha = 180  // Overlay semi trasparente
        style = Paint.Style.FILL
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private var circleRadius = 0f
    private var circleCenterX = 0f
    private var circleCenterY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // parametri cerchio
        val minDimension = minOf(w, h)
        circleRadius = (minDimension * 0.4f) // 40%
        circleCenterX = w / 2f
        circleCenterY = h / 2f
    }

    override fun onDraw(canvas: Canvas) {   // Overlay scuro con cerchio trasparente grande quanto immagine profilo
        super.onDraw(canvas)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val overlayCanvas = Canvas(bitmap)

        overlayCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        overlayCanvas.drawCircle(circleCenterX, circleCenterY, circleRadius, clearPaint)

        canvas.drawBitmap(bitmap, 0f, 0f, null)

        bitmap.recycle()
    }

    fun getCircleBounds(): RectF {
        return RectF(
            circleCenterX - circleRadius,
            circleCenterY - circleRadius,
            circleCenterX + circleRadius,
            circleCenterY + circleRadius
        )
    }
}