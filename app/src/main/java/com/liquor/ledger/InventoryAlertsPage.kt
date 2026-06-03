package com.liquor.ledger


import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.liquor.ledger.firebase.FirebaseManager


class InventoryAlertPage(private val activity: Activity) {


    fun build(): LinearLayout {


        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)
        root.setPadding(40, 40, 40, 40)


        val title = TextView(activity)
        title.text = "Inventory Alert Report"
        title.textSize = 28f
        title.setTextColor(Color.BLACK)


        val subtitle = TextView(activity)
        subtitle.text = "Products with 10 or fewer units in stock"
        subtitle.textSize = 18f
        subtitle.setTextColor(Color.DKGRAY)
        subtitle.setPadding(0, 15, 0, 30)


        val scrollView = ScrollView(activity)


        val content = LinearLayout(activity)
        content.orientation = LinearLayout.VERTICAL


        scrollView.addView(content)


        root.addView(title)
        root.addView(subtitle)
        root.addView(scrollView)


        FirebaseManager.db.collection("products")
            .addSnapshotListener { snapshot, error ->


                content.removeAllViews()


                if (error != null) {


                    val errorText = TextView(activity)
                    errorText.text = "Error loading inventory alerts: ${error.message}"
                    errorText.textSize = 18f
                    errorText.setTextColor(Color.RED)


                    content.addView(errorText)
                    return@addSnapshotListener
                }


                if (snapshot == null || snapshot.isEmpty) {


                    val emptyText = TextView(activity)
                    emptyText.text = "No products found."
                    emptyText.textSize = 18f
                    emptyText.setTextColor(Color.DKGRAY)


                    content.addView(emptyText)
                    return@addSnapshotListener
                }


                var alertCount = 0


                for (doc in snapshot.documents) {


                    val name = doc.getString("name") ?: "Unknown Product"
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


                    if (stock <= 10) {


                        alertCount++


                        val item = TextView(activity)


                        item.text = """
Product: $name
SKU: $sku
Category: $category
Vendor: $vendor
Current Stock: $stock
                       """.trimIndent()


                        item.textSize = 18f
                        item.setTextColor(Color.BLACK)
                        item.gravity = Gravity.START
                        item.setPadding(20, 20, 20, 30)


                        content.addView(item)


                        val divider = TextView(activity)
                        divider.text = "────────────────────────"


                        content.addView(divider)
                    }
                }


                if (alertCount == 0) {


                    val noAlerts = TextView(activity)


                    noAlerts.text =
                        "No inventory alerts. All products have more than 10 units in stock."


                    noAlerts.textSize = 18f
                    noAlerts.setTextColor(Color.DKGRAY)


                    content.addView(noAlerts)
                }
            }


        return root
    }
}
