package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.liquor.ledger.firebase.FirebaseManager

class InventoryAlertPage(private val activity: Activity) {

    // Reads saved settings from SettingsPage
    private val prefs = activity.getSharedPreferences("settings_prefs", Activity.MODE_PRIVATE)

    private val KEY_COLORBLIND_MODE = "colorblind_mode"
    private val KEY_DARK_MODE = "dark_mode"

    fun build(): LinearLayout {

        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(getPageBackgroundColor())
        root.setPadding(40, 40, 40, 40)

        val title = TextView(activity)
        title.text = "Inventory Alert Report"
        title.textSize = 28f
        title.setTextColor(getPrimaryTextColor())

        val subtitle = TextView(activity)
        subtitle.text = "Products with 10 or fewer units in stock"
        subtitle.textSize = 18f
        subtitle.setTextColor(getSecondaryTextColor())
        subtitle.setPadding(0, 15, 0, 30)

        val summaryRow = LinearLayout(activity)
        summaryRow.orientation = LinearLayout.HORIZONTAL

        val lowStockCard = makeSummaryCard("Low Stock", "0")
        val outOfStockCard = makeSummaryCard("Out of Stock", "0")
        val totalCheckedCard = makeSummaryCard("Products Checked", "0")

        summaryRow.addView(lowStockCard)
        summaryRow.addView(outOfStockCard)
        summaryRow.addView(totalCheckedCard)

        val actionRow = LinearLayout(activity)
        actionRow.orientation = LinearLayout.HORIZONTAL
        actionRow.setPadding(0, 0, 0, 24)

        val exportAlertsButton = TextView(activity)
        exportAlertsButton.text = "Export Alerts"
        exportAlertsButton.textSize = 16f
        exportAlertsButton.gravity = Gravity.CENTER
        exportAlertsButton.setTextColor(Color.WHITE)
        exportAlertsButton.setBackgroundColor(getPositiveColor())
        exportAlertsButton.setPadding(0, 16, 0, 16)

        val createPoButton = TextView(activity)
        createPoButton.text = "Create Purchase Order"
        createPoButton.textSize = 16f
        createPoButton.gravity = Gravity.CENTER
        createPoButton.setTextColor(Color.WHITE)
        createPoButton.setBackgroundColor(getPrimaryActionColor())
        createPoButton.setPadding(0, 16, 0, 16)

        val actionButtonParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        actionButtonParams.setMargins(6, 0, 6, 0)

        actionRow.addView(exportAlertsButton, actionButtonParams)
        actionRow.addView(createPoButton, actionButtonParams)

        createPoButton.setOnClickListener {
            createPurchaseOrderFromAlerts()
        }

        val scrollView = ScrollView(activity)
        scrollView.setBackgroundColor(getPageBackgroundColor())

        val content = LinearLayout(activity)
        content.orientation = LinearLayout.VERTICAL
        content.setBackgroundColor(getPageBackgroundColor())

        scrollView.addView(content)

        root.addView(title)
        root.addView(subtitle)
        root.addView(summaryRow)
        root.addView(scrollView)
        root.addView(actionRow)

        FirebaseManager.db.collection("products")
            .addSnapshotListener { snapshot, error ->

                content.removeAllViews()

                if (error != null) {
                    val errorText = TextView(activity)
                    errorText.text = "Error loading inventory alerts: ${error.message}"
                    errorText.textSize = 18f
                    errorText.setTextColor(getNegativeColor())

                    content.addView(errorText)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    val emptyText = TextView(activity)
                    emptyText.text = "No products found."
                    emptyText.textSize = 18f
                    emptyText.setTextColor(getSecondaryTextColor())

                    content.addView(emptyText)
                    return@addSnapshotListener
                }

                var alertCount = 0
                var outOfStockCount = 0
                var totalChecked = 0

                for (doc in snapshot.documents) {

                    val name = doc.getString("name") ?: "Unknown Product"
                    totalChecked++

                    val sku = doc.getString("sku") ?: "—"
                    val category = doc.getString("category") ?: "—"
                    val vendor = doc.getString("vendor") ?: "—"

                    val stock = when (val stockField = doc.get("stock")) {
                        is Long -> stockField.toInt()
                        is Double -> stockField.toInt()
                        is Int -> stockField
                        is String -> stockField.toIntOrNull() ?: 0
                        else -> 0
                    }

                    if (stock <= 0) {
                        outOfStockCount++
                    }

                    if (stock <= 10) {

                        alertCount++

                        val stockStatusText = when {
                            stock <= 0 -> "OUT OF STOCK"
                            stock <= 5 -> "CRITICAL STOCK"
                            else -> "LOW STOCK"
                        }

                        val stockStatusColor = when {
                            stock <= 0 -> getNegativeColor()
                            stock <= 5 -> getWarningColor()
                            else -> getLowStockColor()
                        }

                        val alertCard = LinearLayout(activity)
                        alertCard.orientation = LinearLayout.VERTICAL
                        alertCard.setBackgroundColor(getCardBackgroundColor())
                        alertCard.setPadding(24, 24, 24, 24)

                        val cardParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )

                        cardParams.setMargins(0, 0, 0, 20)

                        val nameText = TextView(activity)
                        nameText.text = name
                        nameText.textSize = 20f
                        nameText.setTextColor(getPrimaryTextColor())
                        nameText.setPadding(0, 14, 0, 8)

                        val statusText = TextView(activity)
                        statusText.text = stockStatusText
                        statusText.textSize = 14f
                        statusText.setTextColor(Color.WHITE)
                        statusText.setBackgroundColor(stockStatusColor)
                        statusText.gravity = Gravity.CENTER
                        statusText.setPadding(12, 8, 12, 8)

                        val detailsText = TextView(activity)
                        detailsText.text = """
                            SKU: $sku
                            Category: $category
                            Vendor: $vendor
                            Current Stock: $stock
                        """.trimIndent()

                        detailsText.textSize = 16f
                        detailsText.setTextColor(getSecondaryTextColor())

                        alertCard.addView(statusText)
                        alertCard.addView(nameText)
                        alertCard.addView(detailsText)

                        content.addView(alertCard, cardParams)
                    }
                }

                lowStockCard.text = "Low Stock\n$alertCount"
                outOfStockCard.text = "Out of Stock\n$outOfStockCount"
                totalCheckedCard.text = "Products Checked\n$totalChecked"

                if (alertCount == 0) {
                    val noAlerts = TextView(activity)

                    noAlerts.text =
                        "No inventory alerts. All products have more than 10 units in stock."

                    noAlerts.textSize = 18f
                    noAlerts.setTextColor(getSecondaryTextColor())

                    content.addView(noAlerts)
                }
            }

