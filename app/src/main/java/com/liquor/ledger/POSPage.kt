package com.liquor.ledger

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.liquor.ledger.firebase.FirebaseManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class POSPage(private val activity: Activity) {

    private val db = FirebaseManager.db

    // Reads saved settings from SettingsPage
    private val prefs = activity.getSharedPreferences("settings_prefs", Activity.MODE_PRIVATE)

    private val KEY_COLORBLIND_MODE = "colorblind_mode"
    private val KEY_DARK_MODE = "dark_mode"

    private var selectedProductName = ""
    private var selectedProductSku = ""
    private var selectedProductPrice = 0.0
    private var selectedProductTax = 0.0
    private var selectedProductStock = 0
    private var cartSubtotal = 0.0
    private var cartTax = 0.0
    private var cartTotal = 0.0
    private val cartItems = mutableListOf<MutableMap<String, Any>>()
    private var amountPaid = 0.0
    private var remainingBalance = 0.0
    private var changeDue = 0.0

    private lateinit var remainingBalanceAmount: TextView
    private lateinit var paymentAmountBox: EditText

    fun build(): LinearLayout {

        val page = LinearLayout(activity)
        page.orientation = LinearLayout.HORIZONTAL
        page.setBackgroundColor(getPageBackgroundColor())

        val leftSide = LinearLayout(activity)
        leftSide.orientation = LinearLayout.VERTICAL
        leftSide.setBackgroundColor(getSectionBackgroundColor())

        leftSide.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        val leftParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            3f
        )

        val headerBox = LinearLayout(activity)
        headerBox.orientation = LinearLayout.HORIZONTAL
        headerBox.setPadding(30, 25, 30, 20)
        headerBox.setBackgroundColor(getCardBackgroundColor())

        val headerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val leftHeader = LinearLayout(activity)
        leftHeader.orientation = LinearLayout.VERTICAL

        val leftHeaderParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val headerTitle = TextView(activity)
        headerTitle.text = "Point of Sale / Register"
        headerTitle.textSize = 24f
        headerTitle.setTextColor(getPrimaryTextColor())

        val headerDate = TextView(activity)
        headerDate.textSize = 14f
        headerDate.setTextColor(getMutedTextColor())

        val dateFormat = SimpleDateFormat(
            "EEEE, MMMM d, yyyy • h:mm a",
            Locale.getDefault()
        )

        headerDate.text = dateFormat.format(Date())

        leftHeader.addView(headerTitle)
        leftHeader.addView(headerDate)

        val rightHeader = LinearLayout(activity)
        rightHeader.orientation = LinearLayout.VERTICAL

        val cashDrawerText = TextView(activity)
        cashDrawerText.text = "Cash Drawer: \$0.00"
        cashDrawerText.textSize = 16f
        cashDrawerText.setTextColor(getPrimaryTextColor())

        val drawerStatusText = TextView(activity)
        drawerStatusText.text = "Drawer Closed"
        drawerStatusText.textSize = 13f
        drawerStatusText.setTextColor(getMutedTextColor())

        rightHeader.addView(cashDrawerText)
        rightHeader.addView(drawerStatusText)

        headerBox.addView(leftHeader, leftHeaderParams)
        headerBox.addView(rightHeader)

        leftSide.addView(headerBox, headerParams)

        val entryRow = LinearLayout(activity)
        entryRow.orientation = LinearLayout.HORIZONTAL
        entryRow.setPadding(30, 20, 30, 20)
        entryRow.setBackgroundColor(getCardBackgroundColor())

        val entryRowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        entryRowParams.setMargins(0, 10, 0, 10)

        val searchBox = EditText(activity)
        searchBox.hint = "Scan or search product"
        searchBox.textSize = 16f
        searchBox.setSingleLine(true)
        styleInput(searchBox)

        val searchParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            3f
        )

        val qtyBox = EditText(activity)
        qtyBox.hint = "Qty"
        qtyBox.textSize = 16f
        qtyBox.setSingleLine(true)
        styleInput(qtyBox)

        val smallInputParams = LinearLayout.LayoutParams(
            140,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        smallInputParams.setMargins(10, 0, 0, 0)

        val priceBox = EditText(activity)
        priceBox.hint = "Price"
        priceBox.textSize = 16f
        priceBox.setSingleLine(true)
        styleInput(priceBox)

        val discountBox = EditText(activity)
        discountBox.hint = "Discount %"
        discountBox.textSize = 16f
        discountBox.setSingleLine(true)
        styleInput(discountBox)

        val taxBox = EditText(activity)
        taxBox.hint = "Tax %"
        taxBox.textSize = 16f
        taxBox.setSingleLine(true)
        styleInput(taxBox)

        val addButton = Button(activity)
        addButton.text = "Add"
        styleButton(addButton, getPrimaryActionColor())

        val clearButton = Button(activity)
        clearButton.text = "Clear"
        styleButton(clearButton, getMutedButtonColor())

        entryRow.addView(searchBox, searchParams)
        entryRow.addView(qtyBox, smallInputParams)
        entryRow.addView(priceBox, smallInputParams)
        entryRow.addView(discountBox, smallInputParams)
        entryRow.addView(taxBox, smallInputParams)
        entryRow.addView(addButton)
        entryRow.addView(clearButton)

        leftSide.addView(entryRow, entryRowParams)

        val suggestionList = LinearLayout(activity)
        suggestionList.orientation = LinearLayout.VERTICAL
        suggestionList.setBackgroundColor(getCardBackgroundColor())

        val suggestionListParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        suggestionListParams.setMargins(30, 0, 30, 10)

        leftSide.addView(suggestionList, suggestionListParams)

        var isSelectingProduct = false

        val cartBox = LinearLayout(activity)
        cartBox.orientation = LinearLayout.VERTICAL
        cartBox.setPadding(30, 20, 30, 20)
        cartBox.setBackgroundColor(getCardBackgroundColor())

        val cartBoxParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        cartBoxParams.setMargins(0, 10, 0, 10)

        val cartHeader = LinearLayout(activity)
        cartHeader.orientation = LinearLayout.HORIZONTAL
        cartHeader.setPadding(10, 10, 10, 10)

        val productHeader = makeHeaderCell("Product")
        val qtyHeader = makeHeaderCell("Qty")
        val priceHeader = makeHeaderCell("Price")
        val discountHeader = makeHeaderCell("Disc %")
        val taxHeader = makeHeaderCell("Tax %")
        val totalHeader = makeHeaderCell("Line Total")
        val removeHeader = makeHeaderCell("Remove")

        val wideColumn = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            2f
        )

        val smallColumn = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        cartHeader.addView(productHeader, wideColumn)
        cartHeader.addView(qtyHeader, smallColumn)
        cartHeader.addView(priceHeader, smallColumn)
        cartHeader.addView(discountHeader, smallColumn)
        cartHeader.addView(taxHeader, smallColumn)
        cartHeader.addView(totalHeader, smallColumn)
        cartHeader.addView(removeHeader, smallColumn)

        val emptyCartText = TextView(activity)
        emptyCartText.text = "No items in cart"
        emptyCartText.textSize = 16f
        emptyCartText.setTextColor(getMutedTextColor())
        emptyCartText.setPadding(10, 40, 10, 10)

        cartBox.addView(cartHeader)
        cartBox.addView(emptyCartText)

        val cartScrollView = ScrollView(activity)
        cartScrollView.isVerticalScrollBarEnabled = true
        cartScrollView.isScrollbarFadingEnabled = false
        cartScrollView.setBackgroundColor(getCardBackgroundColor())

        val cartItemsContainer = LinearLayout(activity)
        cartItemsContainer.orientation = LinearLayout.VERTICAL
        cartItemsContainer.setBackgroundColor(getCardBackgroundColor())

        cartScrollView.addView(cartItemsContainer)

        val cartScrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        cartBox.addView(cartScrollView, cartScrollParams)

        val totalsRow = LinearLayout(activity)
        totalsRow.orientation = LinearLayout.HORIZONTAL
        totalsRow.setPadding(30, 8, 30, 8)
        totalsRow.setBackgroundColor(getCardBackgroundColor())
        totalsRow.elevation = 8f

        leftSide.addView(cartBox, cartBoxParams)

        val totalsRowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        totalsRowParams.setMargins(0, 10, 0, 0)

        val subtotalText = TextView(activity)
        subtotalText.text = "Subtotal: $0.00"
        subtotalText.textSize = 16f
        subtotalText.setTextColor(getPrimaryTextColor())

        val taxText = TextView(activity)
        taxText.text = "Tax: $0.00"
        taxText.textSize = 16f
        taxText.setTextColor(getPrimaryTextColor())

        val totalText = TextView(activity)
        totalText.text = "Total: $0.00"
        totalText.textSize = 18f
        totalText.setTextColor(getPrimaryTextColor())

        val totalsTextParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val newTransactionButton = Button(activity)
        newTransactionButton.text = "New Transaction"
        styleButton(newTransactionButton, getMutedButtonColor())

        val completeTransactionButton = Button(activity)
        completeTransactionButton.text = "Complete Transaction"
        styleButton(completeTransactionButton, getPositiveColor())

        totalsRow.addView(subtotalText, totalsTextParams)
        totalsRow.addView(taxText, totalsTextParams)
        totalsRow.addView(totalText, totalsTextParams)
        totalsRow.addView(newTransactionButton)
        totalsRow.addView(completeTransactionButton)

        fun addSelectedProductToCart() {

            if (selectedProductName.isEmpty()) {
                Toast.makeText(activity, "Select a product first", Toast.LENGTH_SHORT).show()
                return
            }

            val quantity = qtyBox.text.toString().toIntOrNull() ?: 1

            if (quantity <= 0) {
                Toast.makeText(activity, "Enter a valid quantity", Toast.LENGTH_SHORT).show()
                return
            }

            if (selectedProductStock > 0 && quantity > selectedProductStock) {
                Toast.makeText(activity, "Not enough stock available", Toast.LENGTH_SHORT).show()
                return
            }

            emptyCartText.visibility = android.view.View.GONE

            val lineSubtotal = selectedProductPrice * quantity
            val lineTax = lineSubtotal * (selectedProductTax / 100)
            val lineTotal = lineSubtotal + lineTax

            val cartItem = mutableMapOf<String, Any>(
                "name" to selectedProductName,
                "sku" to selectedProductSku,
                "quantity" to quantity,
                "price" to selectedProductPrice,
                "taxPercent" to selectedProductTax,
                "lineSubtotal" to lineSubtotal,
                "lineTax" to lineTax,
                "lineTotal" to lineTotal
            )

            cartItems.add(cartItem)

            cartSubtotal += lineSubtotal
            cartTax += lineTax
            cartTotal += lineTotal
            remainingBalance = cartTotal - amountPaid

            val cartRow = LinearLayout(activity)
            cartRow.orientation = LinearLayout.HORIZONTAL
            cartRow.setPadding(10, 10, 10, 10)
            cartRow.setBackgroundColor(getCardBackgroundColor())

            val productCell = makeCartCell(selectedProductName)
            val qtyCell = makeCartCell(quantity.toString())
            val priceCell = makeCartCell("$${"%.2f".format(selectedProductPrice)}")
            val discountCell = makeCartCell("0%")
            val taxCell = makeCartCell("${"%.2f".format(selectedProductTax)}%")
            val totalCell = makeCartCell("$${"%.2f".format(lineTotal)}")

            val removeButton = Button(activity)
            removeButton.text = "X"
            removeButton.textSize = 12f
            styleButton(removeButton, getNegativeColor())

            cartRow.addView(productCell, wideColumn)
            cartRow.addView(qtyCell, smallColumn)
            cartRow.addView(priceCell, smallColumn)
            cartRow.addView(discountCell, smallColumn)
            cartRow.addView(taxCell, smallColumn)
            cartRow.addView(totalCell, smallColumn)
            cartRow.addView(removeButton, smallColumn)

            cartItemsContainer.addView(cartRow)

            removeButton.setOnClickListener {
                cartSubtotal -= lineSubtotal
                cartTax -= lineTax
                cartTotal -= lineTotal
                remainingBalance = cartTotal - amountPaid

                if (remainingBalance < 0.0) {
                    remainingBalance = 0.0
                }

                if (cartSubtotal < 0.0) cartSubtotal = 0.0
                if (cartTax < 0.0) cartTax = 0.0
                if (cartTotal < 0.0) cartTotal = 0.0

                subtotalText.text = "Subtotal: $${"%.2f".format(cartSubtotal)}"
                taxText.text = "Tax: $${"%.2f".format(cartTax)}"
                totalText.text = "Total: $${"%.2f".format(cartTotal)}"
                remainingBalanceAmount.text = "$${"%.2f".format(remainingBalance)}"

                cartItemsContainer.removeView(cartRow)
                cartItems.remove(cartItem)

                if (cartItemsContainer.childCount == 0) {
                    emptyCartText.visibility = android.view.View.VISIBLE
                }

                if (cartItems.isEmpty()) {
                    amountPaid = 0.0
                    remainingBalance = 0.0
                    changeDue = 0.0
                    paymentAmountBox.setText("")
                }
            }

            subtotalText.text = "Subtotal: $${"%.2f".format(cartSubtotal)}"
            taxText.text = "Tax: $${"%.2f".format(cartTax)}"
            totalText.text = "Total: $${"%.2f".format(cartTotal)}"
            remainingBalanceAmount.text = "$${"%.2f".format(remainingBalance)}"

            searchBox.setText("")
            qtyBox.setText("")
            priceBox.setText("")
            discountBox.setText("")
            taxBox.setText("")

            selectedProductName = ""
            selectedProductSku = ""
            selectedProductPrice = 0.0
            selectedProductTax = 0.0
            selectedProductStock = 0
        }

        searchBox.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {

                if (isSelectingProduct) {
                    return
                }

                val query = s.toString().trim()

                suggestionList.removeAllViews()

                if (query.isEmpty()) {
                    return
                }

                CoroutineScope(Dispatchers.IO).launch {

                    try {
                        val exactSkuSnapshot = db.collection("products")
                            .whereEqualTo("sku", query)
                            .limit(1)
                            .get()
                            .await()

                        if (!exactSkuSnapshot.isEmpty) {

                            val product = exactSkuSnapshot.documents[0]

                            val name = product.getString("name") ?: query
                            val sku = product.getString("sku") ?: ""
                            val price = product.getDouble("price") ?: 0.0
                            val taxPercent = product.getDouble("taxPercent") ?: 0.0
                            val stock = product.getLong("stock")?.toInt() ?: 0

                            withContext(Dispatchers.Main) {
                                isSelectingProduct = true

                                searchBox.setText(name)
                                searchBox.setSelection(searchBox.text.length)
                                priceBox.setText(price.toString())
                                taxBox.setText(taxPercent.toString())

                                selectedProductName = name
                                selectedProductSku = sku
                                selectedProductPrice = price
                                selectedProductTax = taxPercent
                                selectedProductStock = stock

                                suggestionList.removeAllViews()

                                addSelectedProductToCart()

                                isSelectingProduct = false
                            }

                            return@launch
                        }

                        if (query.length < 2) {
                            return@launch
                        }

                        val snapshot = db.collection("products")
                            .get()
                            .await()

                        val matches = snapshot.documents.mapNotNull { doc ->

                            val name = doc.getString("name") ?: return@mapNotNull null
                            val sku = doc.getString("sku") ?: ""
                            val price = doc.getDouble("price") ?: 0.0
                            val taxPercent = doc.getDouble("taxPercent") ?: 0.0
                            val stock = doc.getLong("stock")?.toInt() ?: 0

                            mapOf(
                                "name" to name,
                                "sku" to sku,
                                "price" to price.toString(),
                                "taxPercent" to taxPercent.toString(),
                                "stock" to stock.toString()
                            )

                        }.filter { product ->
                            product["name"]?.contains(query, ignoreCase = true) == true ||
                                    product["sku"]?.contains(query, ignoreCase = true) == true
                        }.take(5)

                        withContext(Dispatchers.Main) {
                            suggestionList.removeAllViews()

                            matches.forEach { product ->
                                val suggestionItem = TextView(activity)
                                suggestionItem.text = "${product["name"]}    $${product["price"]}"
                                suggestionItem.textSize = 15f
                                suggestionItem.setTextColor(getPrimaryTextColor())
                                suggestionItem.setPadding(16, 12, 16, 12)
                                suggestionItem.setBackgroundColor(getInputBackgroundColor())

                                suggestionItem.setOnClickListener {
                                    isSelectingProduct = true

                                    searchBox.setText(product["name"])
                                    searchBox.setSelection(searchBox.text.length)
                                    priceBox.setText(product["price"])
                                    taxBox.setText(product["taxPercent"])

                                    selectedProductName = product["name"] ?: ""
                                    selectedProductSku = product["sku"] ?: ""
                                    selectedProductPrice = product["price"]?.toDoubleOrNull() ?: 0.0
                                    selectedProductTax = product["taxPercent"]?.toDoubleOrNull() ?: 0.0
                                    selectedProductStock = product["stock"]?.toIntOrNull() ?: 0

                                    suggestionList.removeAllViews()

                                    addSelectedProductToCart()

                                    isSelectingProduct = false
                                }

                                suggestionList.addView(suggestionItem)
                            }
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                activity,
                                "Product search error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
        })

        leftSide.addView(totalsRow, totalsRowParams)

        val rightPanel = LinearLayout(activity)
        rightPanel.orientation = LinearLayout.VERTICAL
        rightPanel.setBackgroundColor(getPanelBackgroundColor())
        rightPanel.setPadding(18, 8, 18, 8)

        val rightParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )

        val remainingBalanceLabel = TextView(activity)
        remainingBalanceLabel.text = "Remaining Balance"
        remainingBalanceLabel.textSize = 14f
        remainingBalanceLabel.setTextColor(getMutedTextColor())

        remainingBalanceAmount = TextView(activity)
        remainingBalanceAmount.text = "$${"%.2f".format(remainingBalance)}"
        remainingBalanceAmount.textSize = 34f
        remainingBalanceAmount.setTextColor(getPrimaryTextColor())
        remainingBalanceAmount.setPadding(0, 0, 0, 12)

        val paymentAmountLabel = TextView(activity)
        paymentAmountLabel.text = "Payment Amount"
        paymentAmountLabel.textSize = 14f
        paymentAmountLabel.setTextColor(getMutedTextColor())

        paymentAmountBox = EditText(activity)
        paymentAmountBox.hint = "Enter amount"
        paymentAmountBox.textSize = 18f
        paymentAmountBox.setSingleLine(true)
        styleInput(paymentAmountBox)

        rightPanel.addView(remainingBalanceLabel)
        rightPanel.addView(remainingBalanceAmount)
        rightPanel.addView(paymentAmountLabel)
        rightPanel.addView(paymentAmountBox)

        val cashButtonsGrid = LinearLayout(activity)
        cashButtonsGrid.orientation = LinearLayout.VERTICAL

        val cashGridParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        cashGridParams.setMargins(0, 8, 0, 7)

        val cashRow1 = LinearLayout(activity)
        cashRow1.orientation = LinearLayout.HORIZONTAL

        val fiveButton = Button(activity)
        fiveButton.text = "$5"

        val tenButton = Button(activity)
        tenButton.text = "$10"

        val twentyButton = Button(activity)
        twentyButton.text = "$20"

        val cashRow2 = LinearLayout(activity)
        cashRow2.orientation = LinearLayout.HORIZONTAL

        val fiftyButton = Button(activity)
        fiftyButton.text = "$50"

        val hundredButton = Button(activity)
        hundredButton.text = "$100"

        val exactButton = Button(activity)
        exactButton.text = "FULL"

        val cashButtonParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        cashButtonParams.setMargins(6, 1, 6, 1)

        cashRow1.addView(fiveButton, cashButtonParams)
        cashRow1.addView(tenButton, cashButtonParams)
        cashRow1.addView(twentyButton, cashButtonParams)

        cashRow2.addView(fiftyButton, cashButtonParams)
        cashRow2.addView(hundredButton, cashButtonParams)
        cashRow2.addView(exactButton, cashButtonParams)

        styleButton(fiveButton, getCashButtonColor())
        styleButton(tenButton, getCashButtonColor())
        styleButton(twentyButton, getCashButtonColor())
        styleButton(fiftyButton, getCashButtonColor())
        styleButton(hundredButton, getCashButtonColor())
        styleButton(exactButton, getCashButtonColor())

        cashButtonsGrid.addView(cashRow1)
        cashButtonsGrid.addView(cashRow2)

        rightPanel.addView(cashButtonsGrid, cashGridParams)

        val keypadGrid = LinearLayout(activity)
        keypadGrid.orientation = LinearLayout.VERTICAL

        val keypadParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        keypadParams.setMargins(0, 0, 0, 5)

        val keypadButtonParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        keypadButtonParams.setMargins(4, 1, 4, 1)

        val keypadRow1 = LinearLayout(activity)
        keypadRow1.orientation = LinearLayout.HORIZONTAL

        val button1 = Button(activity)
        button1.text = "1"

        val button2 = Button(activity)
        button2.text = "2"

        val button3 = Button(activity)
        button3.text = "3"

        keypadRow1.addView(button1, keypadButtonParams)
        keypadRow1.addView(button2, keypadButtonParams)
        keypadRow1.addView(button3, keypadButtonParams)

        val keypadRow2 = LinearLayout(activity)
        keypadRow2.orientation = LinearLayout.HORIZONTAL

        val button4 = Button(activity)
        button4.text = "4"

        val button5 = Button(activity)
        button5.text = "5"

        val button6 = Button(activity)
        button6.text = "6"

        keypadRow2.addView(button4, keypadButtonParams)
        keypadRow2.addView(button5, keypadButtonParams)
        keypadRow2.addView(button6, keypadButtonParams)

        val keypadRow3 = LinearLayout(activity)
        keypadRow3.orientation = LinearLayout.HORIZONTAL

        val button7 = Button(activity)
        button7.text = "7"

        val button8 = Button(activity)
        button8.text = "8"

        val button9 = Button(activity)
        button9.text = "9"

        keypadRow3.addView(button7, keypadButtonParams)
        keypadRow3.addView(button8, keypadButtonParams)
        keypadRow3.addView(button9, keypadButtonParams)

        val keypadRow4 = LinearLayout(activity)
        keypadRow4.orientation = LinearLayout.HORIZONTAL

        val decimalButton = Button(activity)
        decimalButton.text = "."

        val button0 = Button(activity)
        button0.text = "0"

        val backspaceButton = Button(activity)
        backspaceButton.text = "⌫"

        keypadRow4.addView(decimalButton, keypadButtonParams)
        keypadRow4.addView(button0, keypadButtonParams)
        keypadRow4.addView(backspaceButton, keypadButtonParams)

        styleButton(button1, getKeypadButtonColor())
        styleButton(button2, getKeypadButtonColor())
        styleButton(button3, getKeypadButtonColor())
        styleButton(button4, getKeypadButtonColor())
        styleButton(button5, getKeypadButtonColor())
        styleButton(button6, getKeypadButtonColor())
        styleButton(button7, getKeypadButtonColor())
        styleButton(button8, getKeypadButtonColor())
        styleButton(button9, getKeypadButtonColor())
        styleButton(decimalButton, getKeypadButtonColor())
        styleButton(button0, getKeypadButtonColor())
        styleButton(backspaceButton, getKeypadButtonColor())

        keypadGrid.addView(keypadRow1)
        keypadGrid.addView(keypadRow2)
        keypadGrid.addView(keypadRow3)
        keypadGrid.addView(keypadRow4)

        rightPanel.addView(keypadGrid, keypadParams)

        val clearAmountButton = Button(activity)
        clearAmountButton.text = "Clear Amount"
        styleButton(clearAmountButton, getWarningColor())

        val applyPaymentButton = Button(activity)
        applyPaymentButton.text = "Apply Payment"
        styleButton(applyPaymentButton, getPositiveColor())

        val completeSaleButton = Button(activity)
        completeSaleButton.text = "Complete Sale"
        styleButton(completeSaleButton, getNegativeColor())

        val cancelTransactionButton = Button(activity)
        cancelTransactionButton.text = "Cancel Transaction"
        styleButton(cancelTransactionButton, getMutedButtonColor())

        val actionButtonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        actionButtonParams.setMargins(0, 3, 0, 3)

        rightPanel.addView(clearAmountButton, actionButtonParams)
        rightPanel.addView(applyPaymentButton, actionButtonParams)
        rightPanel.addView(completeSaleButton, actionButtonParams)
        rightPanel.addView(cancelTransactionButton, actionButtonParams)

        clearAmountButton.setOnClickListener {
            paymentAmountBox.setText("")
        }

        fiveButton.setOnClickListener {
            paymentAmountBox.setText("5.00")
        }

        tenButton.setOnClickListener {
            paymentAmountBox.setText("10.00")
        }

        twentyButton.setOnClickListener {
            paymentAmountBox.setText("20.00")
        }

        fiftyButton.setOnClickListener {
            paymentAmountBox.setText("50.00")
        }

        hundredButton.setOnClickListener {
            paymentAmountBox.setText("100.00")
        }

        exactButton.setOnClickListener {
            paymentAmountBox.setText("%.2f".format(remainingBalance))
        }

        fun appendPaymentText(value: String) {
            paymentAmountBox.append(value)
        }

        button1.setOnClickListener { appendPaymentText("1") }
        button2.setOnClickListener { appendPaymentText("2") }
        button3.setOnClickListener { appendPaymentText("3") }

        button4.setOnClickListener { appendPaymentText("4") }
        button5.setOnClickListener { appendPaymentText("5") }
        button6.setOnClickListener { appendPaymentText("6") }

        button7.setOnClickListener { appendPaymentText("7") }
        button8.setOnClickListener { appendPaymentText("8") }
        button9.setOnClickListener { appendPaymentText("9") }

        button0.setOnClickListener { appendPaymentText("0") }

        decimalButton.setOnClickListener {
            val currentText = paymentAmountBox.text.toString()

            if (!currentText.contains(".")) {
                appendPaymentText(".")
            }
        }

        backspaceButton.setOnClickListener {
            val currentText = paymentAmountBox.text.toString()

            if (currentText.isNotEmpty()) {
                paymentAmountBox.setText(
                    currentText.substring(0, currentText.length - 1)
                )

                paymentAmountBox.setSelection(
                    paymentAmountBox.text.length
                )
            }
        }

        fun completePaidTransaction() {

            Toast.makeText(activity, "Completing sale...", Toast.LENGTH_SHORT).show()

            if (cartItems.isEmpty()) {
                Toast.makeText(activity, "Cart is empty", Toast.LENGTH_SHORT).show()
                return
            }

            if (amountPaid < cartTotal) {
                Toast.makeText(activity, "Payment is not complete", Toast.LENGTH_SHORT).show()
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val saleData = hashMapOf(
                        "subtotal" to cartSubtotal,
                        "tax" to cartTax,
                        "total" to cartTotal,
                        "paidAmount" to amountPaid,
                        "changeDue" to maxOf(0.0, amountPaid - cartTotal),
                        "timestamp" to Date(),
                        "items" to cartItems.map { HashMap(it) }
                    )

                    db.collection("sales").add(saleData).await()

                    cartItems.forEach { item ->
                        try {
                            val sku = item["sku"].toString()
                            val quantitySold = item["quantity"].toString().toIntOrNull() ?: 0

                            if (sku.isNotEmpty() && quantitySold > 0) {
                                val productSnapshot = db.collection("products")
                                    .whereEqualTo("sku", sku)
                                    .limit(1)
                                    .get()
                                    .await()

                                if (!productSnapshot.isEmpty) {
                                    val productDoc = productSnapshot.documents[0]
                                    val currentStock = productDoc.getLong("stock") ?: 0L
                                    val newStock = maxOf(0L, currentStock - quantitySold)

                                    db.collection("products")
                                        .document(productDoc.id)
                                        .update("stock", newStock)
                                        .await()
                                }
                            }

                        } catch (stockError: Exception) {
                        }
                    }

                    withContext(Dispatchers.Main) {
                        cartItems.clear()
                        cartItemsContainer.removeAllViews()
                        emptyCartText.visibility = android.view.View.VISIBLE

                        cartSubtotal = 0.0
                        cartTax = 0.0
                        cartTotal = 0.0
                        amountPaid = 0.0
                        remainingBalance = 0.0
                        changeDue = 0.0

                        subtotalText.text = "Subtotal: $0.00"
                        taxText.text = "Tax: $0.00"
                        totalText.text = "Total: $0.00"
                        remainingBalanceAmount.text = "$0.00"

                        searchBox.setText("")
                        qtyBox.setText("")
                        priceBox.setText("")
                        discountBox.setText("")
                        taxBox.setText("")
                        paymentAmountBox.setText("")

                        Toast.makeText(activity, "Sale completed", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            activity,
                            "Sale error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        applyPaymentButton.setOnClickListener {
            val paymentAmount = paymentAmountBox.text.toString().toDoubleOrNull()

            if (paymentAmount == null || paymentAmount <= 0.0) {
                Toast.makeText(activity, "Enter a valid payment amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cartTotal <= 0.0) {
                Toast.makeText(activity, "Cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            amountPaid += paymentAmount
            remainingBalance = cartTotal - amountPaid

            if (remainingBalance < 0.0) {
                changeDue = amountPaid - cartTotal
                remainingBalance = 0.0

                AlertDialog.Builder(activity)
                    .setTitle("Change Due")
                    .setMessage("$${"%.2f".format(changeDue)}")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            } else {
                changeDue = 0.0
            }

            remainingBalanceAmount.text = "$${"%.2f".format(remainingBalance)}"
            paymentAmountBox.setText("")

            if (remainingBalance <= 0.1) {
                completePaidTransaction()
            }
        }

        completeSaleButton.setOnClickListener {
            completePaidTransaction()
        }

        addButton.setOnClickListener {
            addSelectedProductToCart()
        }

        newTransactionButton.setOnClickListener {
            AlertDialog.Builder(activity)
                .setTitle("Cancel Current Transaction?")
                .setMessage("This will clear the current cart and payment information.")
                .setPositiveButton("Yes") { dialog, _ ->
                    cartItems.clear()
                    cartItemsContainer.removeAllViews()
                    emptyCartText.visibility = android.view.View.VISIBLE

                    cartSubtotal = 0.0
                    cartTax = 0.0
                    cartTotal = 0.0
                    amountPaid = 0.0
                    remainingBalance = 0.0
                    changeDue = 0.0

                    subtotalText.text = "Subtotal: $0.00"
                    taxText.text = "Tax: $0.00"
                    totalText.text = "Total: $0.00"
                    remainingBalanceAmount.text = "$0.00"

                    searchBox.setText("")
                    qtyBox.setText("")
                    priceBox.setText("")
                    discountBox.setText("")
                    taxBox.setText("")
                    paymentAmountBox.setText("")

                    selectedProductName = ""
                    selectedProductSku = ""
                    selectedProductPrice = 0.0
                    selectedProductTax = 0.0
                    selectedProductStock = 0

                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        val rightScrollView = ScrollView(activity)
        rightScrollView.isVerticalScrollBarEnabled = true
        rightScrollView.isScrollbarFadingEnabled = false
        rightScrollView.setBackgroundColor(getPanelBackgroundColor())

        rightScrollView.addView(rightPanel)

        page.addView(leftSide, leftParams)
        page.addView(rightScrollView, rightParams)

        return page
    }

    private fun makeHeaderCell(text: String): TextView {
        val cell = TextView(activity)
        cell.text = text
        cell.textSize = 15f
        cell.setTextColor(getPrimaryTextColor())
        return cell
    }

    private fun makeCartCell(text: String): TextView {
        val cell = TextView(activity)
        cell.text = text
        cell.textSize = 14f
        cell.setTextColor(getPrimaryTextColor())
        return cell
    }

    private fun styleInput(input: EditText) {
        input.setTextColor(getPrimaryTextColor())
        input.setHintTextColor(getMutedTextColor())
        input.setBackgroundColor(getInputBackgroundColor())
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

    private fun getSectionBackgroundColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(34, 34, 34)
        } else {
            Color.rgb(245, 247, 250)
        }
    }

    private fun getCardBackgroundColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(48, 48, 48)
        } else {
            Color.WHITE
        }
    }

    private fun getPanelBackgroundColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(42, 42, 42)
        } else {
            Color.rgb(235, 239, 245)
        }
    }

    private fun getInputBackgroundColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(60, 60, 60)
        } else {
            Color.rgb(245, 245, 245)
        }
    }

    private fun getPrimaryTextColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    private fun getMutedTextColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(180, 180, 180)
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
            Color.rgb(76, 175, 80)
        }
    }

    private fun getWarningColor(): Int {
        return Color.rgb(230, 159, 0)
    }

    private fun getNegativeColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(213, 94, 0)
        } else {
            Color.rgb(211, 47, 47)
        }
    }

    private fun getCashButtonColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(0, 114, 178)
        } else {
            Color.rgb(120, 160, 120)
        }
    }

    private fun getKeypadButtonColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(70, 70, 70)
        } else {
            Color.LTGRAY
        }
    }

    private fun getMutedButtonColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(90, 90, 90)
        } else {
            Color.rgb(107, 114, 128)
        }
    }
}