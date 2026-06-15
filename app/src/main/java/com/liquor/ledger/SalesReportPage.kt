package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.Timestamp
import com.liquor.ledger.firebase.FirebaseManager
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SalesReportPage(private val activity: Activity) {

    private val allSales = mutableListOf<SaleRow>()
    private lateinit var reportText: TextView
    private lateinit var filterSpinner: Spinner

    // Reads saved settings from SettingsPage
    private val prefs = activity.getSharedPreferences("settings_prefs", Activity.MODE_PRIVATE)

    private val KEY_COLORBLIND_MODE = "colorblind_mode"
    private val KEY_DARK_MODE = "dark_mode"

    data class SaleRow(
        val date: Date,
        val total: Double,
        val paymentType: String,
        val employee: String
    )

    fun build(): LinearLayout {
        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(40, 40, 40, 40)
        root.setBackgroundColor(getPageBackgroundColor())

        val title = TextView(activity)
        title.text = "Sales Report"
        title.textSize = 28f
        title.setTextColor(getPrimaryTextColor())

        filterSpinner = Spinner(activity)
        filterSpinner.adapter = makeSpinnerAdapter(
            listOf("30d", "60d", "90d", "YTD", "All")
        )

        val exportReportBtn = Button(activity)
        exportReportBtn.text = "Export This Report to Excel"
        styleButton(exportReportBtn, getPositiveColor())

        val exportFullBtn = Button(activity)
        exportFullBtn.text = "Full Data Export"
        styleButton(exportFullBtn, getPrimaryActionColor())

        reportText = TextView(activity)
        reportText.textSize = 18f
        reportText.setTextColor(getPrimaryTextColor())
        reportText.gravity = Gravity.START
        reportText.setPadding(0, 30, 0, 0)

        root.addView(title)
        root.addView(filterSpinner)
        root.addView(exportReportBtn)
        root.addView(exportFullBtn)
        root.addView(reportText)

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                updateReport()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        exportReportBtn.setOnClickListener {
            exportSales(getFilteredSales(), "sales_report_${filterSpinner.selectedItem}.csv")
        }

        exportFullBtn.setOnClickListener {
            exportSales(allSales, "sales_full_data_export.csv")
        }

        loadSales()

        return root
    }

    private fun loadSales() {
        FirebaseManager.db.collection("sales")
            .get()
            .addOnSuccessListener { result ->
                allSales.clear()

                for (doc in result) {
                    val dateValue = doc.get("timestamp") ?: doc.get("date")

                    val date = when (dateValue) {
                        is Timestamp -> dateValue.toDate()
                        is Date -> dateValue
                        else -> Date()
                    }

                    allSales.add(
                        SaleRow(
                            date = date,
                            total = doc.getDouble("total") ?: 0.0,
                            paymentType = doc.getString("paymentType") ?: "Unknown",
                            employee = doc.getString("employee") ?: "Unknown"
                        )
                    )
                }

                updateReport()
            }
            .addOnFailureListener {
                reportText.setTextColor(getNegativeColor())
                reportText.text = "Failed to load sales report."
            }
    }

    private fun updateReport() {
        val filtered = getFilteredSales()
        val totalSales = filtered.sumOf { it.total }
        val totalTransactions = filtered.size
        val averageSale = if (totalTransactions > 0) {
            totalSales / totalTransactions
        } else {
            0.0
        }

        reportText.setTextColor(getPrimaryTextColor())

        reportText.text = """
            Filter: ${filterSpinner.selectedItem}

            Total Transactions: $totalTransactions
            Total Sales: $${"%.2f".format(totalSales)}
            Average Sale: $${"%.2f".format(averageSale)}
        """.trimIndent()
    }

    private fun getFilteredSales(): List<SaleRow> {
        val selected = filterSpinner.selectedItem?.toString() ?: "All"
        val calendar = Calendar.getInstance()

        return when (selected) {
            "30d" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                allSales.filter { it.date.after(calendar.time) }
            }

            "60d" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -60)
                allSales.filter { it.date.after(calendar.time) }
            }

            "90d" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -90)
                allSales.filter { it.date.after(calendar.time) }
            }

            "YTD" -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)

                allSales.filter { it.date.after(calendar.time) }
            }

            else -> allSales
        }
    }

    private fun exportSales(sales: List<SaleRow>, fileName: String) {
        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )

            val exportsFolder = File(downloadsFolder, "exports")

            if (!exportsFolder.exists()) {
                exportsFolder.mkdirs()
            }

            val file = File(exportsFolder, fileName)
            val writer = FileWriter(file)
            val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US)

            writer.append("Date,Total,Payment Type,Employee\n")

            for (sale in sales) {
                writer.append("${dateFormat.format(sale.date)},")
                writer.append("${"%.2f".format(sale.total)},")
                writer.append("${sale.paymentType},")
                writer.append("${sale.employee}\n")
            }

            writer.flush()
            writer.close()

            Toast.makeText(
                activity,
                "Exported to Downloads/exports/$fileName",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                activity,
                "Export failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
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
                view.textSize = 16f
                view.setPadding(16, 12, 16, 12)
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
                view.textSize = 16f
                view.setPadding(16, 12, 16, 12)
                return view
            }
        }.also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun styleButton(button: Button, backgroundColor: Int) {
        button.setTextColor(Color.WHITE)
        button.setBackgroundColor(backgroundColor)
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
            Color.rgb(20, 180, 120)
        }
    }

    private fun getNegativeColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(213, 94, 0)
        } else {
            Color.RED
        }
    }
}