        return root
    }

    private fun makeSummaryCard(label: String, value: String): TextView {

        val card = TextView(activity)
        card.text = "$label\n$value"
        card.textSize = 18f
        card.setTextColor(getPrimaryTextColor())
        card.gravity = Gravity.CENTER
        card.setBackgroundColor(getCardBackgroundColor())
        card.setPadding(20, 20, 20, 20)

        val params = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        params.setMargins(6, 0, 6, 24)
        card.layoutParams = params

        return card
    }

    private fun createPurchaseOrderFromAlerts() {

        FirebaseManager.db.collection("products")
            .get()
            .addOnSuccessListener { snapshot ->

                val itemsByVendor = mutableMapOf<String, MutableList<HashMap<String, Any>>>()

                for (doc in snapshot.documents) {

                    val name = doc.getString("name") ?: "Unknown Product"
                    val vendor = doc.getString("vendor") ?: "Unknown Vendor"

                    val stock = when (val stockField = doc.get("stock")) {
                        is Long -> stockField.toInt()
                        is Double -> stockField.toInt()
                        is Int -> stockField
                        is String -> stockField.toIntOrNull() ?: 0
                        else -> 0
                    }

                    val reorderPoint = when (val reorderField = doc.get("reorderPoint")) {
                        is Long -> reorderField.toInt()
                        is Double -> reorderField.toInt()
                        is Int -> reorderField
                        is String -> reorderField.toIntOrNull() ?: 10
                        else -> 10
                    }

                    val cost = doc.getDouble("cost") ?: 0.0

                    if (stock <= reorderPoint) {

                        val neededQuantity = reorderPoint - stock

                        if (neededQuantity > 0) {

                            val item = hashMapOf<String, Any>(
                                "productName" to name,
                                "quantity" to neededQuantity.toLong(),
                                "costPerUnit" to cost
                            )

                            if (!itemsByVendor.containsKey(vendor)) {
                                itemsByVendor[vendor] = mutableListOf()
                            }

                            itemsByVendor[vendor]?.add(item)
                        }
                    }
                }

                if (itemsByVendor.isEmpty()) {
                    Toast.makeText(
                        activity,
                        "No low stock items need ordering",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                FirebaseManager.db.collection("purchaseOrders")
                    .get()
                    .addOnSuccessListener { poSnapshot ->

                        var nextNumber = poSnapshot.size() + 1

                        itemsByVendor.forEach { vendorEntry ->

                            val vendor = vendorEntry.key
                            val items = vendorEntry.value

                            val total = items.sumOf { item ->
                                val quantity = item["quantity"] as Long
                                val costPerUnit = item["costPerUnit"] as Double
                                quantity * costPerUnit
                            }

                            val poNumber = "PO-" + nextNumber.toString().padStart(4, '0')
                            nextNumber++

                            val newPO = hashMapOf(
                                "poNumber" to poNumber,
                                "vendor" to vendor,
                                "date" to com.google.firebase.Timestamp.now(),
                                "total" to total,
                                "status" to "pending review",
                                "notes" to "Auto-created from Inventory Alerts",
                                "items" to items
                            )

                            FirebaseManager.db.collection("purchaseOrders")
                                .add(newPO)
                        }

                        Toast.makeText(
                            activity,
                            "Purchase order draft created",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    activity,
                    "Error creating purchase order: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
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

    private fun getCardBackgroundColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(48, 48, 48)
        } else {
            Color.rgb(245, 247, 250)
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

    private fun getWarningColor(): Int {
        return Color.rgb(230, 159, 0)
    }

    private fun getLowStockColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(86, 180, 233)
        } else {
            Color.rgb(245, 190, 65)
        }
    }

    private fun getNegativeColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(213, 94, 0)
        } else {
            Color.rgb(211, 47, 47)
        }
    }
}