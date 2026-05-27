package com.liquor.ledger

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class ReportsPage(private val activity: Activity) {

    fun build(): LinearLayout {

        val page = LinearLayout(activity)
        page.orientation = LinearLayout.VERTICAL
        page.setBackgroundColor(Color.WHITE)
        page.setPadding(dp(20), dp(20), dp(20), dp(20))

        // REPORT TITLE
        val title = TextView(activity)
        title.text = "Sales Analytics"
        title.textSize = 22f
        title.setTextColor(Color.BLACK)

        // REPORT DESCRIPTION
        val description = TextView(activity)
        description.text = "Comprehensive sales data and revenue analysis."
        description.textSize = 14f
        description.setTextColor(Color.DKGRAY)
        description.setPadding(0, dp(8), 0, dp(20))

        // SUMMARY ROW
        val summaryRow = LinearLayout(activity)
        summaryRow.orientation = LinearLayout.HORIZONTAL

        summaryRow.addView(makeSummaryCard("Total Income", "$0.00", Color.rgb(0, 150, 70)))
        summaryRow.addView(makeSummaryCard("Total Expenses", "$0.00", Color.RED))
        summaryRow.addView(makeSummaryCard("Net", "$0.00", Color.rgb(45, 95, 255)))

        // GRAPH DATA
        val graphData = listOf(
            ReportBar("Sales Revenue", 0f),
            ReportBar("ATM Commissions", 0f)
        )

        // GRAPH
        val graph = BarChartView(activity, graphData)

        val graphParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(220)
        )

        graphParams.setMargins(0, dp(30), 0, dp(20))

        // EXPORT BUTTON PLACEHOLDER
        val exportButton = TextView(activity)
        exportButton.text = "Export to Excel"
        exportButton.textSize = 16f
        exportButton.gravity = Gravity.CENTER
        exportButton.setTextColor(Color.WHITE)
        exportButton.setBackgroundColor(Color.rgb(20, 180, 120))
        exportButton.setPadding(0, dp(14), 0, dp(14))

        page.addView(title)
        page.addView(description)
        page.addView(summaryRow)
        page.addView(graph, graphParams)
        page.addView(exportButton)

        return page
    }

    private fun makeSummaryCard(label: String, amount: String, color: Int): TextView {

        val card = TextView(activity)
        card.text = "$label\n$amount"
        card.textSize = 18f
        card.setTextColor(color)
        card.setPadding(dp(20), dp(16), dp(20), dp(16))
        card.setBackgroundColor(Color.rgb(245, 247, 250))

        val params = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        params.setMargins(dp(6), 0, dp(6), 0)
        card.layoutParams = params

        return card
    }

    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}

data class ReportBar(
    val label: String,
    val value: Float
)

class BarChartView(
    activity: Activity,
    private val data: List<ReportBar>
) : View(activity) {

    private val barPaint = Paint()
    private val textPaint = Paint()
    private val axisPaint = Paint()

    init {
        barPaint.color = Color.rgb(20, 180, 120)

        textPaint.color = Color.DKGRAY
        textPaint.textSize = 28f
        textPaint.textAlign = Paint.Align.CENTER

        axisPaint.color = Color.LTGRAY
        axisPaint.strokeWidth = 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) {
            return
        }

        canvas.drawColor(Color.WHITE)

        val paddingLeft = 100f
        val paddingTop = 40f
        val paddingBottom = 100f

        val chartBottom = height - paddingBottom
        val chartTop = paddingTop
        val chartHeight = chartBottom - chartTop

        val maxValue = 500f
        val increment = 50f

        // GRID LINE PAINT
        val gridPaint = Paint()
        gridPaint.color = Color.LTGRAY
        gridPaint.strokeWidth = 2f

        // AXIS TEXT PAINT
        val axisTextPaint = Paint()
        axisTextPaint.color = Color.GRAY
        axisTextPaint.textSize = 24f

        // DRAW GRID LINES + LABELS
        var currentValue = 0f

        while (currentValue <= maxValue) {

            val y = chartBottom - (currentValue / maxValue) * chartHeight

            // LINE
            canvas.drawLine(
                paddingLeft,
                y,
                width.toFloat(),
                y,
                gridPaint
            )

            // NUMBER LABEL
            canvas.drawText(
                currentValue.toInt().toString(),
                20f,
                y + 8f,
                axisTextPaint
            )

            currentValue += increment
        }

        // BAR SIZING
        val barSpace = (width - paddingLeft) / data.size
        val barWidth = barSpace * 0.45f

        // DRAW BARS
        for (i in data.indices) {

            val item = data[i]

            val barHeight = (item.value / maxValue) * chartHeight

            val left = paddingLeft + i * barSpace + barSpace * 0.25f
            val top = chartBottom - barHeight
            val right = left + barWidth
            val bottom = chartBottom

            // BAR
            canvas.drawRect(left, top, right, bottom, barPaint)

            // VALUE LABEL
            canvas.drawText(
                "$${item.value}",
                left + barWidth / 2f,
                top - 10f,
                textPaint
            )

            // BAR NAME
            canvas.drawText(
                item.label,
                left + barWidth / 2f,
                height - 30f,
                textPaint
                )
            }
        }
    }