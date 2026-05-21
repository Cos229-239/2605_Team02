package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var mainContent: LinearLayout
    private lateinit var header: TextView
    private lateinit var contentBox: LinearLayout

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

        // SIDEBAR SIZE
        val sidebarParams = LinearLayout.LayoutParams(
            290,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        // APP TITLE
        val title = TextView(this)
        title.text = "Liquor\nLedger"
        title.textSize = 22f
        title.setTextColor(Color.WHITE)
        title.setPadding(30, 60, 20, 60)

        sidebar.addView(title)

        // SIDEBAR TABS
        sidebar.addView(makeTab("POS / Register"))
        sidebar.addView(makeTab("Inventory"))
        sidebar.addView(makeTab("Reports"))
        sidebar.addView(makeTab("Settings"))

        // MAIN CONTENT AREA
        mainContent = LinearLayout(this)
        mainContent.orientation = LinearLayout.VERTICAL
        mainContent.setBackgroundColor(Color.WHITE)

        // MAIN CONTENT SIZE
        val mainParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )

        // ADD SIDEBAR AND CONTENT TO ROOT
        root.addView(sidebar, sidebarParams)
        root.addView(mainContent, mainParams)

        // SET SCREEN CONTENT
        setContentView(root)

        // DEFAULT PAGE
        showPage("POS / Register")
    }

    private fun makeTab(text: String): TextView {

        // TAB TEXT
        val tab = TextView(this)

        tab.text = text
        tab.textSize = 18f
        tab.setTextColor(Color.WHITE)

        // TAB SPACING
        tab.setPadding(35, 20, 20, 20)

        // TAB CLICK EVENT
        tab.setOnClickListener {
            showPage(text)
        }

        return tab
    }

    private fun showPage(pageName: String) {

        // CLEAR OLD PAGE
        mainContent.removeAllViews()

        // PAGE HEADER
        header = TextView(this)

        header.text = pageName
        header.textSize = 32f
        header.setTextColor(Color.BLACK)
        header.setPadding(50, 50, 0, 20)

        // CONTENT BOX
        contentBox = LinearLayout(this)

        contentBox.orientation = LinearLayout.VERTICAL
        contentBox.setBackgroundColor(Color.WHITE)

        // CONTENT BOX SIZE
        val boxParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            900
        )

        // CONTENT BOX MARGINS
        boxParams.setMargins(40, 20, 40, 40)

        // ADD HEADER AND CONTENT BOX
        mainContent.addView(header)
        mainContent.addView(contentBox, boxParams)
    }
}