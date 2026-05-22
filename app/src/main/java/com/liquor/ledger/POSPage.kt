package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.TextView

class POSPage(private val activity: Activity) {
    fun build(): LinearLayout {

        val page = LinearLayout(activity)
        page.orientation = LinearLayout.HORIZONTAL
        page.setBackgroundColor(Color.WHITE)

        val leftSide = LinearLayout(activity)
        leftSide.orientation = LinearLayout.VERTICAL
        leftSide.setBackgroundColor(Color.rgb(245, 247, 250))

        val leftParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            3f
        )

        val rightPanel = LinearLayout(activity)
        rightPanel.orientation = LinearLayout.VERTICAL
        rightPanel.setBackgroundColor(Color.rgb(235, 239, 245))

        val rightParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )

        val leftText = TextView(activity)
        leftText.text = "Left POS Area"
        leftText.textSize = 22f
        leftText.setTextColor(Color.BLACK)
        leftText.setPadding(30, 30, 30, 30)

        val rightText = TextView(activity)
        rightText.text = "Payment Panel"
        rightText.textSize = 22f
        rightText.setTextColor(Color.BLACK)
        rightText.setPadding(30, 30, 30, 30)

<<<<<<< Updated upstream
            leftSide.addView(leftText)
            rightPanel.addView(rightText)

            page.addView(leftSide, leftParams)
            page.addView(rightPanel, rightParams)
=======
        leftSide.addView(leftText)
        rightPanel.addView(rightText)
        page.addView(leftSide, leftParams)
        page.addView(rightPanel, rightParams)
>>>>>>> Stashed changes

        return page
    }
}
