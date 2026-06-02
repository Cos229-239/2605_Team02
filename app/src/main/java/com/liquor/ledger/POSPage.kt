package com.liquor.ledger

    import android.app.Activity
    import android.graphics.Color
    import android.text.Editable
    import android.text.TextWatcher
    import android.widget.Button
    import android.widget.EditText
    import android.widget.LinearLayout
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

    private var selectedProductName = ""
    private var selectedProductSku = ""
    private var selectedProductPrice = 0.0
    private var selectedProductTax = 0.0
    private var selectedProductStock = 0
    private var cartSubtotal = 0.0
    private var cartTax = 0.0
    private var cartTotal = 0.0

    fun build(): LinearLayout {

        val page = LinearLayout(activity)
        page.orientation = LinearLayout.HORIZONTAL
        page.setBackgroundColor(Color.WHITE)

        val leftSide = LinearLayout(activity)
        leftSide.orientation = LinearLayout.VERTICAL
        leftSide.setBackgroundColor(Color.rgb(245, 247, 250))

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
        headerBox.setBackgroundColor(Color.WHITE)

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
        headerTitle.setTextColor(Color.BLACK)

        val headerDate = TextView(activity)
        headerDate.textSize = 14f
        headerDate.setTextColor(Color.GRAY)

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
        cashDrawerText.setTextColor(Color.BLACK)

        val drawerStatusText = TextView(activity)
        drawerStatusText.text = "Drawer Closed"
        drawerStatusText.textSize = 13f
        drawerStatusText.setTextColor(Color.GRAY)

        rightHeader.addView(cashDrawerText)
        rightHeader.addView(drawerStatusText)

        headerBox.addView(leftHeader, leftHeaderParams)
        headerBox.addView(rightHeader)

        leftSide.addView(headerBox, headerParams)

        val entryRow = LinearLayout(activity)
        entryRow.orientation = LinearLayout.HORIZONTAL
        entryRow.setPadding(30, 20, 30, 20)
        entryRow.setBackgroundColor(Color.WHITE)

        val entryRowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        entryRowParams.setMargins(0, 10, 0, 10)

        val searchBox = EditText(activity)

        searchBox.setTextColor(Color.BLACK)
        searchBox.setHintTextColor(Color.DKGRAY)
        searchBox.setBackgroundColor(Color.rgb(245, 245, 245))

        searchBox.hint = "Scan or search product"
        searchBox.textSize = 16f
        searchBox.setSingleLine(true)

        val searchParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            3f
        )

        val qtyBox = EditText(activity)

        qtyBox.setTextColor(Color.BLACK)
        qtyBox.setHintTextColor(Color.DKGRAY)
        qtyBox.setBackgroundColor(Color.rgb(245, 245, 245))

        qtyBox.hint = "Qty"
        qtyBox.textSize = 16f
        qtyBox.setSingleLine(true)

        val smallInputParams = LinearLayout.LayoutParams(
            140,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        smallInputParams.setMargins(10, 0, 0, 0)

        val priceBox = EditText(activity)

        priceBox.setTextColor(Color.BLACK)
        priceBox.setHintTextColor(Color.DKGRAY)
        priceBox.setBackgroundColor(Color.rgb(245, 245, 245))

        priceBox.hint = "Price"
        priceBox.textSize = 16f
        priceBox.setSingleLine(true)

        val discountBox = EditText(activity)

        discountBox.setTextColor(Color.BLACK)
        discountBox.setHintTextColor(Color.DKGRAY)
        discountBox.setBackgroundColor(Color.rgb(245, 245, 245))

        discountBox.hint = "Discount %"
        discountBox.textSize = 16f
        discountBox.setSingleLine(true)

        val taxBox = EditText(activity)

        taxBox.setTextColor(Color.BLACK)
        taxBox.setHintTextColor(Color.DKGRAY)
        taxBox.setBackgroundColor(Color.rgb(245, 245, 245))

        taxBox.hint = "Tax %"
        taxBox.textSize = 16f
        taxBox.setSingleLine(true)

        val addButton = Button(activity)
        addButton.text = "Add"

        val clearButton = Button(activity)
        clearButton.text = "Clear"

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
        suggestionList.setBackgroundColor(Color.WHITE)

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
        cartBox.setBackgroundColor(Color.WHITE)

        val cartBoxParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        cartBoxParams.setMargins(0, 10, 0, 10)

        val cartHeader = LinearLayout(activity)
        cartHeader.orientation = LinearLayout.HORIZONTAL
        cartHeader.setPadding(10, 10, 10, 10)

        val productHeader = TextView(activity)
        productHeader.text = "Product"
        productHeader.textSize = 15f
        productHeader.setTextColor(Color.BLACK)

        val qtyHeader = TextView(activity)
        qtyHeader.text = "Qty"
        qtyHeader.textSize = 15f
        qtyHeader.setTextColor(Color.BLACK)

        val priceHeader = TextView(activity)
        priceHeader.text = "Price"
        priceHeader.textSize = 15f
        priceHeader.setTextColor(Color.BLACK)

        val discountHeader = TextView(activity)
        discountHeader.text = "Disc %"
        discountHeader.textSize = 15f
        discountHeader.setTextColor(Color.BLACK)

        val taxHeader = TextView(activity)
        taxHeader.text = "Tax %"
        taxHeader.textSize = 15f
        taxHeader.setTextColor(Color.BLACK)

        val totalHeader = TextView(activity)
        totalHeader.text = "Line Total"
        totalHeader.textSize = 15f
        totalHeader.setTextColor(Color.BLACK)

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

        val removeHeader = TextView(activity)
        removeHeader.text = "Remove"
        removeHeader.textSize = 15f
        removeHeader.setTextColor(Color.BLACK)

        cartHeader.addView(removeHeader, smallColumn)

        val emptyCartText = TextView(activity)
        emptyCartText.text = "No items in cart"
        emptyCartText.textSize = 16f
        emptyCartText.setTextColor(Color.GRAY)
        emptyCartText.setPadding(10, 40, 10, 10)

        cartBox.addView(cartHeader)
        cartBox.addView(emptyCartText)

        val cartItemsContainer = LinearLayout(activity)
        cartItemsContainer.orientation = LinearLayout.VERTICAL

        cartBox.addView(cartItemsContainer)

        val totalsRow = LinearLayout(activity)
        totalsRow.orientation = LinearLayout.HORIZONTAL
        totalsRow.setPadding(30, 8, 30, 8)
        totalsRow.setBackgroundColor(Color.WHITE)
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
        subtotalText.setTextColor(Color.BLACK)

        val taxText = TextView(activity)
        taxText.text = "Tax: $0.00"
        taxText.textSize = 16f
        taxText.setTextColor(Color.BLACK)

        val totalText = TextView(activity)
        totalText.text = "Total: $0.00"
        totalText.textSize = 18f
        totalText.setTextColor(Color.BLACK)

        val totalsTextParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val newTransactionButton = Button(activity)
        newTransactionButton.text = "New Transaction"

        val completeTransactionButton = Button(activity)
        completeTransactionButton.text = "Complete Transaction"

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

            cartSubtotal += lineSubtotal
            cartTax += lineTax
            cartTotal += lineTotal

            val cartRow = LinearLayout(activity)
            cartRow.orientation = LinearLayout.HORIZONTAL
            cartRow.setPadding(10, 10, 10, 10)

            val productCell = TextView(activity)
            productCell.text = selectedProductName
            productCell.textSize = 14f
            productCell.setTextColor(Color.BLACK)

            val qtyCell = TextView(activity)
            qtyCell.text = quantity.toString()
            qtyCell.textSize = 14f
            qtyCell.setTextColor(Color.BLACK)

            val priceCell = TextView(activity)
            priceCell.text = "$${"%.2f".format(selectedProductPrice)}"
            priceCell.textSize = 14f
            priceCell.setTextColor(Color.BLACK)

            val discountCell = TextView(activity)
            discountCell.text = "0%"
            discountCell.textSize = 14f
            discountCell.setTextColor(Color.BLACK)

            val taxCell = TextView(activity)
            taxCell.text = "${"%.2f".format(selectedProductTax)}%"
            taxCell.textSize = 14f
            taxCell.setTextColor(Color.BLACK)

            val totalCell = TextView(activity)
            totalCell.text = "$${"%.2f".format(lineTotal)}"
            totalCell.textSize = 14f
            totalCell.setTextColor(Color.BLACK)

            val removeButton = Button(activity)
            removeButton.text = "X"
            removeButton.textSize = 12f
            removeButton.setTextColor(Color.WHITE)
            removeButton.setBackgroundColor(Color.rgb(231, 76, 60))

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

                if (cartSubtotal < 0.0) cartSubtotal = 0.0
                if (cartTax < 0.0) cartTax = 0.0
                if (cartTotal < 0.0) cartTotal = 0.0

                subtotalText.text = "Subtotal: $${"%.2f".format(cartSubtotal)}"
                taxText.text = "Tax: $${"%.2f".format(cartTax)}"
                totalText.text = "Total: $${"%.2f".format(cartTotal)}"

                cartItemsContainer.removeView(cartRow)

                if (cartItemsContainer.childCount == 0) {
                    emptyCartText.visibility = android.view.View.VISIBLE
                }
            }

            subtotalText.text = "Subtotal: $${"%.2f".format(cartSubtotal)}"
            taxText.text = "Tax: $${"%.2f".format(cartTax)}"
            totalText.text = "Total: $${"%.2f".format(cartTotal)}"

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
                                suggestionItem.setTextColor(Color.BLACK)
                                suggestionItem.setPadding(16, 12, 16, 12)
                                suggestionItem.setBackgroundColor(Color.rgb(248, 249, 250))

                                suggestionItem.setOnClickListener {

                                    isSelectingProduct = true

                                    searchBox.setText(product["name"])
                                    searchBox.setSelection(searchBox.text.length)
                                    priceBox.setText(product["price"])
                                    taxBox.setText(product["taxPercent"])

                                    selectedProductName = product["name"] ?: ""
                                    selectedProductSku = product["sku"] ?: ""
                                    selectedProductPrice = product["price"]?.toDoubleOrNull() ?: 0.0
                                    selectedProductTax =
                                        product["taxPercent"]?.toDoubleOrNull() ?: 0.0
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
        rightPanel.setBackgroundColor(Color.rgb(235, 239, 245))

        val rightParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )

        val leftText = TextView(activity)
        leftText.text = "Left POS Area"
        leftText.textSize = 22f
        leftText.setTextColor(Color.BLACK)
        leftText.setPadding(30, 30, 30, 30)

        val rightText = TextView(activity)
        rightText.text = "Payment Panel"
        rightText.textSize = 22f
        rightText.setTextColor(Color.BLACK)
        rightText.setPadding(30, 30, 30, 30)

        rightPanel.setPadding(18, 14, 18, 14)

        val paymentTitle = TextView(activity)
        paymentTitle.text = "Payment"
        paymentTitle.textSize = 24f
        paymentTitle.setTextColor(Color.BLACK)
        paymentTitle.setPadding(0, 0, 0, 8)

        val remainingBalanceLabel = TextView(activity)
        remainingBalanceLabel.text = "Remaining Balance"
        remainingBalanceLabel.textSize = 14f
        remainingBalanceLabel.setTextColor(Color.GRAY)

        val remainingBalanceAmount = TextView(activity)
        remainingBalanceAmount.text = "$0.00"
        remainingBalanceAmount.textSize = 34f
        remainingBalanceAmount.setTextColor(Color.BLACK)
        remainingBalanceAmount.setPadding(0, 0, 0, 12)

        val paymentAmountLabel = TextView(activity)
        paymentAmountLabel.text = "Payment Amount"
        paymentAmountLabel.textSize = 14f
        paymentAmountLabel.setTextColor(Color.GRAY)

        val paymentAmountBox = EditText(activity)
        paymentAmountBox.hint = "Enter amount"
        paymentAmountBox.textSize = 18f
        paymentAmountBox.setSingleLine(true)
        paymentAmountBox.setTextColor(Color.BLACK)
        paymentAmountBox.setHintTextColor(Color.DKGRAY)
        paymentAmountBox.setBackgroundColor(Color.WHITE)

        rightPanel.addView(paymentTitle)
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

        fiveButton.setBackgroundColor(Color.rgb(120, 160, 120))
        tenButton.setBackgroundColor(Color.rgb(120, 160, 120))
        twentyButton.setBackgroundColor(Color.rgb(120, 160, 120))
        fiftyButton.setBackgroundColor(Color.rgb(120, 160, 120))
        hundredButton.setBackgroundColor(Color.rgb(120, 160, 120))
        exactButton.setBackgroundColor(Color.rgb(120, 160, 120))

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

        keypadGrid.addView(keypadRow1)
        keypadGrid.addView(keypadRow2)
        keypadGrid.addView(keypadRow3)
        keypadGrid.addView(keypadRow4)

        rightPanel.addView(keypadGrid, keypadParams)

        val clearAmountButton = Button(activity)
        clearAmountButton.text = "Clear Amount"
        clearAmountButton.setBackgroundColor(Color.rgb(230, 149, 62))

        val applyPaymentButton = Button(activity)
        applyPaymentButton.text = "Apply Payment"
        applyPaymentButton.setBackgroundColor(Color.rgb(76, 175, 80))

        val completeSaleButton = Button(activity)
        completeSaleButton.text = "Complete Sale"
        completeSaleButton.setBackgroundColor(Color.rgb(211, 47, 47))

        val cancelTransactionButton = Button(activity)
        cancelTransactionButton.text = "Cancel Transaction"

        val actionButtonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        actionButtonParams.setMargins(0, 3, 0, 3)

        rightPanel.addView(clearAmountButton, actionButtonParams)
        rightPanel.addView(applyPaymentButton, actionButtonParams)
        rightPanel.addView(completeSaleButton, actionButtonParams)
        rightPanel.addView(cancelTransactionButton, actionButtonParams)

        addButton.setOnClickListener {
            addSelectedProductToCart()
        }
        
        page.addView(leftSide, leftParams)
        page.addView(rightPanel, rightParams)

        return page
    }

}