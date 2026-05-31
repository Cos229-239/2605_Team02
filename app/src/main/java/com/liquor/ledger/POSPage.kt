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

    // Reads saved settings from SettingsPage
    private val prefs = activity.getSharedPreferences("settings_prefs", Activity.MODE_PRIVATE)

    private val KEY_COLORBLIND_MODE = "colorblind_mode"
    private val KEY_DARK_MODE = "dark_mode"

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
        emptyCartText.setTextColor(getMutedTextColor())
        emptyCartText.setPadding(10, 40, 10, 10)

        cartBox.addView(cartHeader)
        cartBox.addView(emptyCartText)

        val totalsRow = LinearLayout(activity)
        totalsRow.orientation = LinearLayout.HORIZONTAL
        totalsRow.setPadding(30, 20, 30, 20)
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
        totalText.textSize = 20f
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

        leftSide.addView(totalsRow, totalsRowParams)

        val rightPanel = LinearLayout(activity)
        rightPanel.orientation = LinearLayout.VERTICAL
        rightPanel.setBackgroundColor(getPanelBackgroundColor())

        val rightParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )

        rightPanel.setPadding(18, 14, 18, 14)

        val paymentTitle = TextView(activity)
        paymentTitle.text = "Payment"
        paymentTitle.textSize = 24f
        paymentTitle.setTextColor(getPrimaryTextColor())
        paymentTitle.setPadding(0, 0, 0, 8)

        val remainingBalanceLabel = TextView(activity)
        remainingBalanceLabel.text = "Remaining Balance"
        remainingBalanceLabel.textSize = 14f
        remainingBalanceLabel.setTextColor(getMutedTextColor())

        val remainingBalanceAmount = TextView(activity)
        remainingBalanceAmount.text = "$0.00"
        remainingBalanceAmount.textSize = 34f
        remainingBalanceAmount.setTextColor(getPrimaryTextColor())
        remainingBalanceAmount.setPadding(0, 0, 0, 12)

        val paymentAmountLabel = TextView(activity)
        paymentAmountLabel.text = "Payment Amount"
        paymentAmountLabel.textSize = 14f
        paymentAmountLabel.setTextColor(getMutedTextColor())

        val paymentAmountBox = EditText(activity)
        paymentAmountBox.hint = "Enter amount"
        paymentAmountBox.textSize = 18f
        paymentAmountBox.setSingleLine(true)
        styleInput(paymentAmountBox)

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

        val cashButtonColor = getCashButtonColor()

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

        styleButton(fiveButton, cashButtonColor)
        styleButton(tenButton, cashButtonColor)
        styleButton(twentyButton, cashButtonColor)
        styleButton(fiftyButton, cashButtonColor)
        styleButton(hundredButton, cashButtonColor)
        styleButton(exactButton, cashButtonColor)

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

        val applyPaymentButton = Button(activity)
        applyPaymentButton.text = "Apply Payment"

        val completeSaleButton = Button(activity)
        completeSaleButton.text = "Complete Sale"

        val cancelTransactionButton = Button(activity)
        cancelTransactionButton.text = "Cancel Transaction"

        styleButton(clearAmountButton, getWarningColor())
        styleButton(applyPaymentButton, getPositiveColor())
        styleButton(completeSaleButton, getNegativeColor())
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

        page.addView(leftSide, leftParams)
        page.addView(rightPanel, rightParams)

        return page
    }

    private fun makeHeaderCell(text: String): TextView {
        val cell = TextView(activity)
        cell.text = text
        cell.textSize = 15f
        cell.setTextColor(getPrimaryTextColor())
        return cell
    }

    private fun styleInput(input: EditText) {
        input.setTextColor(getPrimaryTextColor())
        input.setHintTextColor(getMutedTextColor())
        input.setBackgroundColor(getInputBackgroundColor())
    }

    private fun styleButton(button: Button, backgroundColor: Int) {
        button.setBackgroundColor(backgroundColor)
        button.setTextColor(Color.WHITE)
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

    private fun getMutedTextColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(180, 180, 180)
        } else {
            Color.GRAY
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
            Color.rgb(46, 204, 113)
        }
    }

    private fun getWarningColor(): Int {
        return Color.rgb(230, 159, 0)
    }

    private fun getNegativeColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(213, 94, 0)
        } else {
            Color.rgb(231, 76, 60)
        }
    }

    private fun getCashButtonColor(): Int {
        return if (isColorblindModeEnabled()) {
            Color.rgb(0, 114, 178)
        } else {
            Color.rgb(107, 142, 115)
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