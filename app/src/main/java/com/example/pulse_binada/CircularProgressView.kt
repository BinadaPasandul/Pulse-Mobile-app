package com.example.pulse_binada

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    
    private var progress = 0f
    private var maxProgress = 100f
    private var progressColor = ContextCompat.getColor(context, R.color.primary_color)
    private var progressBackgroundColor = Color.WHITE
    private var progressStrokeWidth = 12f
    private var progressBackgroundStrokeWidth = 8f

    init {
        // Load custom attributes
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircularProgressView)
        try {
            progressColor = typedArray.getColor(R.styleable.CircularProgressView_progressColor, progressColor)
            progressBackgroundColor = typedArray.getColor(R.styleable.CircularProgressView_progressBackgroundColor, progressBackgroundColor)
            progressStrokeWidth = typedArray.getDimension(R.styleable.CircularProgressView_progressStrokeWidth, progressStrokeWidth)
            progressBackgroundStrokeWidth = typedArray.getDimension(R.styleable.CircularProgressView_progressBackgroundStrokeWidth, progressBackgroundStrokeWidth)
        } finally {
            typedArray.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (minOf(width, height) / 2f) - maxOf(progressStrokeWidth, progressBackgroundStrokeWidth) / 2f
        
        rectF.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        
        // Draw background circle
        backgroundPaint.color = progressBackgroundColor
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = progressBackgroundStrokeWidth
        backgroundPaint.strokeCap = Paint.Cap.ROUND
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
        
        // Draw progress arc
        paint.color = progressColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = progressStrokeWidth
        paint.strokeCap = Paint.Cap.ROUND
        
        val sweepAngle = (progress / maxProgress) * 360f
        canvas.drawArc(rectF, -90f, sweepAngle, false, paint)
    }

    fun setProgress(progress: Float) {
        this.progress = progress.coerceIn(0f, maxProgress)
        invalidate()
    }

    fun setMaxProgress(maxProgress: Float) {
        this.maxProgress = maxProgress
        invalidate()
    }

    fun getProgress(): Float = progress

    fun getMaxProgress(): Float = maxProgress
}
