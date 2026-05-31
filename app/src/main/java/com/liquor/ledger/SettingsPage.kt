package com.liquor.ledger

// IMPORTS NEEDED FOR UI COMPONENTS
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/*
 * SettingsPage
 *
 * This class builds the Settings screen programmatically.
 * It is not an Activity. MainActivity creates this page and adds it
 * into the main content area.
 */
class SettingsPage(private val context: Context) {

    /*
     * SharedPreferences
     *
     * Saves small app settings locally on the device.
     * Each toggle is stored as true or false.
     */
    private val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    /*
     * Setting Keys
     *
     * These keys are used to save and load each setting.
     */
    private val KEY_COLORBLIND_MODE = "colorblind_mode"
    private val KEY_DARK_MODE = "dark_mode"
    private val KEY_LOW_STOCK_ALERTS = "low_stock_alerts"
    private val KEY_SALES_ALERTS = "sales_alerts"
    private val KEY_SOUND_NOTIFICATIONS = "sound_notifications"
    private val KEY_AUTO_PRINT_RECEIPTS = "auto_print_receipts"

    /*
     * build()
     *
     * Creates the Settings page layout and returns it to MainActivity.
     */
    fun build(): LinearLayout {

        // MAIN VERTICAL CONTAINER
        val layout = LinearLayout(context)

        // STACK ITEMS TOP TO BOTTOM
        layout.orientation = LinearLayout.VERTICAL

        // BUILD THE SETTINGS UI INTO THE LAYOUT
        buildSettingsContent(layout)

        // RETURN COMPLETED SETTINGS PAGE
        return layout
    }

    /*
     * buildSettingsContent()
     *
     * Builds or rebuilds the contents of the Settings page.
     *
     * We use this so Dark Mode and Colorblind Mode can refresh the UI
     * immediately after the user toggles them.
     */
    private fun buildSettingsContent(layout: LinearLayout) {

        // CLEAR OLD VIEWS BEFORE REBUILDING
        layout.removeAllViews()

        // READ CURRENT THEME SETTINGS
        val darkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false)

        // APPLY PAGE BACKGROUND
        if (darkModeEnabled) {
            layout.setBackgroundColor(Color.rgb(30, 30, 30))
        } else {
            layout.setBackgroundColor(Color.WHITE)
        }

        // ADD INNER PADDING
        layout.setPadding(dp(20), dp(20), dp(20), dp(20))

        // PAGE TITLE
        layout.addView(makeTitle("Settings"))

        /*
         * FUNCTIONAL SETTINGS TOGGLES
         *
         * Every toggle now:
         * 1. Loads its saved value
         * 2. Saves when changed
         * 3. Runs its related behavior
         */
        layout.addView(
            makeFunctionalSwitch(
                "Colorblind Mode",
                KEY_COLORBLIND_MODE,
                layout
            )
        )

        layout.addView(
            makeFunctionalSwitch(
                "Dark Mode",
                KEY_DARK_MODE,
                layout
            )
        )

        layout.addView(
            makeFunctionalSwitch(
                "Low Stock Alerts",
                KEY_LOW_STOCK_ALERTS,
                layout
            )
        )

        layout.addView(
            makeFunctionalSwitch(
                "Sales Alerts",
                KEY_SALES_ALERTS,
                layout
            )
        )

        layout.addView(
            makeFunctionalSwitch(
                "Sound Notifications",
                KEY_SOUND_NOTIFICATIONS,
                layout
            )
        )

        layout.addView(
            makeFunctionalSwitch(
                "Auto Print Receipts",
                KEY_AUTO_PRINT_RECEIPTS,
                layout
            )
        )

        // TEST PRINTER BUTTON
        val testPrinterButton = Button(context)
        testPrinterButton.text = "Test Printer"

        // APPLY COLORBLIND-FRIENDLY BUTTON COLOR IF ENABLED
        if (prefs.getBoolean(KEY_COLORBLIND_MODE, false)) {
            testPrinterButton.setBackgroundColor(Color.rgb(0, 114, 178)) // accessible blue
        }

        /*
         * TEST PRINTER FUNCTIONALITY
         *
         * This does not connect to a real printer yet.
         * It simulates the behavior and respects the saved settings.
         */
        testPrinterButton.setOnClickListener {
            testPrinter()
        }

