package com.piecer.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

data class WrappedCardShareContent(
    val eyebrow: String,
    val title: String,
    val bigValue: String,
    val subtitle: String,
    val isClosingCard: Boolean = false
)

object WrappedShareHelper {
    private const val WIDTH = 1080
    private const val HEIGHT = 1920
    private const val COLOR_INK = 0xFF0D0D0F.toInt()
    private const val COLOR_CHARCOAL = 0xFF1C1C1E.toInt()
    private const val COLOR_WHITE = 0xFFFFFFFF.toInt()
    private const val COLOR_STONE = 0xFF8E8E93.toInt()
    private const val COLOR_MINT = 0xFF34D399.toInt()

    fun shareCard(
        context: Context,
        card: WrappedCardShareContent,
        periodLabel: String,
        shareChooserTitle: String
    ) {
        val bitmap = renderCardBitmap(card, periodLabel)
        val cacheFolder = File(context.cacheDir, "wrapped").apply { mkdirs() }
        val outFile = File(cacheFolder, "piecer_wrapped.png")
        FileOutputStream(outFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, shareChooserTitle))
    }

    fun renderCardBitmap(
        card: WrappedCardShareContent,
        periodLabel: String
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(COLOR_INK)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_CHARCOAL
        }
        val cardRect = RectF(64f, 120f, WIDTH - 64f, HEIGHT - 120f)
        canvas.drawRoundRect(cardRect, 56f, 56f, cardPaint)

        val eyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MINT
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(card.eyebrow.uppercase(), 132f, 260f, eyebrowPaint)

        val periodPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_STONE
            textSize = 36f
        }
        canvas.drawText(periodLabel, 132f, 320f, periodPaint)

        if (card.isClosingCard) {
            val piePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_WHITE
            }
            val pieRect = RectF(WIDTH / 2f - 140f, HEIGHT / 2f - 280f, WIDTH / 2f + 140f, HEIGHT / 2f)
            canvas.drawArc(pieRect, -60f, 300f, true, piePaint)

            val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_MINT
            }
            canvas.drawArc(pieRect, 250f, 40f, true, accentPaint)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleY = if (card.isClosingCard) HEIGHT / 2f + 120f else 620f
        drawWrappedText(canvas, card.title, 132f, titleY, WIDTH - 264f, titlePaint, 78f)

        val bigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MINT
            textSize = 104f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bigY = if (card.isClosingCard) HEIGHT / 2f + 260f else 900f
        drawWrappedText(canvas, card.bigValue, 132f, bigY, WIDTH - 264f, bigPaint, 120f)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_STONE
            textSize = 44f
        }
        val subY = if (card.isClosingCard) HEIGHT / 2f + 380f else 1120f
        drawWrappedText(canvas, card.subtitle, 132f, subY, WIDTH - 264f, subPaint, 58f)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 255, 255, 255)
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("PIECER", 132f, HEIGHT - 200f, footerPaint)

        return bitmap
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float
    ) {
        if (text.isBlank()) return
        val words = text.split(" ")
        var line = ""
        var y = startY
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth) {
                line = candidate
            } else {
                canvas.drawText(line, x, y, paint)
                line = word
                y += lineHeight
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, x, y, paint)
        }
    }
}
