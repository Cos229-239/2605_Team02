package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.liquor.ledger.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// TimecardReportsPage shows all employees clock in/out records
// Only accessible to managers via the Timecard page
// onBack callback is called when the back button is pressed
class TimecardReportsPage(
    private val activity: Activity,
    private val onBack: () -> Unit
) {

    private val db: FirebaseFirestore = FirebaseManager.db

    // Reads saved settings from SettingsPage
    private val prefs = activity.getSharedPreferences("settings_prefs", Activity.MODE_PRIVATE)

    private val KEY_COLORBLIND_MODE = "colorblind_mode"
    private val KEY_DARK_MODE = "dark_mode"

    private lateinit var reportContainer: LinearLayout
    private lateinit var weekRangeLabel: TextView
    private var currentWeekStart: Calendar = getWeekStart(Calendar.getInstance())

    fun build(): LinearLayout {

        val page = LinearLayout(activity)
        page.orientation = LinearLayout.VERTICAL
        page.setBackgroundColor(getPageBackgroundColor())

        // TOP BAR with back button and title
        val topBar = LinearLayout(activity)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(dp(16), dp(12), dp(16), dp(12))
        topBar.setBackgroundColor(getSectionBackgroundColor())

        val backBtn = TextView(activity)
        backBtn.text = "< Back to Timecard"
        backBtn.textSize = 14f
        backBtn.setTextColor(getPrimaryActionColor())
        backBtn.setPadding(0, 0, dp(16), 0)
        backBtn.setOnClickListener { onBack() }

        val titleText = TextView(activity)
        titleText.text = "Timecard Reports"
        titleText.textSize = 18f
        titleText.setTextColor(getPrimaryTextColor())
        titleText.setTypeface(null, Typeface.BOLD)
        titleText.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        topBar.addView(backBtn)
        topBar.addView(titleText)
        page.addView(topBar)

        val subtitle = TextView(activity)
        subtitle.text = "All employee attendance records"
        subtitle.textSize = 13f
        subtitle.setTextColor(getMutedTextColor())
        subtitle.setPadding(dp(16), dp(4), dp(16), dp(8))
        subtitle.setBackgroundColor(getPageBackgroundColor())
        page.addView(subtitle)

        // WEEK NAVIGATION
        val weekNav = LinearLayout(activity)
        weekNav.orientation = LinearLayout.HORIZONTAL
        weekNav.gravity = Gravity.CENTER_VERTICAL
        weekNav.setPadding(dp(16), dp(8), dp(16), dp(8))
        weekNav.setBackgroundColor(getPageBackgroundColor())

        val prevBtn = makeNavButton("< Prev Week")
        prevBtn.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
            loadReport()
        }

        weekRangeLabel = TextView(activity)
        weekRangeLabel.textSize = 14f
        weekRangeLabel.setTextColor(getPrimaryTextColor())
        weekRangeLabel.setTypeface(null, Typeface.BOLD)
        weekRangeLabel.gravity = Gravity.CENTER
        weekRangeLabel.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val nextBtn = makeNavButton("Next Week >")
        nextBtn.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1)
            loadReport()
        }

        weekNav.addView(prevBtn)
        weekNav.addView(weekRangeLabel)
        weekNav.addView(nextBtn)
        page.addView(weekNav)

        // TABLE HEADER
        page.addView(makeTableHeader())

        // SCROLLABLE REPORT
        val scrollView = ScrollView(activity)
        scrollView.setBackgroundColor(getPageBackgroundColor())

        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        reportContainer = LinearLayout(activity)
        reportContainer.orientation = LinearLayout.VERTICAL
        reportContainer.setBackgroundColor(getPageBackgroundColor())

        scrollView.addView(reportContainer)
        page.addView(scrollView, scrollParams)

        loadReport()

        return page
    }

    // Loads all employee timecards for the selected week
    private fun loadReport() {
        reportContainer.removeAllViews()

        val weekEnd = getWeekEnd(currentWeekStart)
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

        weekRangeLabel.text = "Week of ${dateFormat.format(currentWeekStart.time)}" +
                " - ${dateFormat.format(weekEnd.time)}" +
                ", ${yearFormat.format(weekEnd.time)}"

        val loadingText = TextView(activity)
        loadingText.text = "Loading report..."
        loadingText.textSize = 14f
        loadingText.setTextColor(getMutedTextColor())
        loadingText.setPadding(dp(16), dp(16), dp(16), dp(16))
        reportContainer.addView(loadingText)

        val weekDates = mutableListOf<String>()
        val cal = currentWeekStart.clone() as Calendar
        val dateStrFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 0..6) {
            weekDates.add(dateStrFormat.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val employeesSnapshot = db.collection("employees").get().await()

                val employees = employeesSnapshot.documents.mapNotNull { doc ->
                    Employee(
                        employeeId = doc.getString("employeeId") ?: "",
                        name = doc.getString("name") ?: "",
                        position = doc.getString("position") ?: "",
                        email = doc.getString("email") ?: "",
                        uid = doc.getString("uid") ?: ""
                    )
                }.filter { it.employeeId.isNotEmpty() }

                val timecardsByEmployee =
                    mutableMapOf<String, MutableMap<String, Map<String, Any?>>>()

                employees.forEach { employee ->
                    val empTimecards = db.collection("timecards")
                        .whereEqualTo("employeeId", employee.employeeId)
                        .get()
                        .await()

                    empTimecards.documents.forEach { doc ->
                        val date = doc.getString("date") ?: ""

                        if (date.isNotEmpty() && weekDates.contains(date)) {
                            if (!timecardsByEmployee.containsKey(employee.employeeId)) {
                                timecardsByEmployee[employee.employeeId] = mutableMapOf()
                            }

                            timecardsByEmployee[employee.employeeId]!![date] = mapOf(
                                "clockIn" to doc.getTimestamp("clockIn"),
                                "clockOut" to doc.getTimestamp("clockOut"),
                                "breakMinutes" to doc.getLong("breakMinutes"),
                                "hoursWorked" to doc.getDouble("hoursWorked"),
                                "status" to doc.getString("status")
                            )
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    reportContainer.removeAllViews()

                    if (employees.isEmpty()) {
                        val emptyText = TextView(activity)
                        emptyText.text = "No employees found"
                        emptyText.textSize = 14f
                        emptyText.setTextColor(getMutedTextColor())
                        emptyText.setPadding(dp(16), dp(16), dp(16), dp(16))

                        reportContainer.addView(emptyText)
                        return@withContext
                    }

                    employees.forEach { employee ->
                        val empTimecards = timecardsByEmployee[employee.employeeId]
                            ?: emptyMap()

                        // Employee name header row
                        val empHeader = LinearLayout(activity)
                        empHeader.orientation = LinearLayout.HORIZONTAL
                        empHeader.setPadding(dp(16), dp(12), dp(16), dp(8))
                        empHeader.setBackgroundColor(getEmployeeHeaderBackgroundColor())

                        val empName = TextView(activity)
                        empName.text = "${employee.name} (${employee.position})"
                        empName.textSize = 15f
                        empName.setTextColor(getPrimaryTextColor())
                        empName.setTypeface(null, Typeface.BOLD)
                        empName.layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )

                        val totalHours = empTimecards.values.sumOf {
                            it["hoursWorked"] as? Double ?: 0.0
                        }

                        val totalHoursText = TextView(activity)
                        totalHoursText.text = "Total: ${"%.2f".format(totalHours)} hrs"
                        totalHoursText.textSize = 14f
                        totalHoursText.setTextColor(getPrimaryActionColor())
                        totalHoursText.setTypeface(null, Typeface.BOLD)

                        empHeader.addView(empName)
                        empHeader.addView(totalHoursText)
                        reportContainer.addView(empHeader)

                        // Daily rows
                        weekDates.forEach { date ->
                            val timecard = empTimecards[date]
                            reportContainer.addView(makeDayRow(date, timecard))

                            val divider = android.view.View(activity)
                            divider.setBackgroundColor(getDividerColor())

                            reportContainer.addView(
                                divider,
                                LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    1
                                )
                            )
                        }

                        val spacer = android.view.View(activity)
                        spacer.setBackgroundColor(getSpacerColor())

                        reportContainer.addView(
                            spacer,
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                dp(4)
                            )
                        )
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    reportContainer.removeAllViews()

                    val errorText = TextView(activity)
                    errorText.text = "Error loading report: ${e.message}"
                    errorText.textSize = 14f
                    errorText.setTextColor(getNegativeColor())
                    errorText.setPadding(dp(16), dp(16), dp(16), dp(16))

                    reportContainer.addView(errorText)
                }
            }
        }
    }

    // Creates a single day row
    private fun makeDayRow(
        date: String,
        timecard: Map<String, Any?>?
    ): LinearLayout {

        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(dp(16), dp(10), dp(16), dp(10))
        row.gravity = Gravity.CENTER_VERTICAL
        row.setBackgroundColor(getPageBackgroundColor())

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val clockIn = timecard?.get("clockIn") as? Timestamp
        val clockOut = timecard?.get("clockOut") as? Timestamp
        val breakMinutes = timecard?.get("breakMinutes") as? Long ?: 0L
        val hoursWorked = timecard?.get("hoursWorked") as? Double ?: 0.0
        val status = timecard?.get("status") as? String

        val clockInStr = clockIn?.let { timeFormat.format(it.toDate()) } ?: "-"
        val clockOutStr = clockOut?.let { timeFormat.format(it.toDate()) } ?: "-"
        val breakStr = if (breakMinutes > 0) "${breakMinutes}m" else "-"
        val hoursStr = if (hoursWorked > 0) {
            "${"%.2f".format(hoursWorked)} hrs"
        } else {
            "-"
        }

        val displayStatus = when {
            status == "Completed" -> "Completed"
            status == "In Progress" -> "In Progress"
            timecard == null -> "Day Off"
            else -> "Day Off"
        }

        val statusColor = when (displayStatus) {
            "Completed" -> getPositiveColor()
            "In Progress" -> getPrimaryActionColor()
            else -> getMutedTextColor()
        }

        row.addView(makeCell(date, 2f, getSecondaryTextColor()))
        row.addView(makeCell(clockInStr, 1f, getSecondaryTextColor()))
        row.addView(makeCell(clockOutStr, 1f, getSecondaryTextColor()))
        row.addView(makeCell(breakStr, 1f, getSecondaryTextColor()))
        row.addView(makeCell(hoursStr, 1f, getSecondaryTextColor()))
        row.addView(makeCell(displayStatus, 1f, statusColor))

        return row
    }

    // Creates the table header
    private fun makeTableHeader(): LinearLayout {
        val header = LinearLayout(activity)
        header.orientation = LinearLayout.HORIZONTAL
        header.setPadding(dp(16), dp(10), dp(16), dp(10))
        header.setBackgroundColor(getSectionBackgroundColor())

        listOf(
            Pair("Date", 2f),
            Pair("Clock In", 1f),
            Pair("Clock Out", 1f),
            Pair("Break", 1f),
            Pair("Hours", 1f),
            Pair("Status", 1f)
        ).forEach { (text, weight) ->
            val cell = TextView(activity)
            cell.text = text
            cell.textSize = 12f
            cell.setTextColor(getMutedTextColor())
            cell.setTypeface(null, Typeface.BOLD)
            cell.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                weight
            )

            header.addView(cell)
        }

        return header
    }

    // Creates a table cell
    private fun makeCell(text: String, weight: Float, color: Int): TextView {
        val cell = TextView(activity)
        cell.text = text
        cell.textSize = 13f
        cell.setTextColor(color)
        cell.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            weight
        )

        return cell
    }

    // Creates a nav button
    private fun makeNavButton(text: String): TextView {
        val btn = TextView(activity)
        btn.text = text
        btn.textSize = 14f
        btn.gravity = Gravity.CENTER
        btn.setTextColor(getPrimaryActionColor())
        btn.setPadding(dp(12), dp(8), dp(12), dp(8))
        btn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        return btn
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
            Color.rgb(248, 249, 250)
        }
    }

    private fun getEmployeeHeaderBackgroundColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(55, 55, 55)
        } else {
            Color.rgb(237, 242, 255)
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
            Color.rgb(55, 65, 81)
        }
    }

    private fun getMutedTextColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(180, 180, 180)
        } else {
            Color.GRAY
        }
    }

    private fun getDividerColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(80, 80, 80)
        } else {
            Color.rgb(229, 231, 235)
        }
    }

    private fun getSpacerColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(90, 90, 90)
        } else {
            Color.rgb(200, 200, 200)
        }
    }

    private fun getPrimaryActionColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(0, 114, 178)
        } else {
            Color.rgb(45, 95, 255)
        }
    }

    private fun getPositiveColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(0, 114, 178)
        } else {
            Color.rgb(34, 197, 94)
        }
    }

    private fun getNegativeColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(213, 94, 0)
        } else {
            Color.RED
        }
    }

    // Gets the Monday of the week
    private fun getWeekStart(cal: Calendar): Calendar {
        val result = cal.clone() as Calendar
        result.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        result.set(Calendar.HOUR_OF_DAY, 0)
        result.set(Calendar.MINUTE, 0)
        result.set(Calendar.SECOND, 0)
        result.set(Calendar.MILLISECOND, 0)
        return result
    }

    // Gets the Sunday of the week
    private fun getWeekEnd(weekStart: Calendar): Calendar {
        val result = weekStart.clone() as Calendar
        result.add(Calendar.DAY_OF_WEEK, 6)
        return result
    }

    // Converts dp to pixels
    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}