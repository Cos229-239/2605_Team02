package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var header: TextView
    private lateinit var contentBox: LinearLayout

    private val darkBlue = Color.rgb(16, 30, 55)
    private val activeBlue = Color.rgb(45, 95, 255)
    private val lightGray = Color.rgb(245, 245, 245)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ROOT LAYOUT
        val root = LinearLayout(this)
        root.orientation = LinearLayout.HORIZONTAL
        root.setBackgroundColor(Color.WHITE)

        // SIDEBAR
        val sidebar = LinearLayout(this)
        sidebar.orientation = LinearLayout.VERTICAL
        sidebar.setBackgroundColor(Color.rgb(16, 30, 55))

        val sidebarParams = LinearLayout.LayoutParams(
            dp(230),
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        // APP TITLE
        val title = TextView(this)
        title.text = "Liquor Ledger"
        title.textSize = 22f
        title.setTextColor(Color.WHITE)
        title.setPadding(dp(24), dp(48), dp(16), dp(40))

        sidebar.addView(title)

        // TABS
        sidebar.addView(makeTab("POS / Register", true))
        sidebar.addView(makeTab("Inventory", false))
        //sidebar.addView(makeTab("Orders", false))
        sidebar.addView(makeTab("Reports", false))
        //sidebar.addView(makeTab("User Information", active = false))
        //sidebar.addView(makeTab("Emergency Contacts", active = false))
        sidebar.addView(makeTab("Settings", false))

        // MAIN CONTENT
        val mainContent = LinearLayout(this)
        mainContent.orientation = LinearLayout.VERTICAL
        mainContent.setBackgroundColor(Color.WHITE)

        val mainParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )

        // HEADER
        header = TextView(this)
        header.text = "POS / Register"
        header.textSize = 32f
        header.setTextColor(Color.BLACK)
        header.setPadding(dp(32), dp(32), 0, dp(16))

        // CONTENT BOX
        contentBox = LinearLayout(this)
        contentBox.orientation = LinearLayout.VERTICAL
        contentBox.setBackgroundColor(lightGray)
        contentBox.setPadding(dp(20), dp(20), dp(20), dp(20))

        val boxParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        boxParams.setMargins(dp(24), dp(10), dp(24), dp(24))

        mainContent.addView(header)
        mainContent.addView(contentBox, boxParams)

        root.addView(sidebar, sidebarParams)
        root.addView(mainContent, mainParams)

        setContentView(root)

        loadPage("POS / Register")
    }

    private fun makeTab(text: String, active: Boolean): TextView {

        val tab = TextView(this)

        tab.text = text
        tab.textSize = 16f
        tab.gravity = Gravity.CENTER_VERTICAL
        tab.setPadding(dp(28), dp(18), dp(16), dp(18))
        tab.setTextColor(Color.WHITE)

        tab.setOnClickListener {
            loadPage(text)
        }

        return tab
    }

    private fun loadPage(pageName: String) {
        header.text = pageName
        contentBox.removeAllViews()

        if (pageName == "POS / Register") {

    }
            val posPage = POSPage(this)
            contentBox.addView(posPage.build())
        }

        else {
            val pageText = TextView(this)
            pageText.text = "$pageName screen will go here"
            pageText.textSize = 18f
            pageText.setTextColor(Color.DKGRAY)
            pageText.setPadding(dp(20), dp(20), dp(20), dp(20))

            contentBox.addView(pageText)
        }
    }
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