        // ADD BUTTON TO PAGE
        layout.addView(testPrinterButton)
    }

    /*
     * makeTitle()
     *
     * Creates and styles the page title.
     */
    private fun makeTitle(text: String): TextView {

        // CHECK DARK MODE
        val darkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false)

        // CREATE TITLE TEXTVIEW
        val title = TextView(context)

        // SET TITLE TEXT
        title.text = text

        // SET TITLE SIZE
        title.textSize = 28f

        // SET TITLE COLOR BASED ON DARK MODE
        if (darkModeEnabled) {
            title.setTextColor(Color.WHITE)
        } else {
            title.setTextColor(Color.BLACK)
        }

        // ALIGN TITLE TO START/LEFT
        title.gravity = Gravity.START

        // ADD SPACE BELOW TITLE
        title.setPadding(0, 0, 0, dp(20))

        return title
    }

    /*
     * makeFunctionalSwitch()
     *
     * Creates a switch that:
     * - Loads its saved ON/OFF value
     * - Saves its new value when changed
     * - Runs extra behavior based on which setting changed
     */
    private fun makeFunctionalSwitch(
        text: String,
        settingKey: String,
        parentLayout: LinearLayout
    ): Switch {

        // READ CURRENT SETTINGS
        val darkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false)
        val colorblindModeEnabled = prefs.getBoolean(KEY_COLORBLIND_MODE, false)

        // CREATE SWITCH COMPONENT
        val settingSwitch = Switch(context)

        // SET SWITCH LABEL
        settingSwitch.text = text

        // SET TEXT SIZE
        settingSwitch.textSize = 18f

        // SET TEXT COLOR BASED ON DARK MODE
        if (darkModeEnabled) {
            settingSwitch.setTextColor(Color.WHITE)
        } else {
            settingSwitch.setTextColor(Color.DKGRAY)
        }

        /*
         * COLORBLIND MODE VISUAL CHANGE
         *
         * When Colorblind Mode is enabled, we make the text use
         * a stronger blue accent instead of relying on red/green meaning.
         */
        if (colorblindModeEnabled && !darkModeEnabled) {
            settingSwitch.setTextColor(Color.rgb(0, 90, 150))
        }

        // ADD VERTICAL SPACING
        settingSwitch.setPadding(0, dp(10), 0, dp(10))

        /*
         * LOAD SAVED VALUE
         *
         * If the setting was previously ON, the switch appears ON.
         * If it was previously OFF or never saved, it appears OFF.
         */
        settingSwitch.isChecked = prefs.getBoolean(settingKey, false)

        /*
         * SAVE VALUE WHEN SWITCH CHANGES
         *
         * Every time the user taps the switch, save true or false.
         */
        settingSwitch.setOnCheckedChangeListener { _, isChecked ->

            // SAVE NEW VALUE
            prefs.edit()
                .putBoolean(settingKey, isChecked)
                .apply()

            // RUN FUNCTIONALITY FOR THIS SPECIFIC SETTING
            handleSettingChanged(settingKey, isChecked, parentLayout)
        }

        return settingSwitch
    }

    /*
     * handleSettingChanged()
     *
     * Runs extra behavior after a setting is toggled.
     */
    private fun handleSettingChanged(
        settingKey: String,
        isChecked: Boolean,
        parentLayout: LinearLayout
    ) {
        when (settingKey) {

            KEY_COLORBLIND_MODE -> {
                showToast(
                    if (isChecked) {
                        "Colorblind Mode enabled"
                    } else {
                        "Colorblind Mode disabled"
                    }
                )

                // Rebuild Settings page so colors update immediately.
                buildSettingsContent(parentLayout)
            }

            KEY_DARK_MODE -> {
                showToast(
                    if (isChecked) {
                        "Dark Mode enabled"
                    } else {
                        "Dark Mode disabled"
                    }
                )

                // Rebuild Settings page so background/text update immediately.
                buildSettingsContent(parentLayout)
            }

            KEY_LOW_STOCK_ALERTS -> {
                showToast(
                    if (isChecked) {
                        "Low Stock Alerts enabled"
                    } else {
                        "Low Stock Alerts disabled"
                    }
                )
            }

            KEY_SALES_ALERTS -> {
                showToast(
                    if (isChecked) {
                        "Sales Alerts enabled"
                    } else {
                        "Sales Alerts disabled"
                    }
                )
            }

            KEY_SOUND_NOTIFICATIONS -> {
                showToast(
                    if (isChecked) {
                        "Sound Notifications enabled"
                    } else {
                        "Sound Notifications disabled"
                    }
                )

                if (isChecked) {
                    playNotificationSound()
                }
            }

            KEY_AUTO_PRINT_RECEIPTS -> {
                showToast(
                    if (isChecked) {
                        "Auto Print Receipts enabled"
                    } else {
                        "Auto Print Receipts disabled"
                    }
                )
            }
        }
    }

    /*
     * testPrinter()
     *
     * Simulates printer behavior for now.
     *
     * Later, this is where you would connect to:
     * - Bluetooth printer
     * - Network printer
     * - Receipt printer SDK
     */
    private fun testPrinter() {

        val autoPrintEnabled = prefs.getBoolean(KEY_AUTO_PRINT_RECEIPTS, false)
        val soundEnabled = prefs.getBoolean(KEY_SOUND_NOTIFICATIONS, false)

        if (soundEnabled) {
            playNotificationSound()
        }

        if (autoPrintEnabled) {
            showToast("Auto Print is ON: sending test receipt...")
        } else {
            showToast("Printer test started")
        }
    }

    /*
     * playNotificationSound()
     *
     * Plays a short beep if Sound Notifications are enabled.
     */
    private fun playNotificationSound() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            showToast("Unable to play notification sound")
        }
    }

    /*
     * showToast()
     *
     * Displays a short popup message to the user.
     */
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /*
     * dp()
     *
     * Converts density-independent pixels into actual pixels.
     */
    private fun dp(value: Int): Int {

        return (value * context.resources.displayMetrics.density).toInt()
    }
}