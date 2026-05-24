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
                2f
            )

            val qtyBox = EditText(activity)
            qtyBox.hint = "Qty"
            qtyBox.textSize = 16f
            qtyBox.setSingleLine(true)

            val smallInputParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
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

            leftSide.addView(leftText)
            rightPanel.addView(rightText)
            page.addView(leftSide, leftParams)
            page.addView(rightPanel, rightParams)

            return page
        }
    }
