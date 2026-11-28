package com.piecer.app.ui.crash

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.piecer.app.R
import com.piecer.app.util.CrashReporter

/**
 * Layar crash mandiri di proses terpisah (:crash).
 * Menggunakan Android View murni tanpa Hilt, Compose, DataStore, Room, atau tema app.
 */
class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reportText = resolveReportText()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0D0F"))
            val pad = dp(16)
            setPadding(pad, dp(24), pad, pad)
            fitsSystemWindows = true
        }

        val titleView = TextView(this).apply {
            text = getString(R.string.crash_activity_title)
            setTextColor(Color.parseColor("#FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
        }
        root.addView(titleView)

        val subtitleView = TextView(this).apply {
            text = getString(R.string.crash_activity_subtitle)
            setTextColor(Color.parseColor("#8E8E93"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(4), 0, dp(12))
        }
        root.addView(subtitleView)

        val scrollView = ScrollView(this).apply {
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            layoutParams = lp
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1C1C1E"))
                cornerRadius = dp(16).toFloat()
            }
            val innerPad = dp(12)
            setPadding(innerPad, innerPad, innerPad, innerPad)
        }

        val reportView = TextView(this).apply {
            text = reportText
            setTextColor(Color.parseColor("#F4F4F5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        scrollView.addView(reportView)
        root.addView(scrollView)

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
            layoutParams = lp
        }

        val copyBtn = createActionButton(
            label = getString(R.string.crash_report_copy),
            bgColor = Color.parseColor("#2C2C2E"),
            textColor = Color.parseColor("#FFFFFF")
        ) {
            copyToClipboard(reportText)
        }
        val shareBtn = createActionButton(
            label = getString(R.string.crash_report_share),
            bgColor = Color.parseColor("#FFFFFF"),
            textColor = Color.parseColor("#0D0D0F")
        ) {
            shareReport(reportText)
        }
        val closeBtn = createActionButton(
            label = getString(R.string.crash_activity_close),
            bgColor = Color.parseColor("#2C2C2E"),
            textColor = Color.parseColor("#F87171")
        ) {
            finishAffinity()
        }

        buttonRow.addView(copyBtn, buttonLayoutParams(endMargin = dp(8)))
        buttonRow.addView(shareBtn, buttonLayoutParams(endMargin = dp(8)))
        buttonRow.addView(closeBtn, buttonLayoutParams(endMargin = 0))
        root.addView(buttonRow)

        setContentView(root)
    }

    private fun resolveReportText(): String {
        val fromExtra = intent?.getStringExtra(EXTRA_CRASH_REPORT)?.takeIf { it.isNotBlank() }
        if (fromExtra != null) return fromExtra
        return CrashReporter.readCrashReport(this)
            ?: getString(R.string.crash_report_empty)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(
            ClipData.newPlainText(getString(R.string.crash_report_share_subject), text)
        )
        Toast.makeText(this, getString(R.string.crash_report_copied), Toast.LENGTH_SHORT).show()
    }

    private fun shareReport(text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crash_report_share_subject))
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.crash_report_share)))
    }

    private fun createActionButton(
        label: String,
        bgColor: Int,
        textColor: Int,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(8), dp(10), dp(8), dp(10))
            setOnClickListener { onClick() }
        }
    }

    private fun buttonLayoutParams(endMargin: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            marginEnd = endMargin
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    companion object {
        const val EXTRA_CRASH_REPORT = "extra_crash_report"

        fun createLaunchIntent(context: Context): Intent {
            return Intent(context, CrashActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
            }
        }
    }
}
