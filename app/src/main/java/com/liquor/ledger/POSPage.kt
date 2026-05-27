package com.liquor.ledger

    import android.app.Activity
    import android.graphics.Color
    import android.widget.LinearLayout
    import android.widget.TextView
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale
    import android.widget.Button
    import android.widget.EditText


    class POSPage(private val activity: Activity) {
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
            searchBox.hint = "Scan or search product"
            searchBox.textSize = 16f
            searchBox.setSingleLine(true)

            val searchParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                3f
            )

            val qtyBox = EditText(activity)
            qtyBox.hint = "Qty"
            qtyBox.textSize = 16f
            qtyBox.setSingleLine(true)

            val smallInputParams = LinearLayout.LayoutParams(
                140,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            smallInputParams.setMargins(10, 0, 0, 0)

            val priceBox = EditText(activity)
            priceBox.hint = "Price"
            priceBox.textSize = 16f
            priceBox.setSingleLine(true)

            val discountBox = EditText(activity)
            discountBox.hint = "Discount %"
            discountBox.textSize = 16f
            discountBox.setSingleLine(true)

            val taxBox = EditText(activity)
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

            val emptyCartText = TextView(activity)
            emptyCartText.text = "No items in cart"
            emptyCartText.textSize = 16f
            emptyCartText.setTextColor(Color.GRAY)
            emptyCartText.setPadding(10, 40, 10, 10)

            cartBox.addView(cartHeader)
            cartBox.addView(emptyCartText)

            val totalsRow = LinearLayout(activity)
            totalsRow.orientation = LinearLayout.HORIZONTAL
            totalsRow.setPadding(30, 20, 30, 20)
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
            totalText.textSize = 20f
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

            rightPanel.addView(rightText)
            page.addView(leftSide, leftParams)
            page.addView(rightPanel, rightParams)

            return page
        }
    }
