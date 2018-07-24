package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.edit.init
import com.centurylink.mdw.studio.edit.isReadonly
import com.centurylink.mdw.studio.edit.label
import com.intellij.ui.JBColor
import com.intellij.ui.table.JBTable
import org.json.JSONArray
import sun.swing.table.DefaultTableCellHeaderRenderer
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class Table(widget: Pagelet.Widget) : SwingWidget(widget, BorderLayout()) {

    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(5, 0, 5, 0)

        val tablePanel = JPanel(BorderLayout())
        tablePanel.border = BorderFactory.createMatteBorder(1, 1, 0, 0, JBColor.border())
        add(tablePanel)

        val columnLabels = mutableListOf<String>()
        val rows = mutableListOf<Array<String>>()
        val rowsArrJson = widget.value as JSONArray
        for (i in 0 until rowsArrJson.length()) {
            val row = mutableListOf<String>()
            val rowArrJson = rowsArrJson.getJSONArray(i)
            for (j in 0 until rowArrJson.length()) {
                val colVal = rowArrJson.getString(j)
                row.add(colVal)
            }
            rows.add(row.toTypedArray())
        }

        for (columnWidget in widget.widgets) {
            columnWidget.init("table", (widget.adapter as WidgetApplier).workflowObj)
            val swingWidget = createSwingWidget(columnWidget)
            swingWidget.addUpdateListener { obj ->
                println("UPDATES")
            }
            columnLabels.add(" " + columnWidget.label)
        }

        val tableModel = DefaultTableModel(rows.toTypedArray(), columnLabels.toTypedArray())
        tableModel.addTableModelListener { e ->
            val colName = tableModel.getColumnName(e.column)
            println("CHANGE " + colName + "[" + e.firstRow + "] = " + tableModel.getValueAt(e.firstRow, e.column))
        }


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