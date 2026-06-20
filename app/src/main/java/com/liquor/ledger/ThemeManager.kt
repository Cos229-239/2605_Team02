package com.liquor.ledger

// Imports Needed for Theme Colors
import android.app.Activity
import android.graphics.Color

/*
 * ThemeManager
 *
 * Central place for Dark Mode and Colorblind Mode colors.
 */
object ThemeManager {

    // SHARED PREFERENCES FILE USED BY SETTINGS PAGE
    private const val PREFS_NAME = "settings_prefs"

    // SETTING KEY FOR COLORBLIND MODE
    private const val KEY_COLORBLIND_MODE = "colorblind_mode"

    // SETTING KEY FOR DARK MODE
    private const val KEY_DARK_MODE = "dark_mode"

    // CHECKS IF DARK MODE IS ENABLED
    fun isDarkMode(activity: Activity): Boolean {
        return activity
            .getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)
    }

    // CHECKS IF COLORBLIND MODE IS ENABLED
    fun isColorblindMode(activity: Activity): Boolean {
        return activity
            .getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
            .getBoolean(KEY_COLORBLIND_MODE, false)
    }

    // MAIN PAGE BACKGROUND COLOR
    fun pageBackground(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.rgb(38, 38, 38)
        } else {
            Color.WHITE
        }
    }

    // SECTION OR TABLE HEADER BACKGROUND COLOR
    fun sectionBackground(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.rgb(48, 48, 48)
        } else {
            Color.rgb(248, 249, 250)
        }
    }

    // INPUT FIELD BACKGROUND COLOR
    fun inputBackground(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.rgb(60, 60, 60)
        } else {
            Color.rgb(243, 244, 246)
        }
    }

    // MAIN TEXT COLOR
    fun primaryText(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    // SECONDARY TEXT COLOR
    fun secondaryText(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.LTGRAY
        } else {
            Color.rgb(55, 65, 81)
        }
    }

    // MUTED TEXT OR HINT TEXT COLOR
    fun mutedText(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.rgb(180, 180, 180)
        } else {
            Color.GRAY
        }
    }

    // PRIMARY BUTTON OR LINK COLOR
    fun primaryAction(activity: Activity): Int {
        return if (isColorblindMode(activity)) {
            Color.rgb(0, 114, 178)
        } else {
            Color.rgb(45, 95, 255)
        }
    }
}