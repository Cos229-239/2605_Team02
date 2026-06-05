package com.liquor.ledger


import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.liquor.ledger.firebase.FirebaseManager


class InventoryReportPage(private val activity: Activity) {


    private val db: FirebaseFirestore = FirebaseManager.db
    private var inventoryListener: ListenerRegistration? = null


    private lateinit var summary: TextView


    fun build(): LinearLayout {
        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)
        root.setPadding(40, 40, 40, 40)


        val title = TextView(activity)
        title.text = "Inventory Report"
        title.textSize = 28f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(Color.BLACK)
        title.setPadding(0, 0, 0, 30)


        summary = TextView(activity)
        summary.text = "Loading inventory report..."
        summary.textSize = 20f
        summary.setTextColor(Color.BLACK)
        summary.setPadding(20, 20, 20, 20)


        root.addView(title)
        root.addView(summary)


        loadInventoryReport()


        return root
    }


    private fun loadInventoryReport() {
        inventoryListener?.remove()


        inventoryListener = db.collection("products")
            .addSnapshotListener { snapshot, error ->


                if (error != null) {
                    summary.text = "Error loading inventory report: ${error.message}"
                    return@addSnapshotListener
                }


                if (snapshot == null || snapshot.isEmpty) {
                    summary.text = """
                       Total Products: 0
                       Low Stock Items: 0
                       Out of Stock Items: 0
                       Inventory Value: $0.00
                   """.trimIndent()
                    return@addSnapshotListener
                }


                var totalProducts = 0
                var lowStockItems = 0
                var outOfStockItems = 0
                var inventoryValue = 0.0


                for (doc in snapshot.documents) {
                    val stock = doc.getLong("stock") ?: 0L
                    val reorderPoint = doc.getLong("reorderPoint") ?: 0L
                    val cost = doc.getDouble("cost") ?: 0.0


                    totalProducts++
                    inventoryValue += stock * cost


                    if (stock == 0L) {
                        outOfStockItems++
                    } else if (stock <= reorderPoint) {
                        lowStockItems++
                    }
                }


                summary.text = """
                   Total Products: $totalProducts
                   Low Stock Items: $lowStockItems
                   Out of Stock Items: $outOfStockItems
                   Inventory Value: $${"%.2f".format(inventoryValue)}
               """.trimIndent()
            }
    }
}
