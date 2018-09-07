package com.centurylink.mdw.studio.config.widgets

import com.intellij.ui.JBIntSpinner
import java.awt.Component
import java.util.*
import javax.swing.AbstractCellEditor
import javax.swing.JTable
import javax.swing.UIManager
import javax.swing.border.EmptyBorder
import javax.swing.table.TableCellEditor

class NumberCell(value: Int, min: Int = 0, max: Int = 1000) : JBIntSpinner(value, min, max) {

    fun init(table: JTable, isSelected: Boolean, hasFocus: Boolean) {
        if (isSelected) {
            foreground = table.selectionForeground
            background = table.selectionBackground
        }
        else {
            foreground = table.foreground
            background = table.background
        }

        if (hasFocus) {
            border = UIManager.getBorder("Table.focusCellHighlightBorder")
        }
        else {
            border = EmptyBorder(1, 1, 1, 1)
        }
    }
}

class NumberCellEditor() : AbstractCellEditor(), TableCellEditor {

    val numberCell = NumberCell(0)

    override fun getTableCellEditorComponent(table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
        numberCell.number = if (value?.toString().isNullOrBlank()) 0 else value.toString().toInt()
        numberCell.init(table, isSelected, true)
        return numberCell
    }

    override fun getCellEditorValue(): Any? {
        return numberCell.number
    }
}