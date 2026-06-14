package com.liquor.ledger

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.liquor.ledger.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// UserInfoPage shows the logged in employee's profile
// Managers can view and edit all employees
// Managers can add new employees and reset passwords
// Managers can unlock locked accounts

class UserInfoPage(private val activity: Activity) {

    private val db: FirebaseFirestore = FirebaseManager.db
    private val auth: FirebaseAuth = FirebaseManager.auth

    private val currentEmployee = SessionManager.currentEmployee
    private val isManager = currentEmployee?.position == "Manager"

    // Container for the employee list (manager view)
    private lateinit var employeeListContainer: LinearLayout

    fun build(): LinearLayout {

        val page = LinearLayout(activity)
        page.orientation = LinearLayout.VERTICAL
        page.setBackgroundColor(Color.WHITE)

        val scrollView = ScrollView(activity)
        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        val content = LinearLayout(activity)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(24), dp(24), dp(24), dp(24))

        // MY PROFILE CARD
        content.addView(makeSectionTitle("My Profile"))
        content.addView(makeProfileCard(currentEmployee))

        // MANAGER SECTION
        if (isManager) {
            // ADD NEW EMPLOYEE BUTTON
            val addBtn = makeActionButton(
                "+ Add New Employee",
                Color.rgb(34, 197, 94)
            ) {
                showAddEmployeeDialog()
            }
            val addBtnParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addBtnParams.setMargins(0, dp(8), 0, dp(16))
            addBtn.layoutParams = addBtnParams
            content.addView(addBtn)

            // ALL EMPLOYEES SECTION
            content.addView(makeSectionTitle("All Employees"))

            employeeListContainer = LinearLayout(activity)
            employeeListContainer.orientation = LinearLayout.VERTICAL
            content.addView(employeeListContainer)

            loadAllEmployees()
        }

        scrollView.addView(content)
        page.addView(scrollView, scrollParams)

