package com.liquor.ledger

// IMPORTS NEEDED FOR UI COMPONENTS
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/*
 * SettingsPage
 */
class SettingsPage(private val context: Context) {

    /*
     * build()
     *
     * Creates the full Settings page layout and returns it.
     * This layout is later added into MainActivity's content area.
     */
    fun build(): LinearLayout {

        // MAIN VERTICAL CONTAINER
        val layout = LinearLayout(context)

        // STACK ITEMS VERTICALLY
        layout.orientation = LinearLayout.VERTICAL

        // ADD INNER PADDING
        layout.setPadding(dp(20), dp(20), dp(20), dp(20))

        // PAGE TITLE
        layout.addView(makeTitle("Settings"))

        // SETTINGS TOGGLES
        layout.addView(makeSwitch("Colorblind Mode"))
        layout.addView(makeSwitch("Dark Mode"))
        layout.addView(makeSwitch("Low Stock Alerts"))
        layout.addView(makeSwitch("Sales Alerts"))
        layout.addView(makeSwitch("Sound Notifications"))
        layout.addView(makeSwitch("Auto Print Receipts"))

        // TEST PRINTER BUTTON
        val testPrinterButton = Button(context)

        // BUTTON LABEL
        testPrinterButton.text = "Test Printer"

        // ADD BUTTON TO LAYOUT
        layout.addView(testPrinterButton)

        // RETURN COMPLETED SETTINGS LAYOUT
        return layout
    }

    /*
     * makeTitle()
     *
     * Creates and styles the page title TextView.
     */
    private fun makeTitle(text: String): TextView {

        // CREATE TITLE TEXTVIEW
        val title = TextView(context)

        // SET DISPLAYED TEXT
        title.text = text

        // TITLE FONT SIZE
        title.textSize = 28f

        // TITLE COLOR
        title.setTextColor(Color.BLACK)

        // ALIGN TEXT TO START/LEFT
        title.gravity = Gravity.START

        // ADD SPACE BELOW TITLE
        title.setPadding(0, 0, 0, dp(20))

        return title
    }

    /*
     * makeSwitch()
     *
     * Creates a reusable styled switch component
     * for application settings.
     */
    private fun makeSwitch(text: String): Switch {

        // CREATE SWITCH COMPONENT
        val settingSwitch = Switch(context)

        // SET SWITCH LABEL
        settingSwitch.text = text

        // TEXT SIZE
        settingSwitch.textSize = 18f

        // TEXT COLOR
        settingSwitch.setTextColor(Color.DKGRAY)

        // ADD VERTICAL SPACING
        settingSwitch.setPadding(0, dp(10), 0, dp(10))

        return settingSwitch
    }

    /*
     * dp()
     *
     * Converts density-independent pixels (dp)
     * into actual screen pixels based on device density.
     *
     * This keeps UI sizing consistent across devices.
     */
    private fun dp(value: Int): Int {

        return (value * context.resources.displayMetrics.density).toInt()
    }
}