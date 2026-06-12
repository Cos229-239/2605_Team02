package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.os.Environment
import android.view.Gravity
import android.widget.*
import com.google.firebase.Timestamp
import com.liquor.ledger.firebase.FirebaseManager
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import android.provider.Settings

class SalesReportPage(private val activity: Activity) {

    private val allSales = mutableListOf<SaleRow>()
    private lateinit var reportText: TextView
    private lateinit var filterSpinner: Spinner

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
        root.setBackgroundColor(Color.WHITE)

        val title = TextView(activity)
        title.text = "Sales Report"
        title.textSize = 28f
        title.setTextColor(Color.BLACK)

        filterSpinner = Spinner(activity)
        filterSpinner.adapter = ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("30d", "60d", "90d", "YTD", "All")
        )

        val exportReportBtn = Button(activity)
        exportReportBtn.text = "Export This Report to Excel"

        val exportFullBtn = Button(activity)
        exportFullBtn.text = "Full Data Export"

        reportText = TextView(activity)
        reportText.textSize = 18f
        reportText.setTextColor(Color.BLACK)
        reportText.gravity = Gravity.START
        reportText.setPadding(0, 30, 0, 0)

        root.addView(title)
        root.addView(filterSpinner)
        root.addView(exportReportBtn)
        root.addView(exportFullBtn)
        root.addView(reportText)

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
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
                reportText.text = "Failed to load sales report."
            }
    }

    private fun updateReport() {
        val filtered = getFilteredSales()
        val totalSales = filtered.sumOf { it.total }
        val totalTransactions = filtered.size
        val averageSale = if (totalTransactions > 0) totalSales / totalTransactions else 0.0

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
}