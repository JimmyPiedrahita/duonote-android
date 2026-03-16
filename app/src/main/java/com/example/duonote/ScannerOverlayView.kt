package com.example.duonote

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class ScannerOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintBackground = Paint().apply {
        color = Color.parseColor("#80050A0A") // El color oscuro semitransparente original
        style = Paint.Style.FILL
    }

    private val paintClear = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.FILL
    }
    
    private val holeRect = RectF()
    private val cornerRadius = 24f * resources.displayMetrics.density // Mismo radio que el frame

    init {
        // Necesario para que funcione el CLEAR mode
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Dibujamos todo el fondo oscuro
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBackground)
        
        // Calculamos el cuadrado de 280dp centrado
        val holeSize = 280f * resources.displayMetrics.density
        val left = (width - holeSize) / 2f
        val top = (height - holeSize) / 2f
        holeRect.set(left, top, left + holeSize, top + holeSize)
        
        // Perforamos el fondo con un rectÃ¡ngulo redondeado transparente
        canvas.drawRoundRect(holeRect, cornerRadius, cornerRadius, paintClear)
    }
}