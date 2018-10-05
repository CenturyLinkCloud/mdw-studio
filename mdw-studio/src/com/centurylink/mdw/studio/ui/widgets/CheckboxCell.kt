package com.centurylink.mdw.studio.ui.widgets

import java.awt.Component
import java.lang.Boolean.parseBoolean
import javax.swing.JCheckBox
import javax.swing.JTable
import javax.swing.UIManager
import javax.swing.border.EmptyBorder
import javax.swing.table.TableCellRenderer

class CheckboxCell(value: Boolean) : JCheckBox() {

    init {
        isBorderPainted = true
        isSelected = value
    }

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

class CheckboxCellRenderer : TableCellRenderer {

    override fun getTableCellRendererComponent(table: JTable, value: Any?,
            isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        val checkbox = CheckboxCell(parseBoolean(value?.toString()))
        checkbox.init(table, isSelected, hasFocus)
        return checkbox
    }
}