package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.intellij.ui.JBColor
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.table.DefaultTableModel

class Table(widget: Pagelet.Widget) : SwingWidget(widget, BorderLayout()) {

    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(5, 0, 5, 0)

        val table = JBTable(DefaultTableModel(5, 3))
        table.border = BorderFactory.createLineBorder(JBColor.border())
        add(table)

    }
}