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

    // Reads saved settings from SettingsPage
    private val prefs = activity.getSharedPreferences("settings_prefs", Activity.MODE_PRIVATE)

    private val KEY_COLORBLIND_MODE = "colorblind_mode"
    private val KEY_DARK_MODE = "dark_mode"

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
        page.setBackgroundColor(getPageBackgroundColor())

        // TOP BAR — search, filter, and buttons
        val topBar = LinearLayout(activity)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(dp(16), dp(12), dp(16), dp(12))
        topBar.setBackgroundColor(getPageBackgroundColor())

        val topBarParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Search input
        val searchInput = EditText(activity)
        searchInput.hint = "Search products..."
        searchInput.textSize = 14f
        searchInput.setTextColor(getPrimaryTextColor())
        searchInput.setHintTextColor(getMutedTextColor())
        searchInput.setPadding(dp(12), dp(8), dp(12), dp(8))
        searchInput.setBackgroundColor(getInputBackgroundColor())

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
            val adjustStockBtn = makeTopButton("Adjust Stock", getPrimaryActionColor())
            adjustStockBtn.setOnClickListener { showAdjustStockDialog() }

            val addProductBtn = makeTopButton("+ Add Product", getPositiveColor())
            addProductBtn.setOnClickListener { showAddProductDialog() }

            topBar.addView(adjustStockBtn)
            topBar.addView(addProductBtn)
        }

        // SUMMARY STATS ROW
        val statsRow = LinearLayout(activity)
        statsRow.orientation = LinearLayout.HORIZONTAL
        statsRow.setPadding(dp(16), dp(8), dp(16), dp(8))
        statsRow.setBackgroundColor(getSectionBackgroundColor())

        val statsParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        totalProductsText = makeStatView("Total Products", "0", getPrimaryTextColor())
        inventoryValueText = makeStatView("Inventory Value", "$0.00", getLinkColor())
        lowStockText = makeStatView("Low Stock", "0", getWarningColor())
        outOfStockText = makeStatView("Out of Stock", "0", getNegativeColor())

        statsRow.addView(totalProductsText)
        statsRow.addView(inventoryValueText)
        statsRow.addView(lowStockText)
        statsRow.addView(outOfStockText)

        // TABLE HEADER
        val tableHeader = makeTableHeader()

        // SCROLLABLE PRODUCT LIST
        val scrollView = ScrollView(activity)
        scrollView.setBackgroundColor(getPageBackgroundColor())

        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        productListContainer = LinearLayout(activity)
        productListContainer.orientation = LinearLayout.VERTICAL
        productListContainer.setBackgroundColor(getPageBackgroundColor())

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
        loadingText.setTextColor(getMutedTextColor())
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
                val searched = if (currentSearch.isEmpty()) {
                    allProducts
                } else {
                    allProducts.filter { product ->
                        product["name"]?.contains(currentSearch, ignoreCase = true) == true ||
                                product["sku"]?.contains(currentSearch, ignoreCase = true) == true ||
                                product["vendor"]?.contains(currentSearch, ignoreCase = true) == true
                    }
                }

                // Apply category filter
                val filtered = if (currentCategory == "All") {
                    searched
                } else {
                    searched.filter { it["category"] == currentCategory }
                }

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
                        emptyText.setTextColor(getMutedTextColor())
                        emptyText.setPadding(dp(16), dp(16), dp(16), dp(16))
                        productListContainer.addView(emptyText)
                    } else {
                        filtered.forEach { product ->
                            productListContainer.addView(makeProductRow(product))

                            // Add thin divider line between rows
                            val divider = android.view.View(activity)
                            divider.setBackgroundColor(getDividerColor())

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
                    errorText.setTextColor(getNegativeColor())
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
        row.setBackgroundColor(getPageBackgroundColor())
        row.gravity = Gravity.CENTER_VERTICAL

        // Product name — clickable link
        val nameCell = TextView(activity)
        nameCell.text = product["name"] ?: ""
        nameCell.textSize = 14f
        nameCell.setTextColor(getLinkColor())
        nameCell.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            2f
        )

        nameCell.setOnClickListener {
            if (isManager) showEditProductDialog(product)
        }

        row.addView(nameCell)
        row.addView(makeCell(product["sku"] ?: "—", 1f))
        row.addView(makeCell(product["category"] ?: "", 1f))
        row.addView(makeCell(product["vendor"] ?: "—", 1f))

        // Stock cell — colored based on status
        val stockCell = TextView(activity)

        val stockColor = when {
            stock == 0 -> getNegativeColor()
            stock <= reorderPoint -> getWarningColor()
            else -> getSecondaryTextColor()
        }

        val stockPrefix = when {
            stock == 0 -> "X "
            stock <= reorderPoint -> "! "
            else -> ""
        }

        stockCell.text = "$stockPrefix$stock"
        stockCell.textSize = 14f
        stockCell.setTextColor(stockColor)
        stockCell.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        row.addView(stockCell)

        // Reorder point — colored if stock is at or below it
        val reorderCell = TextView(activity)

        reorderCell.text = if (stock <= reorderPoint && stock > 0) {
            "! $reorderPoint"
        } else {
            reorderPoint.toString()
        }

        reorderCell.textSize = 14f

        reorderCell.setTextColor(
            if (stock <= reorderPoint && stock > 0) {
                getWarningColor()
            } else {
                getSecondaryTextColor()
            }
        )

        reorderCell.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        row.addView(reorderCell)

        row.addView(makeCell("$${product["cost"]}", 1f))
        row.addView(makeCell("${product["taxPercent"]}%", 1f))
        row.addView(makeCell("${product["marginPercent"]}%", 1f))

        row.addView(
            makeCell(
                "$${"%.2f".format(product["stockValue"]?.toDoubleOrNull() ?: 0.0)}",
                1f
            )
        )

        // Status cell
        val statusCell = TextView(activity)
        statusCell.text = status
        statusCell.textSize = 12f

        statusCell.setTextColor(
            when (status) {
                "Out of Stock" -> getNegativeColor()
                "Low Stock" -> getWarningColor()
                else -> getSecondaryTextColor()
            }
        )

        statusCell.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        row.addView(statusCell)

        return row
    }

    // Shows a dialog with full product details and edit options
    // Shows a dialog allowing managers to edit an existing product's details
    private fun showEditProductDialog(product: Map<String, String>) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Edit Product")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))

        val nameInput = makeDialogInput(form, "Product Name", product["name"] ?: "")
        val skuInput = makeDialogInput(form, "SKU", product["sku"]?.takeIf { it != "—" } ?: "")
        val categoryInput = makeDialogInput(form, "Category", product["category"] ?: "")
        val vendorInput = makeDialogInput(form, "Vendor", product["vendor"]?.takeIf { it != "—" } ?: "")
        val reorderInput = makeDialogInput(form, "Reorder Point",
            product["reorderPoint"] ?: "0", isNumber = true)
        val costInput = makeDialogInput(form, "Cost ($)",
            product["cost"] ?: "0.0", isDecimal = true)
        val priceInput = makeDialogInput(form, "Price ($)",
            product["price"] ?: "0.0", isDecimal = true)
        val taxInput = makeDialogInput(form, "Tax %",
            product["taxPercent"] ?: "0.0", isDecimal = true)
        val marginInput = makeDialogInput(form, "Margin %",
            product["marginPercent"] ?: "0.0", isDecimal = true)

        builder.setView(form)

        builder.setPositiveButton("Save Changes") { _, _ ->
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                android.widget.Toast.makeText(
                    activity, "Product name is required",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val updates = mapOf(
                        "name" to name,
                        "sku" to skuInput.text.toString().trim(),
                        "category" to categoryInput.text.toString().trim(),
                        "vendor" to vendorInput.text.toString().trim(),
                        "reorderPoint" to (reorderInput.text.toString().toLongOrNull() ?: 0L),
                        "cost" to (costInput.text.toString().toDoubleOrNull() ?: 0.0),
                        "price" to (priceInput.text.toString().toDoubleOrNull() ?: 0.0),
                        "taxPercent" to (taxInput.text.toString().toDoubleOrNull() ?: 0.0),
                        "marginPercent" to (marginInput.text.toString().toDoubleOrNull() ?: 0.0)
                    )

                    db.collection("products")
                        .document(product["docId"] ?: "")
                        .update(updates)
                        .await()

                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            activity, "$name updated successfully",
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
                    activity,
                    "Product name is required",
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
                            activity,
                            "$name added to inventory",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()

                        loadProducts()
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

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // Shows dialog to adjust stock for a product
    // Shows dialog to adjust stock for a product using a dropdown
    private fun showAdjustStockDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Adjust Stock")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))

        val productLabel = makeFormLabelForDialog("Select Product")
        val productSpinner = android.widget.Spinner(activity)

        val spinnerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        spinnerParams.setMargins(0, 0, 0, dp(8))
        productSpinner.layoutParams = spinnerParams

        val adjustLabel = makeFormLabelForDialog("Adjust Amount (+ or -)")
        val adjustAmountInput = EditText(activity)
        adjustAmountInput.hint = "e.g. 10 or -5"
        adjustAmountInput.textSize = 14f
        adjustAmountInput.setTextColor(Color.BLACK)
        adjustAmountInput.setHintTextColor(Color.LTGRAY)
        adjustAmountInput.setPadding(dp(8), dp(8), dp(8), dp(8))
        adjustAmountInput.setBackgroundColor(Color.rgb(243, 244, 246))
        adjustAmountInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
            android.text.InputType.TYPE_NUMBER_FLAG_SIGNED

        val noteText = TextView(activity)
        noteText.text = "Enter a positive number to add stock or negative to remove."
        noteText.textSize = 12f
        noteText.setTextColor(Color.GRAY)
        noteText.setPadding(0, dp(4), 0, dp(8))

        form.addView(productLabel)
        form.addView(productSpinner, spinnerParams)
        form.addView(adjustLabel)
        form.addView(adjustAmountInput)
        form.addView(noteText)

        builder.setView(form)

        builder.setPositiveButton("Adjust", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()

        // Load products into the spinner
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("products").get().await()
                val products = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: ""
                    val currentStock = doc.getLong("stock") ?: 0L
                    if (name.isNotEmpty()) Pair(name, currentStock) else null
                }

                withContext(Dispatchers.Main) {
                    val productNames = products.map { "${it.first} (current: ${it.second})" }
                    val adapter = android.widget.ArrayAdapter(
                        activity,
                        android.R.layout.simple_spinner_item,
                        productNames
                    )
                    adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item)
                    productSpinner.adapter = adapter

                    dialog.show()

                    // Override positive button so dialog doesn't auto-close on error
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val selectedIndex = productSpinner.selectedItemPosition
                        if (selectedIndex < 0 || products.isEmpty()) {
                            android.widget.Toast.makeText(
                                activity, "Please select a product",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        val selectedProductName = products[selectedIndex].first
                        val adjustAmount = adjustAmountInput.text.toString()
                            .trim().toIntOrNull()

                        if (adjustAmount == null) {
                            android.widget.Toast.makeText(
                                activity, "Please enter a valid amount",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        adjustProductStock(selectedProductName, adjustAmount)
                        dialog.dismiss()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity, "Error loading products: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                }
            }
        }
    }

    // Performs the actual stock adjustment in Firestore
    private fun adjustProductStock(productName: String, adjustAmount: Int) {
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
                        "Stock updated: $currentStock -> $newStock",
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

    // Creates a table header row
    private fun makeTableHeader(): LinearLayout {
        val header = LinearLayout(activity)
        header.orientation = LinearLayout.HORIZONTAL
        header.setPadding(dp(16), dp(10), dp(16), dp(10))
        header.setBackgroundColor(getSectionBackgroundColor())

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

    // Creates a standard table cell
    private fun makeCell(text: String, weight: Float): TextView {
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

    // Creates a stat summary view
    private fun makeStatView(label: String, value: String, color: Int): TextView {
        val view = TextView(activity)
        view.text = "$label\n$value"
        view.textSize = 13f
        view.setTextColor(color)
        view.gravity = Gravity.CENTER
        view.setPadding(dp(8), dp(8), dp(8), dp(8))
        view.
        layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

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
        defaultValue: String = "",
        isNumber: Boolean = false,
        isDecimal: Boolean = false
    ): EditText {
        val label = TextView(activity)
        label.text = hint
        label.textSize = 13f
        label.setTextColor(getMutedTextColor())
        label.setPadding(0, dp(8), 0, dp(2))
        parent.addView(label)

        val input = EditText(activity)
        input.hint = hint
        if (defaultValue.isNotEmpty()) input.setText(defaultValue)
        input.textSize = 14f
        input.setTextColor(getPrimaryTextColor())
        input.setHintTextColor(getMutedTextColor())
        input.setPadding(dp(8), dp(8), dp(8), dp(8))
        input.setBackgroundColor(getInputBackgroundColor())
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

    private fun makeFormLabelForDialog(text: String): TextView {
        val label = TextView(activity)
        label.text = text
        label.textSize = 13f
        label.setTextColor(Color.GRAY)
        label.setPadding(0, dp(8), 0, dp(2))
        return label
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

    private fun getLinkColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(0, 114, 178)
        } else {
            Color.rgb(45, 95, 255)
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
            Color.RED
        }
    }

    // Converts dp to pixels
    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}
