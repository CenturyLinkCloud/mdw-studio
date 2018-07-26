package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.*
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import org.json.JSONArray
import sun.swing.table.DefaultTableCellHeaderRenderer
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

@Suppress("unused")
class Table(widget: Pagelet.Widget, scrolling: Boolean = false) : SwingWidget(widget, BorderLayout()) {

    constructor(widget: Pagelet.Widget) : this(widget, false)

    private val table: JBTable
    private val tableModel: DefaultTableModel
    private val columnWidgets = mutableListOf<Pagelet.Widget>()
    private val rows = mutableListOf<Array<String>>()
    private val projectSetup = (widget.adapter as WidgetApplier).workflowObj.project as ProjectSetup

    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(5, 0, 5, 0)

        val tablePanel = JPanel(BorderLayout())
        tablePanel.border = BorderFactory.createMatteBorder(1, 1, 0, 0, JBColor.border())
        add(tablePanel)

        val columnLabels = mutableListOf<String>()
        for (i in widget.widgets.indices) {
            val columnWidget = widget.widgets[i]
            columnWidget.init("table", (widget.adapter as WidgetApplier).workflowObj)
            columnLabels.add(" " + columnWidget.label)
            columnWidgets.add(columnWidget)
        }

        // initialize rows from widget value
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

        tableModel = DefaultTableModel(rows.toTypedArray(), columnLabels.toTypedArray())
        tableModel.addTableModelListener { e ->
            if (e.column >= 0) {
                // otherwise is add/delete operation
                val value = tableModel.getValueAt(e.firstRow, e.column)
                rows[e.firstRow][e.column] = value?.toString() ?: ""
            }
            // update widget value from rows
            val updatedRowsArrJson = JSONArray()
            for (row in rows) {
                val rowArrJson = JSONArray()
                for (colVal in row) {
                    rowArrJson.put(colVal.trim())
                }
                updatedRowsArrJson.put(rowArrJson)
            }
            widget.value = updatedRowsArrJson
            applyUpdate()
        }

        table = object : JBTable(tableModel) {
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
            column.cellRenderer = getCellRenderer(columnWidget)
            column.cellEditor = getCellEditor(columnWidget)
            columnWidget.attributes["vw"]?.let {
                table.columnModel.getColumn(i).maxWidth = it.toInt()
            }
        }

        table.setRowHeight(24)

        if (scrolling) {
            val scrollPane = JBScrollPane(table)
            tablePanel.add(scrollPane)
        }
        else {
            tablePanel.add(header, BorderLayout.NORTH)
            tablePanel.add(table, BorderLayout.CENTER)
        }

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
        addButton.preferredSize = Dimension(addButton.preferredSize.width - 8, addButton.preferredSize.height - 4)
        btnPanel.add(addButton, BorderLayout.NORTH)
        addButton.addActionListener {
            val rowList = mutableListOf<String>()
            for (columnWidget in columnWidgets) {
                rowList.add(columnWidget.default ?: "")
            }
            val row = rowList.toTypedArray()
            rows.add(row)
            tableModel.addRow(row)
        }

        val delButton = JButton("Delete")
        btnPanel.add(delButton, BorderLayout.SOUTH)
        delButton.preferredSize = Dimension(delButton.preferredSize.width - 8, delButton.preferredSize.height - 4)
        delButton.addActionListener {
            if (table.selectedRow >= 0) {
                rows.removeAt(table.selectedRow)
                tableModel.removeRow(table.selectedRow)
            }
        }

        addButton.maximumSize = delButton.maximumSize

        return btnPanel
    }

    private fun getCellRenderer(widget: Pagelet.Widget): TableCellRenderer {
        return when (widget.type) {
            "checkbox" -> CheckboxCellRenderer()
            "asset" -> AssetCellRenderer(this@Table.widget.isReadonly, projectSetup)
            else -> DefaultTableCellRenderer()
        }
    }

    /**
     * Only certain widget types are specifically supported.
     */
    private fun getCellEditor(widget: Pagelet.Widget): TableCellEditor {
        return when (widget.type) {
            "checkbox" -> DefaultCellEditor(Checkbox(widget).checkbox)
            "dropdown" -> DefaultCellEditor(Dropdown(widget).combo)
            "asset" -> AssetCellEditor(this@Table.widget.isReadonly, projectSetup, widget.source)
            "number" -> NumberCellEditor()
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