package com.centurylink.mdw.studio.config.widgets

import java.awt.Component
import java.lang.Boolean.parseBoolean
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.UIManager
import javax.swing.border.EmptyBorder
import javax.swing.table.TableCellRenderer

class CheckboxCellRenderer : JCheckBox(), TableCellRenderer {

    private val noFocusBorder = EmptyBorder(1, 1, 1, 1)

    init {
        isBorderPainted = true
    }

    override fun getTableCellRendererComponent(table: JTable, value: Any?,
            isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        if (isSelected) {
            foreground = table.selectionForeground
            background = table.selectionBackground
        }
        else {
            foreground = table.foreground
            background = table.background
        }

        setSelected(parseBoolean(value?.toString()))

        if (hasFocus) {
            border = UIManager.getBorder("Table.focusCellHighlightBorder")
        }
        else {
            border = noFocusBorder
        }
        return this
    }
}