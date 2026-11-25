package com.example.leitroboter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.example.leitroboter.data.MapRoute
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class RouteMapView(context: Context, attrs: AttributeSet? = null) : AppCompatImageView(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var routes: List<MapRoute> = emptyList()

    fun setRoutes(value: List<MapRoute>) {
        routes = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val map = drawable ?: return
        if (routes.isEmpty()) return
        val mapWidth = map.intrinsicWidth.toFloat()
        val mapHeight = map.intrinsicHeight.toFloat()
        if (mapWidth <= 0 || mapHeight <= 0) return

        canvas.save()
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        canvas.concat(imageMatrix)
        val unit = mapWidth / 1400f
        for (route in routes) {
            if (route.points.isEmpty()) continue
            path.reset()
            route.points.forEachIndexed { index, point ->
                val x = point.x * mapWidth
                val y = point.y * mapHeight
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Color.WHITE
            paint.strokeWidth = 12f * unit
            canvas.drawPath(path, paint)
            paint.color = Color.rgb(22, 146, 71)
            paint.strokeWidth = 7f * unit
            canvas.drawPath(path, paint)

            val end = route.points.last()
            val x = end.x * mapWidth
            val y = end.y * mapHeight
            if (route.points.size > 1) {
                val before = route.points[route.points.lastIndex - 1]
                val angle = atan2(y - before.y * mapHeight, x - before.x * mapWidth)
                val size = 17f * unit
                path.reset()
                path.moveTo(x, y)
                path.lineTo(x - size * cos(angle - 0.5f), y - size * sin(angle - 0.5f))
                path.lineTo(x - size * cos(angle + 0.5f), y - size * sin(angle + 0.5f))
                path.close()
                paint.style = Paint.Style.FILL
                canvas.drawPath(path, paint)
                label(canvas, route.label, x, y, mapWidth, mapHeight, unit, Color.rgb(163, 35, 33))
            }
        }
        val origin = routes.first().points.firstOrNull()
        if (origin != null) {
            val x = origin.x * mapWidth
            val y = origin.y * mapHeight
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            canvas.drawCircle(x, y, 10f * unit, paint)
            paint.color = Color.rgb(28, 96, 198)
            canvas.drawCircle(x, y, 7f * unit, paint)
            label(canvas, context.getString(R.string.map_origin), x, y, mapWidth, mapHeight, unit, paint.color)
        }
        canvas.restore()
    }

    private fun label(canvas: Canvas, text: String, x: Float, y: Float, width: Float, height: Float, unit: Float, color: Int) {
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 23f * unit
        val textWidth = paint.measureText(text)
        val left = (x - textWidth / 2).coerceIn(8f * unit, maxOf(8f * unit, width - textWidth - 8f * unit))
        val baseline = (y - 17f * unit).coerceIn(31f * unit, height - 8f * unit)
        paint.color = Color.WHITE
        canvas.drawRoundRect(left - 5f * unit, baseline - 25f * unit, left + textWidth + 5f * unit, baseline + 6f * unit, 5f * unit, 5f * unit, paint)
        paint.color = color
        canvas.drawText(text, left, baseline, paint)
    }
}
