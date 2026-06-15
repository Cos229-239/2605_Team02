package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class EmergencyContactsPage(private val activity: Activity) {

    // Reads saved settings from SettingsPage
    private val prefs = activity.getSharedPreferences("settings_prefs", Activity.MODE_PRIVATE)

    private val KEY_COLORBLIND_MODE = "colorblind_mode"
    private val KEY_DARK_MODE = "dark_mode"

    fun build(): ScrollView {

        val scrollView = ScrollView(activity)
        scrollView.setBackgroundColor(getPageBackgroundColor())

        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 20, 20, 20)
        root.setBackgroundColor(getPageBackgroundColor())

        val title = TextView(activity)
        title.text = "Emergency Contacts"
        title.textSize = 28f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(getPrimaryTextColor())

        val subtitle = TextView(activity)
        subtitle.text = "Important phone numbers for emergencies and support"
        subtitle.textSize = 14f
        subtitle.setTextColor(getSecondaryTextColor())
        subtitle.setPadding(0, 10, 0, 30)

        root.addView(title)
        root.addView(subtitle)

        val topRow = LinearLayout(activity)
        topRow.orientation = LinearLayout.HORIZONTAL
        topRow.setBackgroundColor(getPageBackgroundColor())

        topRow.addView(
            createSection(
                "Emergency Services",
                listOf(
                    Triple("Police Emergency", "911", "EMERGENCY"),
                    Triple("Fire Department", "911", "EMERGENCY"),
                    Triple("EMS / Ambulance", "911", "EMERGENCY"),
                    Triple("Police Non-Emergency", "(312) 744-5000", "NON-EMERGENCY"),
                    Triple("Poison Control", "1-800-222-1222", "HOTLINE")
                )
            ),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        topRow.addView(
            createSection(
                "Store Management",
                listOf(
                    Triple("Store Manager", "(312) 555-0100", "DIRECT"),
                    Triple("Assistant Manager", "(312) 555-0101", "DIRECT"),
                    Triple("District Manager", "(312) 555-0200", "DIRECT"),
                    Triple("Corporate Office", "1-800-555-0150", "MAIN"),
                    Triple("HR Department", "1-800-555-0151", "MAIN")
                )
            ),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        root.addView(topRow)

        val bottomRow = LinearLayout(activity)
        bottomRow.orientation = LinearLayout.HORIZONTAL
        bottomRow.setPadding(0, 20, 0, 0)
        bottomRow.setBackgroundColor(getPageBackgroundColor())

        bottomRow.addView(
            createSection(
                "Security & Safety",
                listOf(
                    Triple("Security Company", "(312) 555-0300", "DIRECT"),
                    Triple("Alarm Monitoring", "1-800-555-0301", "HOTLINE"),
                    Triple("Lock & Key Service", "(312) 555-0400", "SERVICE")
                )
            ),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        bottomRow.addView(
            createSection(
                "Utilities & Maintenance",
                listOf(
                    Triple("Gas Emergency", "1-800-555-0500", "EMERGENCY"),
                    Triple("Electric Emergency", "1-800-555-0501", "EMERGENCY"),
                    Triple("Water Emergency", "(312) 744-7038", "EMERGENCY"),
                    Triple("HVAC Repair", "(312) 555-0600", "SERVICE"),
                    Triple("Plumbing Service", "(312) 555-0601", "SERVICE")
                )
            ),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        root.addView(bottomRow)

        val protocolBox = LinearLayout(activity)
        protocolBox.orientation = LinearLayout.VERTICAL
        protocolBox.setPadding(30, 25, 30, 25)

        val protocolParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        protocolParams.topMargin = 25

        protocolBox.setBackgroundColor(getProtocolBackgroundColor())

        val protocolTitle = TextView(activity)
        protocolTitle.text = "⚠ EMERGENCY PROTOCOL"
        protocolTitle.textSize = 14f
        protocolTitle.setTypeface(null, Typeface.BOLD)
        protocolTitle.setTextColor(getEmergencyColor())

        val protocolText = TextView(activity)
        protocolText.text =
            "For life-threatening emergencies, always dial 911 first. Then contact store management and follow emergency procedures posted in the break room."
        protocolText.textSize = 13f
        protocolText.setTextColor(getSecondaryTextColor())

        protocolBox.addView(protocolTitle)
        protocolBox.addView(protocolText)

        root.addView(protocolBox, protocolParams)

        scrollView.addView(root)

        return scrollView
    }

    private fun createSection(
        title: String,
        contacts: List<Triple<String, String, String>>
    ): LinearLayout {

        val section = LinearLayout(activity)
        section.orientation = LinearLayout.VERTICAL
        section.setPadding(20, 20, 20, 20)
        section.setBackgroundColor(getSectionBackgroundColor())

        val params = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        params.marginEnd = 12
        section.layoutParams = params

        val sectionTitle = TextView(activity)
        sectionTitle.text = title
        sectionTitle.textSize = 18f
        sectionTitle.setTypeface(null, Typeface.BOLD)
        sectionTitle.setTextColor(getPrimaryTextColor())
        sectionTitle.setPadding(0, 0, 0, 15)

        section.addView(sectionTitle)

        contacts.forEach {
            section.addView(
                createContactCard(
                    it.first,
                    it.second,
                    it.third
                )
            )
        }

        return section
    }

    private fun createContactCard(
        name: String,
        phone: String,
        type: String
    ): LinearLayout {

        val card = LinearLayout(activity)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(25, 20, 25, 20)
        card.setBackgroundColor(getCardBackgroundColor())

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.bottomMargin = 15
        card.layoutParams = params

        val topRow = LinearLayout(activity)
        topRow.orientation = LinearLayout.HORIZONTAL

        val nameView = TextView(activity)
        nameView.text = name
        nameView.textSize = 14f
        nameView.setTextColor(getSecondaryTextColor())

        val badge = TextView(activity)
        badge.text = type
        badge.textSize = 10f
        badge.setPadding(12, 4, 12, 4)
        badge.setTextColor(getBadgeColor(type))

        val spacer = TextView(activity)
        spacer.layoutParams =
            LinearLayout.LayoutParams(0, 0, 1f)

        topRow.addView(nameView)
        topRow.addView(spacer)
        topRow.addView(badge)

        val phoneView = TextView(activity)
        phoneView.text = phone
        phoneView.textSize = 18f
        phoneView.setTypeface(null, Typeface.BOLD)
        phoneView.setTextColor(getPhoneColor())
        phoneView.setPadding(0, 10, 0, 0)

        card.addView(topRow)
        card.addView(phoneView)

        return card
    }

    private fun isDarkModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    private fun isColorblindModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_COLORBLIND_MODE, false)
    }

    private fun getPageBackgroundColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(38, 38, 38)
        } else {
            Color.WHITE
        }
    }

    private fun getSectionBackgroundColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(48, 48, 48)
        } else {
            Color.WHITE
        }
    }

    private fun getCardBackgroundColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(60, 60, 60)
        } else {
            Color.rgb(250, 250, 250)
        }
    }

    private fun getProtocolBackgroundColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(65, 45, 45)
        } else {
            Color.rgb(255, 248, 248)
        }
    }

    private fun getPrimaryTextColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    private fun getSecondaryTextColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.LTGRAY
        } else {
            Color.DKGRAY
        }
    }

    private fun getMutedTextColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(180, 180, 180)
        } else {
            Color.GRAY
        }
    }

    private fun getPrimaryActionColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(0, 114, 178)
        } else {
            Color.rgb(37, 99, 235)
        }
    }

    private fun getEmergencyColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(213, 94, 0)
        } else {
            Color.rgb(220, 38, 38)
        }
    }

    private fun getWarningColor(): Int {
        return Color.rgb(230, 159, 0)
    }

    private fun getPhoneColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(0, 114, 178)
        } else {
            Color.rgb(59, 130, 246)
        }
    }

    private fun getBadgeColor(type: String): Int {
        return when (type) {
            "EMERGENCY" -> getEmergencyColor()
            "HOTLINE" -> getWarningColor()
            "SERVICE" -> getMutedTextColor()
            else -> getPrimaryActionColor()
        }
    }
}