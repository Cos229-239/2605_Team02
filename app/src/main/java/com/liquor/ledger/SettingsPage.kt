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
 *
 * This class builds the Settings screen programmatically.
 * It is not an Activity. Instead, MainActivity creates this page
 * and adds the layout into the main content area.
 */
class SettingsPage(private val context: Context) {

    /*
     * SharedPreferences
     *
     * SharedPreferences allows us to save small pieces of data locally
     * on the device.
     *
     * We are using it here to remember whether each setting toggle
     * is ON or OFF after the app closes.
     */
    private val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    /*
     * Setting Keys
     *
     * These are the names used to save each setting.
     * Each key points to one Boolean value: true or false.
     */
    private val KEY_COLORBLIND_MODE = "colorblind_mode"
    private val KEY_DARK_MODE = "dark_mode"
    private val KEY_LOW_STOCK_ALERTS = "low_stock_alerts"
    private val KEY_SALES_ALERTS = "sales_alerts"

    /*
     * build()
     *
     * Creates the full Settings page layout and returns it.
     * This layout is later added into MainActivity's content area.
     */
    fun build(): LinearLayout {

        // MAIN VERTICAL CONTAINER
        val layout = LinearLayout(context)

        // STACK ITEMS FROM TOP TO BOTTOM
        layout.orientation = LinearLayout.VERTICAL

        // ADD INNER PADDING AROUND THE SETTINGS PAGE
        layout.setPadding(dp(20), dp(20), dp(20), dp(20))

        // PAGE TITLE
        layout.addView(makeTitle("Settings"))

        /*
         * FIRST 4 FUNCTIONAL SETTINGS TOGGLES
         *
         * These switches now save their state using SharedPreferences.
         * When the user turns one ON or OFF, the value is stored locally.
         */
        layout.addView(makeFunctionalSwitch("Colorblind Mode", KEY_COLORBLIND_MODE))
        layout.addView(makeFunctionalSwitch("Dark Mode", KEY_DARK_MODE))
        layout.addView(makeFunctionalSwitch("Low Stock Alerts", KEY_LOW_STOCK_ALERTS))
        layout.addView(makeFunctionalSwitch("Sales Alerts", KEY_SALES_ALERTS))

        /*
         * REMAINING UI-ONLY TOGGLES
         *
         * These switches still appear on the Settings screen,
         * but they do not save or trigger functionality yet.
         */
        layout.addView(makeSwitch("Sound Notifications"))
        layout.addView(makeSwitch("Auto Print Receipts"))

        // CREATE TEST PRINTER BUTTON
        val testPrinterButton = Button(context)

        // SET BUTTON LABEL
        testPrinterButton.text = "Test Printer"

        // ADD BUTTON TO THE SETTINGS PAGE
        layout.addView(testPrinterButton)

        // RETURN THE COMPLETED SETTINGS PAGE LAYOUT
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

        // SET TITLE FONT SIZE
        title.textSize = 28f

        // SET TITLE COLOR
        title.setTextColor(Color.BLACK)

        // ALIGN TEXT TO START/LEFT
        title.gravity = Gravity.START

        // ADD SPACE BELOW TITLE
        title.setPadding(0, 0, 0, dp(20))

        // RETURN COMPLETED TITLE COMPONENT
        return title
    }

    /*
     * makeSwitch()
     *
     * Creates a reusable styled switch component.
     *
     * This version is only visual right now.
     * It does not save the ON/OFF state.
     */
    private fun makeSwitch(text: String): Switch {

        // CREATE SWITCH COMPONENT
        val settingSwitch = Switch(context)

        // SET SWITCH LABEL
        settingSwitch.text = text

        // SET TEXT SIZE
        settingSwitch.textSize = 18f

        // SET TEXT COLOR
        settingSwitch.setTextColor(Color.DKGRAY)

        // ADD VERTICAL SPACING AROUND THE SWITCH
        settingSwitch.setPadding(0, dp(10), 0, dp(10))

        // RETURN COMPLETED SWITCH
        return settingSwitch
    }

    /*
     * makeFunctionalSwitch()
     *
     * Creates a switch that saves its ON/OFF value
     * using SharedPreferences.
     *
     * This is used for the first 4 required toggles:
     * - Colorblind Mode
     * - Dark Mode
     * - Low Stock Alerts
     * - Sales Alerts
     */
    private fun makeFunctionalSwitch(text: String, settingKey: String): Switch {

        // CREATE SWITCH COMPONENT
        val settingSwitch = Switch(context)

        // SET SWITCH LABEL
        settingSwitch.text = text

        // SET TEXT SIZE
        settingSwitch.textSize = 18f

        // SET TEXT COLOR
        settingSwitch.setTextColor(Color.DKGRAY)

        // ADD VERTICAL SPACING AROUND THE SWITCH
        settingSwitch.setPadding(0, dp(10), 0, dp(10))

        /*
         * LOAD SAVED VALUE
         *
         * When the Settings page opens, this checks SharedPreferences
         * to see if the switch was previously saved as ON or OFF.
         *
         * If no saved value exists yet, it defaults to false.
         */
        settingSwitch.isChecked = prefs.getBoolean(settingKey, false)

        /*
         * SAVE VALUE WHEN SWITCH CHANGES
         *
         * Every time the user taps the switch, we save the new value.
         *
         * isChecked will be:
         * true  = switch is ON
         * false = switch is OFF
         */
        settingSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit()
                .putBoolean(settingKey, isChecked)
                .apply()
        }

        // RETURN COMPLETED FUNCTIONAL SWITCH
        return settingSwitch
    }

    /*
     * dp()
     *
     * Converts density-independent pixels (dp)
     * into actual screen pixels based on the device density.
     *
     * This keeps spacing consistent across different screen sizes.
     */
    private fun dp(value: Int): Int {

        return (value * context.resources.displayMetrics.density).toInt()
    }
}