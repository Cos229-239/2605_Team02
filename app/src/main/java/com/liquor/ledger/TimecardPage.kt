package com.liquor.ledger

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
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

// TimecardPage shows the weekly timesheet for the current employee
// Managers can also view and edit other employees timesheets
// via a dropdown at the top of the screen
class TimecardPage(private val activity: Activity) {

    // Firestore instance
    private val db: FirebaseFirestore = FirebaseManager.db

    // Reads saved settings from SettingsPage
    private val prefs = activity.getSharedPreferences("settings_prefs", Activity.MODE_PRIVATE)

    private val KEY_COLORBLIND_MODE = "colorblind_mode"
    private val KEY_DARK_MODE = "dark_mode"

    // Current logged in employee
    private val currentEmployee = SessionManager.currentEmployee

    // Is the current user a manager
    private val isManager = currentEmployee?.position == "Manager"

    // The employee currently being viewed
    private var viewingEmployee: Employee? = currentEmployee

    // List of all employees for the manager dropdown
    private var allEmployees: List<Employee> = emptyList()

    // The timesheet table container
    private lateinit var timesheetContainer: LinearLayout

    // The clock in/out/break buttons
    private lateinit var clockInBtn: TextView
    private lateinit var clockOutBtn: TextView
    private lateinit var breakBtn: TextView

    // Week navigation
    private var currentWeekStart: Calendar = getWeekStart(Calendar.getInstance())

    // Week range label
    private lateinit var weekRangeLabel: TextView

    // Total hours label
    private lateinit var totalHoursLabel: TextView

    // Current active timecard document ID
    private var activeTimecardId: String? = null

    // Is the employee currently on break
    private var onBreak = false

    fun build(): LinearLayout {

        val page = LinearLayout(activity)
        page.orientation = LinearLayout.VERTICAL
        page.setBackgroundColor(getPageBackgroundColor())

        // TOP SECTION — employee selector for managers
        if (isManager) {
            val selectorRow = LinearLayout(activity)
            selectorRow.orientation = LinearLayout.HORIZONTAL
            selectorRow.gravity = Gravity.CENTER_VERTICAL
            selectorRow.setPadding(dp(16), dp(12), dp(16), dp(12))
            selectorRow.setBackgroundColor(getSectionBackgroundColor())

            val selectorLabel = TextView(activity)
            selectorLabel.text = "Viewing: "
            selectorLabel.textSize = 14f
            selectorLabel.setTextColor(getSecondaryTextColor())

            val employeeSpinner = Spinner(activity)
            val spinnerParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            employeeSpinner.layoutParams = spinnerParams

            selectorRow.addView(selectorLabel)
            selectorRow.addView(employeeSpinner, spinnerParams)
            page.addView(selectorRow)

            loadEmployeesForDropdown(employeeSpinner)
        }

        // WEEK NAVIGATION ROW
        val weekNav = LinearLayout(activity)
        weekNav.orientation = LinearLayout.HORIZONTAL
        weekNav.gravity = Gravity.CENTER_VERTICAL
        weekNav.setPadding(dp(16), dp(12), dp(16), dp(12))
        weekNav.setBackgroundColor(getPageBackgroundColor())

        val prevWeekBtn = makeNavButton("< Prev")
        prevWeekBtn.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
            loadTimesheet()
        }

