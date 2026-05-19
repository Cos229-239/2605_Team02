package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

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
            290,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        // APP TITLE
        val title = TextView(this)
        title.text = "Liquor Ledger"
        title.textSize = 22f
        title.setTextColor(Color.WHITE)
        title.setPadding(30, 60, 20, 60)

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
        val header = TextView(this)
        header.text = "Dashboard"
        header.textSize = 32f
        header.setTextColor(Color.BLACK)
        header.setPadding(50, 50, 0, 20)

        // CONTENT BOX
        val contentBox = LinearLayout(this)
        contentBox.setBackgroundColor(Color.rgb(245, 245, 245))

        val boxParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            900
        )

        boxParams.setMargins(40, 20, 40, 40)

        mainContent.addView(header)
        mainContent.addView(contentBox, boxParams)

        root.addView(sidebar, sidebarParams)
        root.addView(mainContent, mainParams)

        setContentView(root)
    }

    private fun makeTab(text: String, active: Boolean): TextView {

        val tab = TextView(this)

        tab.text = text
        tab.textSize = 18f
        tab.gravity = Gravity.CENTER_VERTICAL

        tab.setPadding(35, 30, 20, 30)

        if (active) {
            tab.setBackgroundColor(Color.rgb(45, 95, 255))
        }

        tab.setTextColor(Color.WHITE)

        return tab
    }
}