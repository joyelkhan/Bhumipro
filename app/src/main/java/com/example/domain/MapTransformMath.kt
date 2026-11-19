package com.example.domain

import com.example.data.model.PointF
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class CanvasViewportState(
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val rotationDeg: Float = 0f
)

object MapTransformMath {
    const val MIN_ZOOM = 0.15f
    const val MAX_ZOOM = 16.0f

    fun clampZoom(zoom: Float): Float = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)

    fun zoomPercentToScale(percent: Int): Float = (percent.coerceIn(15, 1600) / 100f)

    fun scaleToZoomPercent(zoom: Float): Int = (clampZoom(zoom) * 100f).toInt().coerceIn(15, 1600)

    fun computeFitTransform(
        worldBounds: BoundingBox,
        viewportWidth: Float,
        viewportHeight: Float,
        paddingRatio: Float = 0.08f
    ): CanvasViewportState {
        if (viewportWidth <= 1f || viewportHeight <= 1f || worldBounds.width <= 0.1f || worldBounds.height <= 0.1f) {
            return CanvasViewportState()
        }
        val usableW = viewportWidth * (1f - paddingRatio * 2f)
        val usableH = viewportHeight * (1f - paddingRatio * 2f)
        val scale = clampZoom(min(usableW / worldBounds.width, usableH / worldBounds.height))
        val panX = viewportWidth / 2f - worldBounds.centerX * scale
        val panY = viewportHeight / 2f - worldBounds.centerY * scale
        return CanvasViewportState(zoom = scale, panX = panX, panY = panY, rotationDeg = 0f)
    }

    fun screenToWorld(screenX: Float, screenY: Float, state: CanvasViewportState, centerX: Float = 0f, centerY: Float = 0f): PointF {
        val x0 = (screenX - state.panX) / state.zoom
        val y0 = (screenY - state.panY) / state.zoom
        if (state.rotationDeg == 0f) return PointF(x0, y0)
        val rad = Math.toRadians((-state.rotationDeg).toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val dx = x0 - centerX
        val dy = y0 - centerY
        return PointF(
            x = centerX + dx * cosA - dy * sinA,
            y = centerY + dx * sinA + dy * cosA
        )
    }

    fun worldToScreen(worldX: Float, worldY: Float, state: CanvasViewportState, centerX: Float = 0f, centerY: Float = 0f): PointF {
        val rx: Float
        val ry: Float
        if (state.rotationDeg == 0f) {
            rx = worldX
            ry = worldY
        } else {
            val rad = Math.toRadians(state.rotationDeg.toDouble())
            val cosA = cos(rad).toFloat()
            val sinA = sin(rad).toFloat()
            val dx = worldX - centerX
            val dy = worldY - centerY
            rx = centerX + dx * cosA - dy * sinA
            ry = centerY + dx * sinA + dy * cosA
        }
        return PointF(rx * state.zoom + state.panX, ry * state.zoom + state.panY)
    }

    fun applyFocalZoom(
        current: CanvasViewportState,
        zoomFactor: Float,
        focalScreenX: Float,
        focalScreenY: Float
    ): CanvasViewportState {
        val newZoom = clampZoom(current.zoom * zoomFactor)
        val actualFactor = newZoom / current.zoom
        val newPanX = focalScreenX - (focalScreenX - current.panX) * actualFactor
        val newPanY = focalScreenY - (focalScreenY - current.panY) * actualFactor
        return current.copy(zoom = newZoom, panX = newPanX, panY = newPanY)
    }
}
