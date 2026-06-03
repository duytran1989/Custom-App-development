package com.marknguyen.customappdevelopment.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.marknguyen.customappdevelopment.util.WeatherUtils
import kotlin.math.*

class SunriseSunsetView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#30FFFFFF")
    }
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#FFC107")
    }
    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFC107")
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCFFFFFF")
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    var sunriseEpoch: Long = 0L
    var sunsetEpoch: Long = 0L
    var currentEpoch: Long = 0L
    var timezoneOffset: Int = 0

    fun setTimes(sunrise: Long, sunset: Long, current: Long, tzOffset: Int) {
        sunriseEpoch = sunrise
        sunsetEpoch = sunset
        currentEpoch = current
        timezoneOffset = tzOffset
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h * 0.85f
        val r = (w * 0.38f)
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)

        // Full arc (track)
        canvas.drawArc(rect, 180f, 180f, false, trackPaint)

        // Active arc up to current time
        val progress = if (sunsetEpoch > sunriseEpoch) {
            ((currentEpoch - sunriseEpoch).toFloat() / (sunsetEpoch - sunriseEpoch)).coerceIn(0f, 1f)
        } else 0f
        canvas.drawArc(rect, 180f, 180f * progress, false, activePaint)

        // Sun dot on arc
        val angle = Math.toRadians((180.0 + 180.0 * progress))
        val sx = cx + r * cos(angle).toFloat()
        val sy = cy + r * sin(angle).toFloat()
        canvas.drawCircle(sx, sy, 12f, sunPaint)

        // Labels
        val riseLabel = WeatherUtils.formatSunTime(sunriseEpoch, timezoneOffset)
        val setLabel = WeatherUtils.formatSunTime(sunsetEpoch, timezoneOffset)
        canvas.drawText(riseLabel, cx - r + 10f, cy + 36f, labelPaint)
        canvas.drawText(setLabel, cx + r - 10f, cy + 36f, labelPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, (w * 0.55f).toInt())
    }
}