        return page
    }

    // Loads all employees from Firestore and displays them
    private fun loadAllEmployees() {
        employeeListContainer.removeAllViews()

        val loadingText = TextView(activity)
        loadingText.text = "Loading employees..."
        loadingText.textSize = 14f
        loadingText.setTextColor(Color.GRAY)
        employeeListContainer.addView(loadingText)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("employees").get().await()
                val employees = snapshot.documents.mapNotNull { doc ->
                    Employee(
                        employeeId = doc.getString("employeeId") ?: "",
                        name = doc.getString("name") ?: "",
                        position = doc.getString("position") ?: "",
                        email = doc.getString("email") ?: "",
                        uid = doc.getString("uid") ?: "",
                        docId = doc.id
                    ) to mapOf(
                        "isLocked" to (doc.getBoolean("isLocked") ?: false),
                        "failedAttempts" to (doc.getLong("failedAttempts") ?: 0L)
                    )
                }

                withContext(Dispatchers.Main) {
                    employeeListContainer.removeAllViews()

                    if (employees.isEmpty()) {
                        val emptyText = TextView(activity)
                        emptyText.text = "No employees found"
                        emptyText.textSize = 14f
                        emptyText.setTextColor(Color.GRAY)
                        employeeListContainer.addView(emptyText)
                        return@withContext
                    }

                    employees.forEach { (employee, meta) ->
                        employeeListContainer.addView(
                            makeEmployeeRow(employee, meta))

                        // Divider
                        val divider = android.view.View(activity)
                        divider.setBackgroundColor(Color.rgb(229, 231, 235))
                        employeeListContainer.addView(
                            divider,
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1))
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    employeeListContainer.removeAllViews()
                    val errorText = TextView(activity)
                    errorText.text = "Error loading employees: ${e.message}"
                    errorText.textSize = 14f
                    errorText.setTextColor(Color.RED)
                    employeeListContainer.addView(errorText)
                }
            }
        }
    }

    // Creates a profile card for the current employee
    private fun makeProfileCard(employee: Employee?): LinearLayout {
        val card = LinearLayout(activity)
        card.orientation = LinearLayout.VERTICAL
        card.setBackgroundColor(Color.rgb(248, 249, 250))
        card.setPadding(dp(20), dp(20), dp(20), dp(20))

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardParams.setMargins(0, dp(8), 0, dp(16))
        card.layoutParams = cardParams

        listOf(
            Pair("Employee ID", employee?.employeeId ?: ""),
            Pair("Name", employee?.name ?: ""),
            Pair("Position", employee?.position ?: "")
        ).forEach { (label, value) ->
            val row = LinearLayout(activity)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, dp(6), 0, dp(6))

            val labelView = TextView(activity)
            labelView.text = "$label:"
            labelView.textSize = 14f
            labelView.setTextColor(Color.GRAY)
            labelView.setTypeface(null, Typeface.BOLD)
            labelView.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val valueView = TextView(activity)
            valueView.text = value
            valueView.textSize = 14f
            valueView.setTextColor(Color.BLACK)
            valueView.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)

            row.addView(labelView)
            row.addView(valueView)
            card.addView(row)
        }

        return card
    }

    // Creates a row for each employee in the manager list
    private fun makeEmployeeRow(
        employee: Employee,
        meta: Map<String, Any>
    ): LinearLayout {

        val isLocked = meta["isLocked"] as? Boolean ?: false

        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(16), dp(14), dp(16), dp(14))
        row.setBackgroundColor(
            if (isLocked) Color.rgb(255, 245, 245) else Color.WHITE)

        // Employee info
        val infoColumn = LinearLayout(activity)
        infoColumn.orientation = LinearLayout.VERTICAL
        infoColumn.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val nameText = TextView(activity)
        nameText.text = employee.name
        nameText.textSize = 15f
        nameText.setTextColor(Color.BLACK)
        nameText.setTypeface(null, Typeface.BOLD)

        val detailText = TextView(activity)
        detailText.text = "${employee.employeeId} • ${employee.position}"
        detailText.textSize = 13f
        detailText.setTextColor(Color.GRAY)

        if (isLocked) {
            val lockedText = TextView(activity)
            lockedText.text = "LOCKED"
            lockedText.textSize = 12f
            lockedText.setTextColor(Color.RED)
            lockedText.setTypeface(null, Typeface.BOLD)
            infoColumn.addView(lockedText)
        }

        infoColumn.addView(nameText)
        infoColumn.addView(detailText)
        row.addView(infoColumn)

        // Action buttons
        val btnColumn = LinearLayout(activity)
        btnColumn.orientation = LinearLayout.HORIZONTAL
        btnColumn.gravity = Gravity.CENTER_VERTICAL

        // Edit button
        val editBtn = makeSmallButton("Edit", Color.rgb(45, 95, 255))
        editBtn.setOnClickListener {
            showEditEmployeeDialog(employee)
        }
        btnColumn.addView(editBtn)

        // Set Password button
        val passwordBtn = makeSmallButton("Set Password", Color.rgb(107, 114, 128))
        passwordBtn.setOnClickListener {
            showSetPasswordDialog(employee)
        }
        val pwdParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        pwdParams.setMargins(dp(8), 0, 0, 0)
        passwordBtn.layoutParams = pwdParams
        btnColumn.addView(passwordBtn)

        // Unlock button — only shows if account is locked
        if (isLocked) {
            val unlockBtn = makeSmallButton("Unlock", Color.rgb(245, 158, 11))
            unlockBtn.setOnClickListener {
                unlockAccount(employee.docId)
            }
            val unlockParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            unlockParams.setMargins(dp(8), 0, 0, 0)
            unlockBtn.layoutParams = unlockParams
            btnColumn.addView(unlockBtn)
        }

        row.addView(btnColumn)

        return row
    }

    // Shows dialog to edit an employee's info
    private fun showEditEmployeeDialog(employee: Employee) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Edit Employee")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))

        val nameLabel = makeDialogLabel("Full Name")
        val nameInput = makeDialogInput(employee.name)

        val idLabel = makeDialogLabel("Employee ID")
        val idInput = makeDialogInput(employee.employeeId)

        val positionLabel = makeDialogLabel("Position")
        val positionSpinner = Spinner(activity)
        val positions = arrayOf("Cashier", "Manager")
        val spinnerAdapter = ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_item,
            positions
        )
        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        positionSpinner.adapter = spinnerAdapter
        positionSpinner.setSelection(
            if (employee.position == "Manager") 1 else 0)

        val spinnerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        spinnerParams.setMargins(0, 0, 0, dp(8))
        positionSpinner.layoutParams = spinnerParams

        form.addView(nameLabel)
        form.addView(nameInput)
        form.addView(idLabel)
        form.addView(idInput)
        form.addView(positionLabel)
        form.addView(positionSpinner, spinnerParams)

        builder.setView(form)

        builder.setPositiveButton("Save") { _, _ ->
            val newName = nameInput.text.toString().trim()
            val newId = idInput.text.toString().trim()
            val newPosition = positionSpinner.selectedItem.toString()

            if (newName.isEmpty() || newId.isEmpty()) {
                android.widget.Toast.makeText(
                    activity,
                    "Name and Employee ID are required",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            updateEmployee(employee.docId, newName, newId, newPosition)
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // Shows dialog to set a new password for an employee
    private fun showSetPasswordDialog(employee: Employee) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Set Password for ${employee.name}")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))

        val noteText = TextView(activity)
        noteText.text = "You can hand the tablet to the employee to set their own password."
        noteText.textSize = 13f
        noteText.setTextColor(Color.GRAY)
        noteText.setPadding(0, 0, 0, dp(12))
        form.addView(noteText)

        val passwordLabel = makeDialogLabel("New Password")
        val passwordInput = makeDialogInput("")
        // Star out the password
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD

        val confirmLabel = makeDialogLabel("Confirm Password")
        val confirmInput = makeDialogInput("")
        confirmInput.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD

        form.addView(passwordLabel)
        form.addView(passwordInput)
        form.addView(confirmLabel)
        form.addView(confirmInput)

        builder.setView(form)

        builder.setPositiveButton("Set Password") { _, _ ->
            val password = passwordInput.text.toString()
            val confirm = confirmInput.text.toString()

            if (password.isEmpty()) {
                android.widget.Toast.makeText(
                    activity, "Password cannot be empty",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            if (password != confirm) {
                android.widget.Toast.makeText(
                    activity, "Passwords do not match",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            if (password.length < 6) {
                android.widget.Toast.makeText(
                    activity, "Password must be at least 6 characters",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            setEmployeePassword(employee, password)
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // Shows dialog to add a new employee
    private fun showAddEmployeeDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Add New Employee")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))

        val nameLabel = makeDialogLabel("Full Name *")
        val nameInput = makeDialogInput("")

        val emailLabel = makeDialogLabel("Email *")
        val emailInput = makeDialogInput("")
        emailInput.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        val positionLabel = makeDialogLabel("Position")
        val positionSpinner = Spinner(activity)
        val positions = arrayOf("Cashier", "Manager")
        val spinnerAdapter = ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_item,
            positions
        )
        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        positionSpinner.adapter = spinnerAdapter

        val spinnerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        spinnerParams.setMargins(0, 0, 0, dp(8))
        positionSpinner.layoutParams = spinnerParams

        val passwordLabel = makeDialogLabel("Initial Password *")
        val passwordInput = makeDialogInput("")
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD

        val noteText = TextView(activity)
        noteText.text = "Employee ID will be auto-generated."
        noteText.textSize = 12f
        noteText.setTextColor(Color.GRAY)
        noteText.setPadding(0, dp(4), 0, 0)

        form.addView(nameLabel)
        form.addView(nameInput)
        form.addView(emailLabel)
        form.addView(emailInput)
        form.addView(positionLabel)
        form.addView(positionSpinner, spinnerParams)
        form.addView(passwordLabel)
        form.addView(passwordInput)
        form.addView(noteText)

        builder.setView(form)

        builder.setPositiveButton("Add Employee") { _, _ ->
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val position = positionSpinner.selectedItem.toString()
            val password = passwordInput.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                android.widget.Toast.makeText(
                    activity, "Name, email and password are required",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            if (password.length < 6) {
                android.widget.Toast.makeText(
                    activity, "Password must be at least 6 characters",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            createNewEmployee(name, email, position, password)
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // Updates an employee's info in Firestore
    private fun updateEmployee(
        docId: String,
        name: String,
        employeeId: String,
        position: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("employees")
                    .document(docId)
                    .update(
                        mapOf(
                            "name" to name,
                            "employeeId" to employeeId,
                            "position" to position
                        )
                    )
                    .await()

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity, "Employee updated successfully",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    loadAllEmployees()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity, "Error updating employee: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Sets a new password for an employee using Firebase Auth Admin
    private fun setEmployeePassword(employee: Employee, newPassword: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // signInWithEmailAndPassword then updatePassword

                val credential = com.google.firebase.auth.EmailAuthProvider
                    .getCredential(employee.email, newPassword)

                // Sign in as the employee temporarily to update their password
                val result = auth.signInWithEmailAndPassword(
                    employee.email, newPassword).await()

                result.user?.updatePassword(newPassword)?.await()

                // Sign back in as the manager
                val managerEmail = currentEmployee?.email ?: ""

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Password updated for ${employee.name}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Fallback — send password reset email
                    sendPasswordResetEmail(employee.email)
                }
            }
        }
    }

    // Sends a password reset email as fallback
    private fun sendPasswordResetEmail(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Password reset email sent",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Error: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Creates a new employee in Firebase Auth and Firestore
    private fun createNewEmployee(
        name: String,
        email: String,
        position: String,
        password: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Auto-generate Employee ID based on current count
                val snapshot = db.collection("employees").get().await()
                val nextNumber = snapshot.size() + 1
                val year = java.util.Calendar.getInstance()
                    .get(java.util.Calendar.YEAR)
                val employeeId = "EMP-$year-" +
                    nextNumber.toString().padStart(4, '0')

                // Create Firebase Auth account
                val authResult = auth.createUserWithEmailAndPassword(
                    email, password).await()
                val uid = authResult.user?.uid ?: ""

                // Create Firestore employee document
                val newEmployee = hashMapOf(
                    "employeeId" to employeeId,
                    "name" to name,
                    "email" to email,
                    "position" to position,
                    "uid" to uid,
                    "failedAttempts" to 0L,
                    "isLocked" to false
                )

                db.collection("employees").add(newEmployee).await()

                val managerEmail = currentEmployee?.email ?: ""

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "$employeeId created for $name. Please log back in.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    // Sign out and go back to login
                    auth.signOut()
                    SessionManager.clear()
                    val intent = android.content.Intent(
                        activity, LoginActivity::class.java)
                    intent.flags =
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    activity.startActivity(intent)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Error creating employee: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Unlocks a locked employee account
    private fun unlockAccount(docId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("employees")
                    .document(docId)
                    .update(
                        mapOf(
                            "isLocked" to false,
                            "failedAttempts" to 0L
                        )
                    )
                    .await()

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Account unlocked successfully",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    loadAllEmployees()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Error unlocking account: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Creates a section title
    private fun makeSectionTitle(text: String): TextView {
        val title = TextView(activity)
        title.text = text
        title.textSize = 18f
        title.setTextColor(Color.BLACK)
        title.setTypeface(null, Typeface.BOLD)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, dp(8))
        title.layoutParams = params
        return title
    }

    // Creates an action button
    private fun makeActionButton(
        text: String,
        color: Int,
        onClick: () -> Unit
    ): TextView {
        val btn = TextView(activity)
        btn.text = text
        btn.textSize = 14f
        btn.gravity = Gravity.CENTER
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(color)
        btn.setPadding(dp(16), dp(10), dp(16), dp(10))
        btn.setOnClickListener { onClick() }
        return btn
    }

    // Creates a small button for employee rows
    private fun makeSmallButton(text: String, color: Int): TextView {
        val btn = TextView(activity)
        btn.text = text
        btn.textSize = 12f
        btn.gravity = Gravity.CENTER
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(color)
        btn.setPadding(dp(10), dp(6), dp(10), dp(6))
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
        label.setTextColor(Color.GRAY)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, dp(10), 0, dp(4))
        label.layoutParams = params
        return label
    }

    // Creates a dialog input field
    private fun makeDialogInput(defaultValue: String): EditText {
        val input = EditText(activity)
        input.setText(defaultValue)
        input.textSize = 14f
        input.setTextColor(Color.BLACK)
        input.setPadding(dp(8), dp(8), dp(8), dp(8))
        input.setBackgroundColor(Color.rgb(243, 244, 246))
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, dp(4))
        input.layoutParams = params
        return input
    }

    // Converts dp to pixels
    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}