        weekRangeLabel = TextView(activity)
        weekRangeLabel.textSize = 15f
        weekRangeLabel.setTextColor(getPrimaryTextColor())
        weekRangeLabel.setTypeface(null, Typeface.BOLD)
        weekRangeLabel.gravity = Gravity.CENTER
        weekRangeLabel.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val nextWeekBtn = makeNavButton("Next >")
        nextWeekBtn.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1)
            loadTimesheet()
        }

        weekNav.addView(prevWeekBtn)
        weekNav.addView(weekRangeLabel)
        weekNav.addView(nextWeekBtn)
        page.addView(weekNav)

        // TOTAL HOURS ROW
        totalHoursLabel = TextView(activity)
        totalHoursLabel.text = "Total Hours This Week: 0.00 hrs"
        totalHoursLabel.textSize = 14f
        totalHoursLabel.setTextColor(getPrimaryActionColor())
        totalHoursLabel.setTypeface(null, Typeface.BOLD)
        totalHoursLabel.setPadding(dp(16), dp(8), dp(16), dp(8))
        totalHoursLabel.gravity = Gravity.END
        totalHoursLabel.setBackgroundColor(getPageBackgroundColor())
        page.addView(totalHoursLabel)

        // TABLE HEADER
        page.addView(makeTableHeader())

        // SCROLLABLE TIMESHEET
        val scrollView = ScrollView(activity)
        scrollView.setBackgroundColor(getPageBackgroundColor())

        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        timesheetContainer = LinearLayout(activity)
        timesheetContainer.orientation = LinearLayout.VERTICAL
        timesheetContainer.setBackgroundColor(getPageBackgroundColor())

        scrollView.addView(timesheetContainer)
        page.addView(scrollView, scrollParams)

        // TIMECARD REPORTS BUTTON — managers only
        if (isManager) {
            val reportsBtn = TextView(activity)
            reportsBtn.text = "View Timecard Reports"
            reportsBtn.textSize = 14f
            reportsBtn.gravity = Gravity.CENTER
            reportsBtn.setTextColor(Color.WHITE)
            reportsBtn.setBackgroundColor(getPrimaryActionColor())
            reportsBtn.setPadding(dp(16), dp(12), dp(16), dp(12))

            val reportsBtnParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            reportsBtnParams.setMargins(dp(16), dp(8), dp(16), dp(4))
            reportsBtn.layoutParams = reportsBtnParams

            reportsBtn.setOnClickListener {
                val parent = page.parent as? LinearLayout
                if (parent != null) {
                    parent.removeAllViews()
                    val reportsPage = TimecardReportsPage(activity) {
                        parent.removeAllViews()
                        parent.addView(build())
                    }
                    parent.addView(reportsPage.build())
                }
            }

            page.addView(reportsBtn)
        }

        // CLOCK IN/OUT/BREAK BUTTONS
        val buttonRow = LinearLayout(activity)
        buttonRow.orientation = LinearLayout.HORIZONTAL
        buttonRow.setPadding(dp(16), dp(12), dp(16), dp(12))
        buttonRow.setBackgroundColor(getSectionBackgroundColor())

        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        clockInBtn = makeClockButton("Clock In", getPositiveColor())
        clockInBtn.setOnClickListener { clockIn() }

        clockOutBtn = makeClockButton("Clock Out", getNegativeColor())
        clockOutBtn.setOnClickListener { clockOut() }

        breakBtn = makeClockButton("Break Out", getWarningColor())
        breakBtn.setOnClickListener { toggleBreak() }

        buttonRow.addView(clockInBtn)
        buttonRow.addView(clockOutBtn)
        buttonRow.addView(breakBtn)

        page.addView(buttonRow, buttonParams)

        loadTimesheet()

        return page
    }

    // Loads all employees into the manager dropdown
    private fun loadEmployeesForDropdown(spinner: Spinner) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("employees").get().await()
                val employees = snapshot.documents.mapNotNull { doc ->
                    Employee(
                        employeeId = doc.getString("employeeId") ?: "",
                        name = doc.getString("name") ?: "",
                        position = doc.getString("position") ?: "",
                        email = doc.getString("email") ?: "",
                        uid = doc.getString("uid") ?: ""
                    )
                }

                withContext(Dispatchers.Main) {
                    allEmployees = employees
                    val names = employees.map { it.name }
                    val adapter = makeSpinnerAdapter(names)
                    spinner.adapter = adapter

                    val currentIndex = employees.indexOfFirst {
                        it.employeeId == currentEmployee?.employeeId
                    }

                    if (currentIndex >= 0) {
                        spinner.setSelection(currentIndex)
                    }

                    spinner.onItemSelectedListener =
                        object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: android.widget.AdapterView<*>?,
                                view: android.view.View?,
                                position: Int,
                                id: Long
                            ) {
                                viewingEmployee = employees[position]
                                loadTimesheet()
                            }

                            override fun onNothingSelected(
                                parent: android.widget.AdapterView<*>?
                            ) {
                            }
                        }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadTimesheet()
                }
            }
        }
    }

    // Loads the timesheet for the current week and employee
    private fun loadTimesheet() {
        timesheetContainer.removeAllViews()

        val weekEnd = getWeekEnd(currentWeekStart)

        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

        weekRangeLabel.text = "Week of ${dateFormat.format(currentWeekStart.time)}" +
                " - ${dateFormat.format(weekEnd.time)}" +
                ", ${yearFormat.format(weekEnd.time)}"

        val loadingText = TextView(activity)
        loadingText.text = "Loading..."
        loadingText.textSize = 14f
        loadingText.setTextColor(getMutedTextColor())
        loadingText.setPadding(dp(16), dp(16), dp(16), dp(16))
        timesheetContainer.addView(loadingText)

        val employeeId = viewingEmployee?.employeeId ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("timecards")
                    .whereEqualTo("employeeId", employeeId)
                    .get()
                    .await()

                val timecardMap = mutableMapOf<String, Map<String, Any?>>()

                snapshot.documents.forEach { doc ->
                    val date = doc.getString("date") ?: ""

                    if (date.isNotEmpty()) {
                        timecardMap[date] = mapOf(
                            "docId" to doc.id,
                            "clockIn" to doc.getTimestamp("clockIn"),
                            "clockOut" to doc.getTimestamp("clockOut"),
                            "breakStart" to doc.getTimestamp("breakStart"),
                            "breakEnd" to doc.getTimestamp("breakEnd"),
                            "breakMinutes" to doc.getLong("breakMinutes"),
                            "hoursWorked" to doc.getDouble("hoursWorked"),
                            "status" to doc.getString("status")
                        )
                    }
                }

                val todayStr = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(Calendar.getInstance().time)

                val todayCard = timecardMap[todayStr]
                val hasClockIn = todayCard?.get("clockIn") != null
                val hasClockOut = todayCard?.get("clockOut") != null
                val hasBreakStart = todayCard?.get("breakStart") != null
                val hasBreakEnd = todayCard?.get("breakEnd") != null

                activeTimecardId = todayCard?.get("docId") as? String
                onBreak = hasBreakStart && !hasBreakEnd

                withContext(Dispatchers.Main) {
                    timesheetContainer.removeAllViews()

                    updateButtonStates(hasClockIn, hasClockOut)

                    var totalMinutes = 0.0

                    val cal = currentWeekStart.clone() as Calendar

                    for (i in 0..6) {
                        val dateStr = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                        ).format(cal.time)

                        val dayName = SimpleDateFormat(
                            "EEEE",
                            Locale.getDefault()
                        ).format(cal.time)

                        val displayDate = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                        ).format(cal.time)

                        val timecard = timecardMap[dateStr]
                        val row = makeTimesheetRow(displayDate, dayName, timecard)

                        timesheetContainer.addView(row)

                        val hours = timecard?.get("hoursWorked") as? Double ?: 0.0
                        totalMinutes += hours * 60

                        val divider = android.view.View(activity)
                        divider.setBackgroundColor(getDividerColor())

                        timesheetContainer.addView(
                            divider,
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                1
                            )
                        )

                        cal.add(Calendar.DAY_OF_WEEK, 1)
                    }

                    val totalHours = totalMinutes / 60
                    totalHoursLabel.text =
                        "Total Hours This Week: ${"%.2f".format(totalHours)} hrs"
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    timesheetContainer.removeAllViews()

                    val errorText = TextView(activity)
                    errorText.text = "Error loading timesheet: ${e.message}"
                    errorText.textSize = 14f
                    errorText.setTextColor(getNegativeColor())
                    errorText.setPadding(dp(16), dp(16), dp(16), dp(16))

                    timesheetContainer.addView(errorText)
                }
            }
        }
    }

    // Creates a single timesheet row for a day
    private fun makeTimesheetRow(
        date: String,
        dayName: String,
        timecard: Map<String, Any?>?
    ): LinearLayout {

        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(dp(16), dp(12), dp(16), dp(12))
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
        val hoursStr = if (hoursWorked > 0) "${"%.2f".format(hoursWorked)} hrs" else "-"

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

        row.addView(makeRowCell(date, 2f))
        row.addView(makeRowCell(dayName, 1f))
        row.addView(makeRowCell(clockInStr, 1f))
        row.addView(makeRowCell(clockOutStr, 1f))
        row.addView(makeRowCell(breakStr, 1f))
        row.addView(makeRowCell(hoursStr, 1f))

        val statusCell = TextView(activity)
        statusCell.text = displayStatus
        statusCell.textSize = 13f
        statusCell.setTextColor(statusColor)
        statusCell.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        row.addView(statusCell)

        if (isManager && timecard != null) {
            row.isClickable = true
            row.isFocusable = true

            row.setOnClickListener {
                showEditTimecardDialog(
                    timecard["docId"] as? String ?: "",
                    date,
                    clockIn,
                    clockOut,
                    timecard["breakStart"] as? Timestamp,
                    timecard["breakEnd"] as? Timestamp
                )
            }
        }

        return row
    }

    // Clock in the current employee
    private fun clockIn() {
        val employeeId = viewingEmployee?.employeeId ?: return
        val employeeName = viewingEmployee?.name ?: ""

        val today = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Calendar.getInstance().time)

        val dayName = SimpleDateFormat(
            "EEEE",
            Locale.getDefault()
        ).format(Calendar.getInstance().time)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existing = db.collection("timecards")
                    .whereEqualTo("employeeId", employeeId)
                    .whereEqualTo("date", today)
                    .get()
                    .await()

                if (!existing.isEmpty) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            activity,
                            "Already clocked in today",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val newTimecard = hashMapOf(
                    "employeeId" to employeeId,
                    "employeeName" to employeeName,
                    "date" to today,
                    "dayOfWeek" to dayName,
                    "clockIn" to Timestamp.now(),
                    "clockOut" to null,
                    "breakStart" to null,
                    "breakEnd" to null,
                    "breakMinutes" to 0L,
                    "hoursWorked" to 0.0,
                    "status" to "In Progress"
                )

                val docRef = db.collection("timecards").add(newTimecard).await()
                activeTimecardId = docRef.id

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Clocked in successfully",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()

                    loadTimesheet()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Error clocking in: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Clock out the current employee
    private fun clockOut() {
        val docId = activeTimecardId ?: run {
            android.widget.Toast.makeText(
                activity,
                "No active clock in found",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = db.collection("timecards")
                    .document(docId)
                    .get()
                    .await()

                val clockInTime = doc.getTimestamp("clockIn")
                val breakMinutes = doc.getLong("breakMinutes") ?: 0L
                val clockOutTime = Timestamp.now()

                val totalMinutes = if (clockInTime != null) {
                    val diff = clockOutTime.toDate().time - clockInTime.toDate().time
                    (diff / 1000 / 60).toDouble()
                } else {
                    0.0
                }

                val netMinutes = totalMinutes - breakMinutes
                val hoursWorked = netMinutes / 60

                db.collection("timecards")
                    .document(docId)
                    .update(
                        mapOf(
                            "clockOut" to clockOutTime,
                            "hoursWorked" to hoursWorked,
                            "status" to "Completed"
                        )
                    )
                    .await()

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Clocked out. Hours worked: ${"%.2f".format(hoursWorked)}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()

                    loadTimesheet()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Error clocking out: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Toggle break in/out
    private fun toggleBreak() {
        val docId = activeTimecardId ?: run {
            android.widget.Toast.makeText(
                activity,
                "No active clock in found",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!onBreak) {
                    db.collection("timecards")
                        .document(docId)
                        .update("breakStart", Timestamp.now())
                        .await()

                    onBreak = true

                    withContext(Dispatchers.Main) {
                        breakBtn.text = "Break In"
                        breakBtn.setBackgroundColor(getPositiveColor())

                        android.widget.Toast.makeText(
                            activity,
                            "Break started",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {
                    val doc = db.collection("timecards")
                        .document(docId)
                        .get()
                        .await()

                    val breakStart = doc.getTimestamp("breakStart")
                    val breakEnd = Timestamp.now()
                    val existingBreakMinutes = doc.getLong("breakMinutes") ?: 0L

                    val newBreakMinutes = if (breakStart != null) {
                        val diff = breakEnd.toDate().time - breakStart.toDate().time
                        existingBreakMinutes + (diff / 1000 / 60)
                    } else {
                        existingBreakMinutes
                    }

                    db.collection("timecards")
                        .document(docId)
                        .update(
                            mapOf(
                                "breakEnd" to breakEnd,
                                "breakMinutes" to newBreakMinutes
                            )
                        )
                        .await()

                    onBreak = false

                    withContext(Dispatchers.Main) {
                        breakBtn.text = "Break Out"
                        breakBtn.setBackgroundColor(getWarningColor())

                        android.widget.Toast.makeText(
                            activity,
                            "Break ended. Total break: ${newBreakMinutes} min",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()

                        loadTimesheet()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Error toggling break: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Shows a dialog for managers to edit a timecard entry
    private fun showEditTimecardDialog(
        docId: String,
        date: String,
        currentClockIn: Timestamp?,
        currentClockOut: Timestamp?,
        currentBreakStart: Timestamp?,
        currentBreakEnd: Timestamp?
    ) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Edit Timecard - $date")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))
        form.setBackgroundColor(getPageBackgroundColor())

        val clockInLabel = makeDialogLabel("Clock In (HH:mm)")
        val clockInInput = makeDialogInput(
            currentClockIn?.let { timeFormat.format(it.toDate()) } ?: ""
        )

        val clockOutLabel = makeDialogLabel("Clock Out (HH:mm)")
        val clockOutInput = makeDialogInput(
            currentClockOut?.let { timeFormat.format(it.toDate()) } ?: ""
        )

        val breakStartLabel = makeDialogLabel("Break Start (HH:mm)")
        val breakStartInput = makeDialogInput(
            currentBreakStart?.let { timeFormat.format(it.toDate()) } ?: ""
        )

        val breakEndLabel = makeDialogLabel("Break End (HH:mm)")
        val breakEndInput = makeDialogInput(
            currentBreakEnd?.let { timeFormat.format(it.toDate()) } ?: ""
        )

        form.addView(clockInLabel)
        form.addView(clockInInput)
        form.addView(clockOutLabel)
        form.addView(clockOutInput)
        form.addView(breakStartLabel)
        form.addView(breakStartInput)
        form.addView(breakEndLabel)
        form.addView(breakEndInput)

        builder.setView(form)

        builder.setPositiveButton("Save") { _, _ ->
            saveEditedTimecard(
                docId,
                date,
                clockInInput.text.toString(),
                clockOutInput.text.toString(),
                breakStartInput.text.toString(),
                breakEndInput.text.toString()
            )
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    // Saves the edited timecard to Firestore
    private fun saveEditedTimecard(
        docId: String,
        date: String,
        clockInStr: String,
        clockOutStr: String,
        breakStartStr: String,
        breakEndStr: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                fun parseTime(timeStr: String): Timestamp? {
                    return if (timeStr.isNotEmpty()) {
                        try {
                            val dateValue = dateFormat.parse("$date $timeStr")
                            dateValue?.let { Timestamp(it) }
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }

                val clockIn = parseTime(clockInStr)
                val clockOut = parseTime(clockOutStr)
                val breakStart = parseTime(breakStartStr)
                val breakEnd = parseTime(breakEndStr)

                val breakMinutes = if (breakStart != null && breakEnd != null) {
                    val diff = breakEnd.toDate().time - breakStart.toDate().time
                    diff / 1000 / 60
                } else {
                    0L
                }

                val hoursWorked = if (clockIn != null && clockOut != null) {
                    val totalMinutes = (clockOut.toDate().time -
                            clockIn.toDate().time) / 1000 / 60
                    (totalMinutes - breakMinutes).toDouble() / 60
                } else {
                    0.0
                }

                val status = if (clockIn != null && clockOut != null) {
                    "Completed"
                } else {
                    "In Progress"
                }

                db.collection("timecards")
                    .document(docId)
                    .update(
                        mapOf(
                            "clockIn" to clockIn,
                            "clockOut" to clockOut,
                            "breakStart" to breakStart,
                            "breakEnd" to breakEnd,
                            "breakMinutes" to breakMinutes,
                            "hoursWorked" to hoursWorked,
                            "status" to status
                        )
                    )
                    .await()

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Timecard updated successfully",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()

                    loadTimesheet()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Error updating timecard: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Updates clock in/out/break button states
    private fun updateButtonStates(hasClockIn: Boolean, hasClockOut: Boolean) {
        val isCurrentWeek = isCurrentWeek(currentWeekStart)

        if (!isCurrentWeek) {
            clockInBtn.isEnabled = false
            clockOutBtn.isEnabled = false
            breakBtn.isEnabled = false

            clockInBtn.alpha = 0.5f
            clockOutBtn.alpha = 0.5f
            breakBtn.alpha = 0.5f

            return
        }

        clockInBtn.isEnabled = !hasClockIn
        clockOutBtn.isEnabled = hasClockIn && !hasClockOut
        breakBtn.isEnabled = hasClockIn && !hasClockOut

        clockInBtn.alpha = if (!hasClockIn) 1f else 0.5f
        clockOutBtn.alpha = if (hasClockIn && !hasClockOut) 1f else 0.5f
        breakBtn.alpha = if (hasClockIn && !hasClockOut) 1f else 0.5f

        breakBtn.text = if (onBreak) "Break In" else "Break Out"
        breakBtn.setBackgroundColor(
            if (onBreak) {
                getPositiveColor()
            } else {
                getWarningColor()
            }
        )
    }

    // Gets the Monday of the week containing the given date
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

    // Checks if the given week start is the current week
    private fun isCurrentWeek(weekStart: Calendar): Boolean {
        val currentWeek = getWeekStart(Calendar.getInstance())

        return weekStart.get(Calendar.WEEK_OF_YEAR) ==
                currentWeek.get(Calendar.WEEK_OF_YEAR) &&
                weekStart.get(Calendar.YEAR) == currentWeek.get(Calendar.YEAR)
    }

    // Creates the table header
    private fun makeTableHeader(): LinearLayout {
        val header = LinearLayout(activity)
        header.orientation = LinearLayout.HORIZONTAL
        header.setPadding(dp(16), dp(10), dp(16), dp(10))
        header.setBackgroundColor(getSectionBackgroundColor())

        listOf(
            Pair("Date", 2f),
            Pair("Day", 1f),
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

    // Creates a standard row cell
    private fun makeRowCell(text: String, weight: Float): TextView {
        val cell = TextView(activity)
        cell.text = text
        cell.textSize = 13f
        cell.setTextColor(getSecondaryTextColor())
        cell.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            weight
        )

        return cell
    }

    // Creates a clock in/out/break button
    private fun makeClockButton(text: String, color: Int): TextView {
        val btn = TextView(activity)
        btn.text = text
        btn.textSize = 15f
        btn.gravity = Gravity.CENTER
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(color)
        btn.setPadding(dp(16), dp(14), dp(16), dp(14))

        val params = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        params.setMargins(dp(4), 0, dp(4), 0)
        btn.layoutParams = params

        return btn
    }

    // Creates a nav button for week navigation
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

    // Creates a dialog label
    private fun makeDialogLabel(text: String): TextView {
        val label = TextView(activity)
        label.text = text
        label.textSize = 13f
        label.setTextColor(getMutedTextColor())
        label.setPadding(0, dp(8), 0, dp(2))

        return label
    }

    // Creates a dialog input field
    private fun makeDialogInput(defaultValue: String): android.widget.EditText {
        val input = android.widget.EditText(activity)
        input.setText(defaultValue)
        input.textSize = 14f
        input.setTextColor(getPrimaryTextColor())
        input.setHintTextColor(getMutedTextColor())
        input.setPadding(dp(8), dp(8), dp(8), dp(8))
        input.setBackgroundColor(getInputBackgroundColor())

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, dp(4))
        input.layoutParams = params

        return input
    }

    private fun makeSpinnerAdapter(items: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(
            activity,
            android.R.layout.simple_spinner_item,
            items
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(getPrimaryTextColor())
                view.setBackgroundColor(getInputBackgroundColor())
                view.textSize = 14f
                view.setPadding(dp(8), dp(8), dp(8), dp(8))
                return view
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(getPrimaryTextColor())
                view.setBackgroundColor(getInputBackgroundColor())
                view.textSize = 14f
                view.setPadding(dp(8), dp(8), dp(8), dp(8))
                return view
            }
        }.also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
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

    private fun getInputBackgroundColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(60, 60, 60)
        } else {
            Color.rgb(243, 244, 246)
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

    private fun getWarningColor(): Int {
        return Color.rgb(230, 159, 0)
    }

    private fun getNegativeColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(213, 94, 0)
        } else {
            Color.rgb(239, 68, 68)
        }
    }

    // Converts dp to pixels
    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}