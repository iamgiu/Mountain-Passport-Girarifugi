package com.example.mountainpassport_girarifugi.ui.profile.settings

import android.app.Activity
import android.content.Context
import android.util.Base64
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.mountainpassport_girarifugi.R
import com.google.android.material.button.MaterialButton
import java.io.ByteArrayOutputStream

class ImageCropActivity : AppCompatActivity() {

    private lateinit var cropImageView: ImageView
    private lateinit var overlayView: CropOverlayView
    private lateinit var cancelButton: MaterialButton
    private lateinit var chooseButton: MaterialButton

    private var imageMatrix = Matrix()
    private var bitmap: Bitmap? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var scaleFactor = 1f

    companion object {
        const val EXTRA_IMAGE_URI = "image_uri"
        const val EXTRA_CROPPED_IMAGE = "cropped_image"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_crop)

        initViews()
        setupGestureDetector()
        loadImage()
        setupClickListeners()
    }

    private fun initViews() {
        cropImageView = findViewById(R.id.cropImageView)
        cancelButton = findViewById(R.id.cancelButton)
        chooseButton = findViewById(R.id.chooseButton)

        val imageContainer = findViewById<android.widget.FrameLayout>(R.id.imageContainer)
        val overlayViewFromLayout = findViewById<android.view.View>(R.id.overlayView)
        imageContainer.removeView(overlayViewFromLayout)

        overlayView = CropOverlayView(this)
        imageContainer.addView(overlayView)
    }

    // Zoom immagine, limitato tra 1/2x e 3x
    private fun setupGestureDetector() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f) // Limit zoom

                imageMatrix.setScale(scaleFactor, scaleFactor)
                cropImageView.imageMatrix = imageMatrix
                return true
            }
        })
    }

    // Riceve uri immagine
    private fun loadImage() {
        val imageUri = intent.getParcelableExtra<Uri>(EXTRA_IMAGE_URI)
        imageUri?.let { uri ->
            try {
                val inputStream = contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                cropImageView.setImageBitmap(bitmap)
                cropImageView.scaleType = ImageView.ScaleType.MATRIX

                // Center the image initially
                centerImage()
            } catch (e: Exception) {
                e.printStackTrace()
                finish()
            }
        }
    }

    private fun centerImage() {
        bitmap?.let { bmp ->
            val viewWidth = cropImageView.width.toFloat()
            val viewHeight = cropImageView.height.toFloat()
            val bmpWidth = bmp.width.toFloat()
            val bmpHeight = bmp.height.toFloat()

            if (viewWidth > 0 && viewHeight > 0) {
                val scale = maxOf(viewWidth / bmpWidth, viewHeight / bmpHeight)
                scaleFactor = scale

                val dx = (viewWidth - bmpWidth * scale) * 0.5f
                val dy = (viewHeight - bmpHeight * scale) * 0.5f

                imageMatrix.setScale(scale, scale)
                imageMatrix.postTranslate(dx, dy)
                cropImageView.imageMatrix = imageMatrix
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector?.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector!!.isInProgress) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    imageMatrix.postTranslate(dx, dy)
                    cropImageView.imageMatrix = imageMatrix

                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }
        }
        return true
    }

    private fun setupClickListeners() {
        cancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        chooseButton.setOnClickListener {
            val croppedBitmap = cropImage()
            if (croppedBitmap != null) {
                // Salva l'immagine croppata temporaneamente
                val success = saveCroppedImageTemporary(croppedBitmap)
                if (success) {
                    setResult(Activity.RESULT_OK)
                } else {
                    setResult(Activity.RESULT_CANCELED)
                }
            } else {
                setResult(Activity.RESULT_CANCELED)
            }
            finish()
        }
    }

    // Salva immagine tagliata e ne riduce la qualità per alleggerirla
    private fun saveCroppedImageTemporary(bitmap: Bitmap): Boolean {
        return try {
            val sharedPreferences = getSharedPreferences("crop_temp", Context.MODE_PRIVATE)

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()
            val base64String = Base64.encodeToString(imageBytes, Base64.DEFAULT)

            sharedPreferences.edit()
                .putString("cropped_image", base64String)
                .apply()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Taglia immagine
    private fun cropImage(): Bitmap? {
        return try {
            val originalBitmap = bitmap ?: return null
            val circleBounds = overlayView.getCircleBounds()
            val size = (circleBounds.width()).toInt()
            val croppedBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(croppedBitmap)
            val imageView = cropImageView
            val matrix = Matrix(imageMatrix)
            val inverse = Matrix()
            matrix.invert(inverse)
            val srcRect = RectF(circleBounds)
            inverse.mapRect(srcRect)
            val srcRectF = Rect(
                srcRect.left.toInt().coerceAtLeast(0),
                srcRect.top.toInt().coerceAtLeast(0),
                srcRect.right.toInt().coerceAtMost(originalBitmap.width),
                srcRect.bottom.toInt().coerceAtMost(originalBitmap.height)
            )

            val destRect = Rect(0, 0, size, size)
            canvas.drawBitmap(originalBitmap, srcRectF, destRect, null)

            croppedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && cropImageView.width > 0) {
            centerImage()
        }
    }
}