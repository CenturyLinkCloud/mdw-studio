package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.label
import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.model.asset.Pagelet
import javax.swing.BorderFactory
import javax.swing.JCheckBox

@Suppress("unused")
class Checkbox(widget: Pagelet.Widget) : SwingWidget(widget) {

    val checkbox = JCheckBox()

    private fun hasExpression(it: String?): Boolean {
        return !it.isNullOrBlank() && !it.equals("true", true) &&
                !it.equals("false", true)
    }

    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(0, 1, 0, 0)

        checkbox.isOpaque = false

        widget.valueString?.let {
            if (hasExpression(it)) {
                checkbox.isEnabled = false
            }
            else {
                checkbox.isSelected = it.toBoolean()
            }
        }

        checkbox.addItemListener {
            widget.value = checkbox.isSelected
            applyUpdate()
        }
        add(checkbox)

        // exclude table widgets and specials
        if (widget.name?.startsWith("_") != true) {
            val expressionEntry = ExpressionEntry(widget.label, widget.valueString) {
                val hasExpr = hasExpression(it)
                // set widget value before triggering checkbox listener so that 'true' and 'false' are honored
                widget.value = it
                checkbox.isEnabled = !hasExpr
                checkbox.isSelected = if (hasExpr) {
                    false
                } else {
                    widget.valueString?.toBoolean() ?: false
                }
                // set widget value again in case checkbox listener set to false for expression
                widget.value = it
                applyUpdate()
                hasExpr
            }
            add(expressionEntry)
        }
    }
}