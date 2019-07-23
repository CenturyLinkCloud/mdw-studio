package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.label
import com.centurylink.mdw.draw.edit.max
import com.centurylink.mdw.draw.edit.min
import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.model.asset.Pagelet
import com.intellij.ui.JBIntSpinner
import java.awt.Dimension
import javax.swing.JFormattedTextField
import javax.swing.text.DefaultFormatter

@Suppress("unused")
class Number(widget: Pagelet.Widget) : SwingWidget(widget) {

    private fun hasExpression(it: String?): Boolean {
        if (it.isNullOrBlank()) {
            return false
        }
        return it.toIntOrNull() == null
    }

    init {
        isOpaque = false

        var min = 0
        widget.min?.let {
            min = it
        }
        var max = 1000
        widget.max?.let {
            max = it
        }

        var num = 0
        widget.value?.let {
            num = if (it is String) {
                it.toIntOrNull() ?: 0

            } else {
                it as Int
            }
        }
        if (num < min) {
            num = min
        }
        else if (num > max) {
            num = max
        }

        val spinner = object : JBIntSpinner(num, min, max) {
            override fun getPreferredSize(): Dimension {
                val size = super.getPreferredSize()
                return Dimension(size.width, size.height - 2)
            }
        }
        spinner.isOpaque = false

        val field = spinner.editor.getComponent(0) as JFormattedTextField
        (field.formatter as DefaultFormatter).commitsOnValidEdit = true

        spinner.addChangeListener {
            widget.value = spinner.number.toString()
            applyUpdate()
        }

        add(spinner)

        if (hasExpression(widget.valueString)) {
            spinner.isEnabled = false
        }
        // expression
        if (widget.name?.startsWith("_") != true) {
            val expressionEntry = ExpressionEntry(widget.label, widget.valueString) {
                val hasExpr = hasExpression(it)
                // set widget value before triggering spinner listener so that 'true' and 'false' are honored
                widget.value = it
                spinner.isEnabled = !hasExpr
                spinner.value = if (hasExpr || widget.valueString.isNullOrBlank()) {
                    0
                } else {
                    widget.valueString?.toInt() ?: 0
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