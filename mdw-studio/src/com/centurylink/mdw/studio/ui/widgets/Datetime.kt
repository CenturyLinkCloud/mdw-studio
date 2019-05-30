package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.label
import com.centurylink.mdw.draw.edit.units
import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.model.asset.Pagelet
import com.intellij.ui.JBIntSpinner
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JFormattedTextField
import javax.swing.JRadioButton
import javax.swing.text.DefaultFormatter

/**
 * This is actually for specifying a time interval
 */
@Suppress("unused")
class Datetime(widget: Pagelet.Widget) : SwingWidget(widget) {

    private fun hasExpression(it: String?): Boolean {
        if (it.isNullOrBlank()) {
            return false
        }
        return try {
            it.toInt()
            false
        } catch (ex: NumberFormatException) {
            true
        }
    }

    init {
        isOpaque = false

        var num = 0
        widget.value?.let {
            num = if (it is String) {
                try {
                    it.toInt()
                } catch (ex: NumberFormatException) {
                    0 // must be an expression
                }

            } else {
                it as Int
            }
        }
        val spinner = object : JBIntSpinner(num, 0, 10000) {
            override fun getPreferredSize(): Dimension {
                val size = super.getPreferredSize()
                return Dimension(size.width, size.height - 2)
            }
        }
        spinner.isOpaque = false

        val field = spinner.editor.getComponent(0) as JFormattedTextField
        (field.formatter as DefaultFormatter).commitsOnValidEdit = true
        spinner.addChangeListener {
            widget.value = spinner.number
            applyUpdate()
        }
        spinner.editor.addPropertyChangeListener {  }
        add(spinner)

        // units
        val buttonGroup = ButtonGroup()
        val radioButtons = mutableListOf<JRadioButton>()
        val units = widget.attributes["units"] ?: "Minutes,Hours,Days"
        for (option in units.split(",")) {
            val radioButton = JRadioButton(option)
            radioButtons.add(radioButton)
            radioButton.isOpaque = false
            radioButton.border = BorderFactory.createEmptyBorder(0, 0, 0, 7)
            radioButton.actionCommand = option
            if (option == widget.units) {
                radioButton.isSelected = true
            }
            radioButton.addActionListener {
                // convert value to reflect new units
                widget.units = it.actionCommand
                applyUpdate()
            }
            buttonGroup.add(radioButton)
            add(radioButton)
        }

        if (hasExpression(widget.valueString)) {
            spinner.isEnabled = false
            for (radioButton in radioButtons) {
                radioButton.isSelected = radioButton.text == "Seconds"
                radioButton.isEnabled = false
            }
        }

        // expression
        if (widget.name?.startsWith("_") != true) {
            val expressionEntry = ExpressionEntry("${widget.label} (in seconds)", widget.valueString) {
                val hasExpr = hasExpression(it)
                // set widget value before triggering spinner listener so that 'true' and 'false' are honored
                widget.value = it
                spinner.isEnabled = !hasExpr
                spinner.value = if (hasExpr || widget.valueString.isNullOrBlank()) {
                    0
                } else {
                    widget.valueString?.toInt() ?: 0
                }
                for (radioButton in radioButtons) {
                    if (hasExpr) {
                        radioButton.isSelected = radioButton.text == "Seconds"
                    }
                    radioButton.isEnabled = !hasExpr
                }
                // set widget value again in case spinner listener set to false for expression
                widget.value = it
                applyUpdate()
                hasExpr
            }
            add(expressionEntry)
        }
    }
}