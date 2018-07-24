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
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor

class Table(widget: Pagelet.Widget) : SwingWidget(widget, BorderLayout()) {

    val columnWidgets = mutableListOf<Pagelet.Widget>()

    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(5, 0, 5, 0)

        val tablePanel = JPanel(BorderLayout())
        tablePanel.border = BorderFactory.createMatteBorder(1, 1, 0, 0, JBColor.border())
        add(tablePanel)

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

        val columnLabels = mutableListOf<String>()
        for (i in widget.widgets.indices) {
            val columnWidget = widget.widgets[i]
            columnWidget.init("table", (widget.adapter as WidgetApplier).workflowObj)
            val swingWidget = createSwingWidget(columnWidget)
            columnLabels.add(" " + columnWidget.label)
            columnWidgets.add(columnWidget)
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


        for (i in columnWidgets.indices) {
            val columnWidget = columnWidgets[i]
            val column = table.columnModel.getColumn(i)
            column.cellEditor = getCellEditor(columnWidget)
        }

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

    private fun getCellEditor(widget: Pagelet.Widget): TableCellEditor {
        return when (widget.type) {
            "checkbox" -> DefaultCellEditor(Checkbox(widget).checkbox)
            "dropdown" -> DefaultCellEditor(Dropdown(widget).combo)
            else -> DefaultCellEditor(Text(widget).textComponent as JTextField)
        }
    }

    private fun createSwingWidget(widget: Pagelet.Widget): SwingWidget {
        val swingWidget = SwingWidget.create(widget)
        swingWidget.addUpdateListener { obj ->
            notifyUpdateListeners(obj)
        }
        return swingWidget
    }
}