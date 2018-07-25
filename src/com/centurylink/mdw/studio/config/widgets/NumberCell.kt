package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.valueString
import com.intellij.ui.JBIntSpinner
import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.JTable
import javax.swing.UIManager
import javax.swing.border.EmptyBorder
import javax.swing.table.TableCellEditor

class NumberCell(val value: Int, val min: Int = 0, val max: Int = 1000) {

    private val noFocusBorder = EmptyBorder(1, 1, 1, 1)

    fun getComponent(table: JTable, isSelected: Boolean, hasFocus: Boolean): Component {

        val spinner = JBIntSpinner(value, min, max)

        if (isSelected) {
            spinner.foreground = table.selectionForeground
            spinner.background = table.selectionBackground
        }
        else {
            spinner.foreground = table.foreground
            spinner.background = table.background
        }

        if (hasFocus) {
            spinner.border = UIManager.getBorder("Table.focusCellHighlightBorder")
        }
        else {
            spinner.border = noFocusBorder
        }
        return spinner
    }
}

class NumberCellEditor(val widget: Pagelet.Widget) : AbstractCellEditor(), TableCellEditor {

    override fun getTableCellEditorComponent(table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
        val number = if (widget.valueString.isNullOrBlank()) 0 else widget.valueString!!.toInt()
        return NumberCell(number).getComponent(table, isSelected, true)
    }

    override fun getCellEditorValue(): Any? {
        return widget.value
    }
}