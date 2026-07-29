package com.example.ffsensitivity

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class CrosshairOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: CustomCrosshairView
    private lateinit var params: WindowManager.LayoutParams

    private var sizeDp = 24
    private var opacityAlpha = 255
    private var colorHex = "#66FCF1"
    private var shapeType = "DOT"
    private var isEditMode = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
        setupOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            sizeDp = it.getIntExtra(EXTRA_SIZE, sizeDp)
            opacityAlpha = it.getIntExtra(EXTRA_ALPHA, opacityAlpha)
            colorHex = it.getStringExtra(EXTRA_COLOR) ?: colorHex
            shapeType = it.getStringExtra(EXTRA_SHAPE) ?: shapeType
            isEditMode = it.getBooleanExtra(EXTRA_EDIT_MODE, isEditMode)

            updateWindowFlags()
            overlayView.updateProperties(sizeDp, opacityAlpha, colorHex, shapeType)
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, FFAssistantApp.CHANNEL_ID)
            .setContentTitle("FF Assistant Crosshair Active")
            .setContentText("Overlay active - Google Play compliant independent service")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(FFAssistantApp.NOTIFICATION_ID, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayView() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }

        overlayView = CustomCrosshairView(this)
        
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        overlayView.setOnTouchListener { _, event ->
            if (!isEditMode) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(overlayView, params)
    }

    private fun updateWindowFlags() {
        if (!::params.isInitialized) return
        if (isEditMode) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        windowManager.updateViewLayout(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    private class CustomCrosshairView(context: Context) : View(context) {
        private var radiusPx = 24f
        private var paintColor = Color.parseColor("#66FCF1")
        private var shape = "DOT"
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        fun updateProperties(sizeDp: Int, alpha: Int, hexColor: String, shape: String) {
            this.radiusPx = (sizeDp * context.resources.displayMetrics.density)
            this.shape = shape
            try {
                this.paintColor = Color.parseColor(hexColor)
            } catch (_: Exception) {
                this.paintColor = Color.CYAN
            }
            paint.color = paintColor
            paint.alpha = alpha.coerceIn(0, 255)
            requestLayout()
            invalidate()
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val dimen = (radiusPx * 2 + 16).toInt()
            setMeasuredDimension(dimen, dimen)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f

            when (shape) {
                "DOT" -> {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, radiusPx / 4f, paint)
                }
                "CROSS" -> {
                    paint.style = Paint.Style.STROKE
                    canvas.drawLine(cx - radiusPx, cy, cx + radiusPx, cy, paint)
                    canvas.drawLine(cx, cy - radiusPx, cx, cy + radiusPx, paint)
                }
                "CIRCLE" -> {
                    paint.style = Paint.Style.STROKE
                    canvas.drawCircle(cx, cy, radiusPx, paint)
                }
                "COMBINED" -> {
                    paint.style = Paint.Style.STROKE
                    canvas.drawCircle(cx, cy, radiusPx, paint)
                    canvas.drawLine(cx - radiusPx, cy, cx + radiusPx, cy, paint)
                    canvas.drawLine(cx, cy - radiusPx, cx, cy + radiusPx, paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, radiusPx / 5f, paint)
                }
            }
        }
    }

    companion object {
        const val EXTRA_SIZE = "extra_size"
        const val EXTRA_ALPHA = "extra_alpha"
        const val EXTRA_COLOR = "extra_color"
        const val EXTRA_SHAPE = "extra_shape"
        const val EXTRA_EDIT_MODE = "extra_edit_mode"
    }
}

