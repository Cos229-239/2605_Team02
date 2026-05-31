package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var mainContent: LinearLayout
    private lateinit var header: TextView
    private lateinit var contentBox: LinearLayout

    private var reportsExpanded = false

    private val darkBlue = Color.rgb(16, 30, 55)
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
        sidebar.setBackgroundColor(darkBlue)

        // SIDEBAR SIZE
        val sidebarParams = LinearLayout.LayoutParams(
            dp(230),
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        // APP TITLE
        val title = TextView(this)
        title.text = "Liquor\nLedger"
        title.textSize = 22f
        title.setTextColor(Color.WHITE)
        title.setPadding(dp(24), dp(48), dp(16), dp(40))

        sidebar.addView(title)

        // SIDEBAR TABS
        sidebar.addView(makeTab("POS / Register"))
        sidebar.addView(makeTab("Inventory"))
        if (SessionManager.currentEmployee?.position == "Manager") {
            sidebar.addView(makeTab("Purchase Orders"))
        }
        sidebar.addView(makeReportsTab(sidebar))
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
        loadPage("POS / Register")
    }

    private fun makeTab(text: String): TextView {

        // TAB TEXT
        val tab = TextView(this)

        tab.text = text
        tab.textSize = 16f
        tab.gravity = Gravity.CENTER_VERTICAL
        tab.setPadding(dp(28), dp(18), dp(16), dp(18))
        tab.setTextColor(Color.WHITE)

        // TAB CLICK EVENT
        tab.setOnClickListener {
            loadPage(text)
        }

        return tab

    }

    private fun makeReportsTab(sidebar: LinearLayout): TextView {

        val tab = makeTab("Reports")

        tab.setOnClickListener {

            reportsExpanded = !reportsExpanded

            sidebar.removeAllViews()

            // APP TITLE
            val title = TextView(this)
            title.text = "Liquor\nLedger"
            title.textSize = 22f
            title.setTextColor(Color.WHITE)
            title.setPadding(dp(24), dp(48), dp(16), dp(40))

            sidebar.addView(title)

            // SIDEBAR TABS
            sidebar.addView(makeTab("POS / Register"))
            sidebar.addView(makeTab("Inventory"))

            if (SessionManager.currentEmployee?.position == "Manager") {
                sidebar.addView(makeTab("Purchase Orders"))
                sidebar.addView(makeReportsTab(sidebar))
            }

            sidebar.addView(makeReportsTab(sidebar))

            if (reportsExpanded) {
                sidebar.addView(makeTab("   Sales Analytics"))
                sidebar.addView(makeTab("   Sales Report"))
                sidebar.addView(makeTab("   Inventory Report"))
                sidebar.addView(makeTab("   Inventory Alert"))
            }

            sidebar.addView(makeTab("Settings"))

        }

        return tab
    }

    private fun loadPage(pageName: String) {

        // CLEAR OLD PAGE
        mainContent.removeAllViews()

        // PAGE HEADER
        header = TextView(this)
        header.text = pageName
        header.textSize = 32f
        header.setTextColor(Color.BLACK)
        header.setPadding(dp(32), dp(32), 0, dp(16))

        // CONTENT BOX
        contentBox = LinearLayout(this)
        contentBox.orientation = LinearLayout.VERTICAL
        contentBox.setBackgroundColor(lightGray)
        contentBox.setPadding(dp(20), dp(20), dp(20), dp(20))

        // CONTENT BOX SIZE
        val boxParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        // CONTENT BOX MARGINS
        boxParams.setMargins(dp(24), dp(10), dp(24), dp(24))

        // ADD HEADER AND CONTENT BOX
        mainContent.addView(header)
        mainContent.addView(contentBox, boxParams)

        if (pageName == "POS / Register") {

            val posPage = POSPage(this)

            contentBox.addView(
                posPage.build(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )

        }
        else if (pageName == "Reports" || pageName == "   Sales Analytics") {

            val reportsPage = ReportsPage(this)

            contentBox.addView(
                reportsPage.build(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )

        }
        else if (
            pageName == "   Sales Report" ||
            pageName == "   Inventory Report" ||
            pageName == "   Inventory Alert"
        )

        else if (pageName == "Inventory") {
            val inventoryPage = InventoryPage(this)
            contentBox.addView(
                inventoryPage.build(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        else if (pageName == "Purchase Orders") {
            val purchaseOrdersPage = PurchaseOrdersPage(this)
            contentBox.addView(
                purchaseOrdersPage.build(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        else if (pageName == "Settings") {
            val settingsPage = SettingsPage(this)
            contentBox.addView(settingsPage.build())

        } else {
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
