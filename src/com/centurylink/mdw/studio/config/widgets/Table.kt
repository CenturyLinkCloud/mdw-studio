package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.isReadonly
import com.centurylink.mdw.studio.edit.label
import com.intellij.ui.JBColor
import com.intellij.ui.table.JBTable
import sun.swing.table.DefaultTableCellHeaderRenderer
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.BoxLayout



class Table(widget: Pagelet.Widget) : SwingWidget(widget, BorderLayout()) {

    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(5, 0, 5, 0)

        val tablePanel = JPanel(BorderLayout())
        tablePanel.border = BorderFactory.createMatteBorder(1, 1, 0, 0, JBColor.border())
        add(tablePanel)

        val columnLabels = mutableListOf<String>()
        for (columnWidget in widget.widgets) {
            val swingWidget = createSwingWidget(columnWidget)
            columnLabels.add(" " + columnWidget.label)
        }

        val tableModel = DefaultTableModel(columnLabels.toTypedArray(), 6)
        val table = object : JBTable(tableModel) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return if (widget.isReadonly) false else super.isCellEditable(row, column)
            }
        }
        val header = table.tableHeader
        val headerCellRenderer = header.defaultRenderer as DefaultTableCellHeaderRenderer
        headerCellRenderer.horizontalAlignment = DefaultTableCellRenderer.LEADING

        tablePanel.add(header, BorderLayout.NORTH)
        tablePanel.add(table, BorderLayout.CENTER)

        if (!widget.isReadonly) {
            add(getButtonPanel(), BorderLayout.EAST)
        }
    }

    private fun getButtonPanel(): JPanel {
        val btnPanel = JPanel()
        btnPanel.layout = BoxLayout(btnPanel, BoxLayout.Y_AXIS)
        btnPanel.isOpaque = false
        btnPanel.border = BorderFactory.createEmptyBorder(15, 5, 0, 0)

        val addButton = JButton("Add")
        btnPanel.add(addButton, BorderLayout.NORTH)
        addButton.addActionListener {
            println("ADD")
        }

        val delButton = JButton("Delete")
        btnPanel.add(delButton, BorderLayout.SOUTH)
        delButton.addActionListener {
            println("DEL")
        }

        addButton.maximumSize = delButton.maximumSize

        return btnPanel
    }

    private fun createSwingWidget(widget: Pagelet.Widget): SwingWidget {
        val swingWidget = SwingWidget.create(widget)
        swingWidget.addUpdateListener { obj ->
            notifyUpdateListeners(obj)
        }
        return swingWidget
    }
}