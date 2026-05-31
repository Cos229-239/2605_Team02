package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class InventoryReportPage(private val activity: Activity) {

    fun build(): LinearLayout {
        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)
        root.setPadding(40, 40, 40, 40)

        val title = TextView(activity)
        title.text = "Inventory Report"
        title.textSize = 28f
        title.setTextColor(Color.BLACK)
        title.setPadding(0, 0, 0, 30)

        val summary = TextView(activity)
        summary.text = """
            Total Products: 0
            Low Stock Items: 0
            Out of Stock Items: 0
            Inventory Value: $0.00
        """.trimIndent()
        summary.textSize = 20f
        summary.setTextColor(Color.BLACK)
        summary.setPadding(20, 20, 20, 20)

        root.addView(title)
        root.addView(summary)

        return root
    }
}