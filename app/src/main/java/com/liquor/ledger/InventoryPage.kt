package com.liquor.ledger

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import com.liquor.ledger.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// InventoryPage displays all products from Firestore
// Manager roles can add new products, adjust stock, and view full inventory details

class InventoryPage(private val activity: Activity) {

    // Firestore database instance
    private val db: FirebaseFirestore = FirebaseManager.db

    // Check if current employee is a manager
    private val isManager = SessionManager.currentEmployee?.position == "Manager"

    // Container for the product list rows
    private lateinit var productListContainer: LinearLayout

    // Summary stat views
    private lateinit var totalProductsText: TextView
    private lateinit var inventoryValueText: TextView
    private lateinit var lowStockText: TextView
    private lateinit var outOfStockText: TextView

    // Current search and filter values
    private var currentSearch = ""
    private var currentCategory = "All"

    fun build(): LinearLayout {

        // ROOT layout — vertical
        val page = LinearLayout(activity)
        page.orientation = LinearLayout.VERTICAL
        page.setBackgroundColor(Color.WHITE)

        // TOP BAR — search, filter, and buttons
        val topBar = LinearLayout(activity)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(dp(16), dp(12), dp(16), dp(12))
        topBar.setBackgroundColor(Color.WHITE)

        val topBarParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Search input
        val searchInput = EditText(activity)
        searchInput.hint = "Search products..."
        searchInput.textSize = 14f
        searchInput.setTextColor(Color.BLACK)
        searchInput.setHintTextColor(Color.LTGRAY)
        searchInput.setPadding(dp(12), dp(8), dp(12), dp(8))
        searchInput.setBackgroundColor(Color.rgb(243, 244, 246))

        val searchParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            2f
        )
        searchParams.setMargins(0, 0, dp(8), 0)
        searchInput.layoutParams = searchParams

        // Listens for search input changes
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                currentSearch = s.toString().trim()
                loadProducts()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Category filter dropdown
        val categorySpinner = android.widget.Spinner(activity)
        val categories = arrayOf("All", "Alcohol", "Wine", "Beer", "Spirits", "Snacks", "Other")
        val spinnerAdapter = android.widget.ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_item,
            categories
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter

        val spinnerParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        spinnerParams.setMargins(0, 0, dp(8), 0)
        categorySpinner.layoutParams = spinnerParams

        categorySpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    currentCategory = categories[position]
                    loadProducts()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

        topBar.addView(searchInput, searchParams)
        topBar.addView(categorySpinner, spinnerParams)

        // Buttons — only show for managers
        if (isManager) {
            val adjustStockBtn = makeTopButton("Adjust Stock", Color.rgb(45, 95, 255))
            adjustStockBtn.setOnClickListener { showAdjustStockDialog() }

            val addProductBtn = makeTopButton("+ Add Product", Color.rgb(34, 197, 94))
            addProductBtn.setOnClickListener { showAddProductDialog() }

            topBar.addView(adjustStockBtn)
            topBar.addView(addProductBtn)
        }

        // SUMMARY STATS ROW
        val statsRow = LinearLayout(activity)
        statsRow.orientation = LinearLayout.HORIZONTAL
        statsRow.setPadding(dp(16), dp(8), dp(16), dp(8))
        statsRow.setBackgroundColor(Color.rgb(248, 249, 250))

        val statsParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        totalProductsText = makeStatView("Total Products", "0", Color.BLACK)
        inventoryValueText = makeStatView("Inventory Value", "$0.00", Color.rgb(45, 95, 255))
        lowStockText = makeStatView("Low Stock", "0", Color.rgb(245, 158, 11))
        outOfStockText = makeStatView("Out of Stock", "0", Color.RED)

        statsRow.addView(totalProductsText)
        statsRow.addView(inventoryValueText)
        statsRow.addView(lowStockText)
        statsRow.addView(outOfStockText)

        // TABLE HEADER
        val tableHeader = makeTableHeader()

