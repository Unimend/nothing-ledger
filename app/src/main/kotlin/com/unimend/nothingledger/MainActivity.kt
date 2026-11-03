package com.unimend.nothingledger

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import kotlin.math.min

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(CoreDiagnosticsView())
    }

    private inner class CoreDiagnosticsView : View(this) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.BLACK)
            val unit = min(width, height) / 24f
            val left = unit * 2f
            paint.color = Color.WHITE
            paint.textSize = unit * 1.15f
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            canvas.drawText("NOTHING", left, unit * 3f, paint)
            canvas.drawText("LEDGER", left, unit * 4.35f, paint)

            paint.color = Color.rgb(255, 45, 45)
            paint.textSize = unit * 0.58f
            canvas.drawText("CORE DIAGNOSTICS / 0.0.1", left, unit * 6.1f, paint)

            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            paint.textSize = unit * 0.72f
            val rows = listOf(
                "01  MONEY           PASS",
                "02  TRANSACTION     PASS",
                "03  DOMAIN RULES    PASS",
                "04  TEST BASELINE   PASS",
                "05  DATABASE        NEXT",
                "06  NOTHING UI      NEXT",
            )
            rows.forEachIndexed { index, row ->
                paint.color = if (row.endsWith("PASS")) Color.WHITE else Color.rgb(110, 110, 110)
                canvas.drawText(row, left, unit * (8.2f + index * 1.35f), paint)
            }

            paint.color = Color.rgb(50, 50, 50)
            val cell = unit * 0.34f
            repeat(18) { x ->
                repeat(3) { y ->
                    if ((x + y) % 3 == 0) {
                        canvas.drawRect(
                            left + x * cell,
                            height - unit * 3f + y * cell,
                            left + x * cell + cell * 0.72f,
                            height - unit * 3f + y * cell + cell * 0.72f,
                            paint,
                        )
                    }
                }
            }
        }
    }
}