        // SCROLLABLE PRODUCT LIST
        val scrollView = ScrollView(activity)
        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        productListContainer = LinearLayout(activity)
        productListContainer.orientation = LinearLayout.VERTICAL

        scrollView.addView(productListContainer)

        // Add everything to page
        page.addView(topBar, topBarParams)
        page.addView(statsRow, statsParams)
        page.addView(tableHeader)
        page.addView(scrollView, scrollParams)

        // Load products from Firestore
        loadProducts()

        return page
    }

    // Loads products from Firestore and displays them
    private fun loadProducts() {
        productListContainer.removeAllViews()

        // Show loading
        val loadingText = TextView(activity)
        loadingText.text = "Loading inventory..."
        loadingText.textSize = 14f
        loadingText.setTextColor(Color.GRAY)
        loadingText.setPadding(dp(16), dp(16), dp(16), dp(16))
        productListContainer.addView(loadingText)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("products").get().await()

                val allProducts = snapshot.documents.mapNotNull { doc ->
                    mapOf(
                        "docId" to doc.id,
                        "name" to (doc.getString("name") ?: ""),
                        "sku" to (doc.getString("sku") ?: "—"),
                        "category" to (doc.getString("category") ?: ""),
                        "vendor" to (doc.getString("vendor") ?: "—"),
                        "stock" to (doc.getLong("stock")?.toString() ?: "0"),
                        "reorderPoint" to (doc.getLong("reorderPoint")?.toString() ?: "0"),
                        "cost" to (doc.getDouble("cost")?.toString() ?: "0.0"),
                        "taxPercent" to (doc.getDouble("taxPercent")?.toString() ?: "0.0"),
                        "marginPercent" to (doc.getDouble("marginPercent")?.toString() ?: "0.0"),
                        "price" to (doc.getDouble("price")?.toString() ?: "0.0"),
                        "stockValue" to (
                            (doc.getLong("stock")?.toDouble() ?: 0.0) *
                                (doc.getDouble("cost") ?: 0.0)
                            ).toString()
                    )
                }

                // Apply search filter
                val searched = if (currentSearch.isEmpty()) allProducts
                else allProducts.filter { product ->
                    product["name"]?.contains(currentSearch, ignoreCase = true) == true ||
                        product["sku"]?.contains(currentSearch, ignoreCase = true) == true ||
                        product["vendor"]?.contains(currentSearch, ignoreCase = true) == true
                }

                // Apply category filter
                val filtered = if (currentCategory == "All") searched
                else searched.filter { it["category"] == currentCategory }

                // Calculate stats
                val totalProducts = allProducts.size
                val inventoryValue = allProducts.sumOf {
                    it["stockValue"]?.toDoubleOrNull() ?: 0.0
                }
                val lowStock = allProducts.count { product ->
                    val stock = product["stock"]?.toIntOrNull() ?: 0
                    val reorder = product["reorderPoint"]?.toIntOrNull() ?: 0
                    stock in 1..reorder
                }
                val outOfStock = allProducts.count { product ->
                    (product["stock"]?.toIntOrNull() ?: 0) == 0
                }

                withContext(Dispatchers.Main) {
                    // Update stats
                    totalProductsText.text = "Total Products\n$totalProducts"
                    inventoryValueText.text = "Inventory Value\n$${"%.2f".format(inventoryValue)}"
                    lowStockText.text = "Low Stock\n! $lowStock"
                    outOfStockText.text = "Out of Stock\nX $outOfStock"

                    productListContainer.removeAllViews()

                    if (filtered.isEmpty()) {
                        val emptyText = TextView(activity)
                        emptyText.text = "No products found"
                        emptyText.textSize = 14f
                        emptyText.setTextColor(Color.GRAY)
                        emptyText.setPadding(dp(16), dp(16), dp(16), dp(16))
                        productListContainer.addView(emptyText)
                    } else {
                        filtered.forEach { product ->
                            productListContainer.addView(makeProductRow(product))
                            // Add thin divider line between rows
                            val divider = android.view.View(activity)
                            divider.setBackgroundColor(Color.rgb(229, 231, 235))
                            productListContainer.addView(
                                divider,
                                LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    1
                                )
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    productListContainer.removeAllViews()
                    val errorText = TextView(activity)
                    errorText.text = "Error loading inventory: ${e.message}"
                    errorText.textSize = 14f
                    errorText.setTextColor(Color.RED)
                    errorText.setPadding(dp(16), dp(16), dp(16), dp(16))
                    productListContainer.addView(errorText)
                }
            }
        }
    }

    // Creates a single product row
    private fun makeProductRow(product: Map<String, String>): LinearLayout {

        val stock = product["stock"]?.toIntOrNull() ?: 0
        val reorderPoint = product["reorderPoint"]?.toIntOrNull() ?: 0

        // Determine stock status
        val status = when {
            stock == 0 -> "Out of Stock"
            stock <= reorderPoint -> "Low Stock"
            else -> ""
        }

        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(dp(16), dp(12), dp(16), dp(12))
        row.setBackgroundColor(Color.WHITE)
        row.gravity = Gravity.CENTER_VERTICAL

        // Product name — clickable blue link
        val nameCell = TextView(activity)
        nameCell.text = product["name"] ?: ""
        nameCell.textSize = 14f
        nameCell.setTextColor(Color.rgb(45, 95, 255))
        nameCell.layoutParams = LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        nameCell.setOnClickListener {
            if (isManager) showProductDetailDialog(product)
        }

        row.addView(nameCell)
        row.addView(makeCell(product["sku"] ?: "—", 1f))
        row.addView(makeCell(product["category"] ?: "", 1f))
        row.addView(makeCell(product["vendor"] ?: "—", 1f))

        // Stock cell — colored based on status
        val stockCell = TextView(activity)
        val stockColor = when {
            stock == 0 -> Color.RED
            stock <= reorderPoint -> Color.rgb(245, 158, 11)
            else -> Color.rgb(55, 65, 81)
        }
        val stockPrefix = when {
            stock == 0 -> "X "
            stock <= reorderPoint -> "! "
            else -> ""
        }
        stockCell.text = "$stockPrefix$stock"
        stockCell.textSize = 14f
        stockCell.setTextColor(stockColor)
        stockCell.layoutParams = LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(stockCell)

        // Reorder point — colored if stock is at or below it
        val reorderCell = TextView(activity)
        reorderCell.text = if (stock <= reorderPoint && stock > 0)
            "! $reorderPoint" else reorderPoint.toString()
        reorderCell.textSize = 14f
        reorderCell.setTextColor(
            if (stock <= reorderPoint && stock > 0)
                Color.rgb(245, 158, 11) else Color.rgb(55, 65, 81)
        )
        reorderCell.layoutParams = LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(reorderCell)

        row.addView(makeCell("$${product["cost"]}", 1f))
        row.addView(makeCell("${product["taxPercent"]}%", 1f))
        row.addView(makeCell("${product["marginPercent"]}%", 1f))
        row.addView(makeCell("$${"%.2f".format(
            product["stockValue"]?.toDoubleOrNull() ?: 0.0)}", 1f))

        // Status cell
        val statusCell = TextView(activity)
        statusCell.text = status
        statusCell.textSize = 12f
        statusCell.setTextColor(when (status) {
            "Out of Stock" -> Color.RED
            "Low Stock" -> Color.rgb(245, 158, 11)
            else -> Color.rgb(55, 65, 81)
        })
        statusCell.layoutParams = LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(statusCell)

        return row
    }

    // Shows a dialog with full product details and edit options
    private fun showProductDetailDialog(product: Map<String, String>) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(product["name"])

        val content = LinearLayout(activity)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(20), dp(10), dp(20), dp(10))

        val details = listOf(
            "SKU" to (product["sku"] ?: "—"),
            "Category" to (product["category"] ?: ""),
            "Vendor" to (product["vendor"] ?: "—"),
            "Stock" to (product["stock"] ?: "0"),
            "Reorder Point" to (product["reorderPoint"] ?: "0"),
            "Cost" to "$${product["cost"]}",
            "Tax %" to "${product["taxPercent"]}%",
            "Margin %" to "${product["marginPercent"]}%",
            "Price" to "$${product["price"]}",
            "Stock Value" to "$${"%.2f".format(
                product["stockValue"]?.toDoubleOrNull() ?: 0.0)}"
        )

        details.forEach { (label, value) ->
            val row = LinearLayout(activity)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, dp(4), 0, dp(4))

            val labelView = TextView(activity)
            labelView.text = "$label:"
            labelView.textSize = 14f
            labelView.setTextColor(Color.GRAY)
            labelView.setTypeface(null, Typeface.BOLD)
            labelView.layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val valueView = TextView(activity)
            valueView.text = value
            valueView.textSize = 14f
            valueView.setTextColor(Color.BLACK)
            valueView.layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            row.addView(labelView)
            row.addView(valueView)
            content.addView(row)
        }

        builder.setView(content)
        builder.setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // Shows dialog to add a new product
    private fun showAddProductDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Add New Product")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))

        // Form fields
        val nameInput = makeDialogInput(form, "Product Name *")
        val skuInput = makeDialogInput(form, "SKU")
        val categoryInput = makeDialogInput(form, "Category")
        val vendorInput = makeDialogInput(form, "Vendor")
        val stockInput = makeDialogInput(form, "Initial Stock", isNumber = true)
        val reorderInput = makeDialogInput(form, "Reorder Point", isNumber = true)
        val costInput = makeDialogInput(form, "Cost ($)", isDecimal = true)
        val priceInput = makeDialogInput(form, "Price ($)", isDecimal = true)
        val taxInput = makeDialogInput(form, "Tax %", isDecimal = true)
        val marginInput = makeDialogInput(form, "Margin %", isDecimal = true)

        builder.setView(form)

        builder.setPositiveButton("Add Product") { _, _ ->
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                android.widget.Toast.makeText(
                    activity, "Product name is required",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            // Write new product to Firestore
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val newProduct = hashMapOf(
                        "name" to name,
                        "sku" to skuInput.text.toString().trim(),
                        "category" to categoryInput.text.toString().trim(),
                        "vendor" to vendorInput.text.toString().trim(),
                        "stock" to (stockInput.text.toString().toLongOrNull() ?: 0L),
                        "reorderPoint" to (reorderInput.text.toString().toLongOrNull() ?: 0L),
                        "cost" to (costInput.text.toString().toDoubleOrNull() ?: 0.0),
                        "price" to (priceInput.text.toString().toDoubleOrNull() ?: 0.0),
                        "taxPercent" to (taxInput.text.toString().toDoubleOrNull() ?: 0.0),
                        "marginPercent" to (marginInput.text.toString().toDoubleOrNull() ?: 0.0)
                    )

                    db.collection("products").add(newProduct).await()

                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            activity, "$name added to inventory",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        loadProducts()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            activity, "Error: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // Shows dialog to adjust stock for a product
    private fun showAdjustStockDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Adjust Stock")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))

        val productNameInput = makeDialogInput(form, "Product Name")
        val adjustAmountInput = makeDialogInput(form, "Adjust Amount (+ or -)", isNumber = false)

        val noteText = TextView(activity)
        noteText.text = "Enter a positive number to add stock or negative to remove.\ne.g. 10 or -5"
        noteText.textSize = 12f
        noteText.setTextColor(Color.GRAY)
        noteText.setPadding(0, dp(4), 0, dp(8))
        form.addView(noteText)

        builder.setView(form)

        builder.setPositiveButton("Adjust") { _, _ ->
            val productName = productNameInput.text.toString().trim()
            val adjustAmount = adjustAmountInput.text.toString().trim().toIntOrNull()

            if (productName.isEmpty() || adjustAmount == null) {
                android.widget.Toast.makeText(
                    activity, "Please enter a valid product name and amount",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            // Find the product and update its stock
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val snapshot = db.collection("products")
                        .whereEqualTo("name", productName)
                        .get()
                        .await()

                    if (snapshot.isEmpty) {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                activity, "Product not found",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }

                    val doc = snapshot.documents[0]
                    val currentStock = doc.getLong("stock") ?: 0L
                    val newStock = maxOf(0L, currentStock + adjustAmount)

                    db.collection("products")
                        .document(doc.id)
                        .update("stock", newStock)
                        .await()

                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            activity,
                            "Stock updated: $currentStock → $newStock",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        loadProducts()
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            activity, "Error: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // Creates a table header row
    private fun makeTableHeader(): LinearLayout {
        val header = LinearLayout(activity)
        header.orientation = LinearLayout.HORIZONTAL
        header.setPadding(dp(16), dp(10), dp(16), dp(10))
        header.setBackgroundColor(Color.rgb(248, 249, 250))

        val columns = listOf(
            Pair("Product", 2f),
            Pair("SKU", 1f),
            Pair("Category", 1f),
            Pair("Vendor", 1f),
            Pair("Stock", 1f),
            Pair("Reorder Pt", 1f),
            Pair("Cost", 1f),
            Pair("Tax%", 1f),
            Pair("Margin%", 1f),
            Pair("Stock Value", 1f),
            Pair("Status", 1f)
        )

        columns.forEach { (text, weight) ->
            val cell = TextView(activity)
            cell.text = text
            cell.textSize = 12f
            cell.setTextColor(Color.rgb(107, 114, 128))
            cell.setTypeface(null, Typeface.BOLD)
            cell.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            header.addView(cell)
        }

        return header
    }

    // Creates a standard table cell
    private fun makeCell(text: String, weight: Float): TextView {
        val cell = TextView(activity)
        cell.text = text
        cell.textSize = 13f
        cell.setTextColor(Color.rgb(55, 65, 81))
        cell.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
        return cell
    }

    // Creates a stat summary view
    private fun makeStatView(label: String, value: String, color: Int): TextView {
        val view = TextView(activity)
        view.text = "$label\n$value"
        view.textSize = 13f
        view.setTextColor(color)
        view.gravity = Gravity.CENTER
        view.setPadding(dp(8), dp(8), dp(8), dp(8))
        view.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        return view
    }

    // Creates a top bar button
    private fun makeTopButton(text: String, color: Int): TextView {
        val btn = TextView(activity)
        btn.text = text
        btn.textSize = 13f
        btn.gravity = Gravity.CENTER
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(color)
        btn.setPadding(dp(12), dp(8), dp(12), dp(8))

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(dp(8), 0, 0, 0)
        btn.layoutParams = params
        return btn
    }

    // Creates an input field for dialogs
    private fun makeDialogInput(
        parent: LinearLayout,
        hint: String,
        isNumber: Boolean = false,
        isDecimal: Boolean = false
    ): EditText {
        val label = TextView(activity)
        label.text = hint
        label.textSize = 13f
        label.setTextColor(Color.GRAY)
        label.setPadding(0, dp(8), 0, dp(2))
        parent.addView(label)

        val input = EditText(activity)
        input.hint = hint
        input.textSize = 14f
        input.setTextColor(Color.BLACK)
        input.setHintTextColor(Color.LTGRAY)
        input.setPadding(dp(8), dp(8), dp(8), dp(8))
        input.setBackgroundColor(Color.rgb(243, 244, 246))
        input.inputType = when {
            isDecimal -> android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            isNumber -> android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            else -> android.text.InputType.TYPE_CLASS_TEXT
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, dp(4))
        input.layoutParams = params
        parent.addView(input)

        return input
    }

    // Converts dp to pixels
    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}
