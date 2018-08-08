package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.*
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import org.json.JSONArray
import sun.swing.table.DefaultTableCellHeaderRenderer
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.*
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

@Suppress("unused")
open class Table(widget: Pagelet.Widget, private val scrolling: Boolean = false,
        private val withButtons: Boolean = true) : SwingWidget(widget, BorderLayout()) {

    constructor(widget: Pagelet.Widget) : this(widget, false, true)

    protected val workflowObj = (widget.adapter as WidgetApplier).workflowObj
    protected val projectSetup = workflowObj.project as ProjectSetup

    private val columnWidgets: List<Pagelet.Widget> by lazy {
        initialColumnWidgets()
    }

    open val rows: MutableList<Array<String>> by lazy {
        initialRows()
    }

    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
    }

    private var _initialized = false
    private val initialized: Boolean
        get() {
            val was = _initialized
            _initialized = true
            return was
        }

    private fun createAndAddTable() {
        val tablePanel = JPanel(BorderLayout())
        if (!scrolling) {
            tablePanel.border = BorderFactory.createMatteBorder(1, 1, 0, 0, JBColor.border())
        }
        add(tablePanel)

        val columnLabels = mutableListOf<String>()
        columnWidgets.forEach { columnLabels.add(" " + it.label)}

        val tableModel = DefaultTableModel(rows.toTypedArray(), columnLabels.toTypedArray())
        tableModel.addTableModelListener { e ->
            if (e.column >= 0) {
                // otherwise is add/delete operation
                val value = tableModel.getValueAt(e.firstRow, e.column)
                rows[e.firstRow][e.column] = value?.toString() ?: ""
            }
            // update widget value from rows
            widget.value = widgetValueFromRows(rows)
            applyUpdate()
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
            column.cellRenderer = getCellRenderer(columnWidget)
            column.cellEditor = getCellEditor(columnWidget)
            columnWidget.attributes["vw"]?.let {
                table.columnModel.getColumn(i).maxWidth = it.toInt()
            }
        }

        table.rowHeight = 24

        if (scrolling) {
            val scrollPane = JBScrollPane(table, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_AS_NEEDED)
            tablePanel.add(scrollPane)
        }
        else {
            tablePanel.add(header, BorderLayout.NORTH)
            tablePanel.add(table, BorderLayout.CENTER)
        }
        tablePanel.revalidate()
        tablePanel.repaint()

        if (!widget.isReadonly && withButtons) {
            add(createButtonPanel(table), BorderLayout.EAST)
        }
    }

    override fun paintComponent(g: Graphics) {
        if (!initialized) {
            createAndAddTable()
        }
        super.paintComponent(g)
    }

    protected open fun initialColumnWidgets(): List<Pagelet.Widget> {
        val colWidgs = mutableListOf<Pagelet.Widget>()
        for (i in widget.widgets.indices) {
            val colWidg = widget.widgets[i]
            colWidg.init("table", workflowObj)
            colWidgs.add(colWidg)
        }
        return colWidgs
    }

    protected open fun initialRows(): MutableList<Array<String>> {
        val rows = mutableListOf<Array<String>>()
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
        return rows
    }

    protected open fun widgetValueFromRows(rows: List<Array<String>>): Any {
        // update widget value from rows
        val updatedRowsArrJson = JSONArray()
        for (row in rows) {
            val rowArrJson = JSONArray()
            for (colVal in row) {
                rowArrJson.put(colVal.trim())
            }
            updatedRowsArrJson.put(rowArrJson)
        }
        return updatedRowsArrJson
    }

    protected open fun createButtonPanel(table: JTable): JPanel {
        val btnPanel = JPanel()
        btnPanel.layout = BoxLayout(btnPanel, BoxLayout.Y_AXIS)
        btnPanel.isOpaque = false
        btnPanel.border = BorderFactory.createEmptyBorder(15, 5, 0, 0)

        val addButton = JButton(AllIcons.General.Add)
        addButton.isOpaque = false
        addButton.preferredSize = Dimension(addButton.preferredSize.width - 8, addButton.preferredSize.height - 2)
        addButton.toolTipText = "Add"
        btnPanel.add(addButton, BorderLayout.NORTH)
        addButton.addActionListener {
            val rowList = mutableListOf<String>()
            for (columnWidget in columnWidgets) {
                rowList.add(columnWidget.default ?: "")
            }
            val row = rowList.toTypedArray()
            rows.add(row)
            (table.model as DefaultTableModel).addRow(row)
        }

        val delButton = JButton(AllIcons.General.Remove)
        delButton.isOpaque = false
        delButton.preferredSize = Dimension(delButton.preferredSize.width - 8, delButton.preferredSize.height - 2)
        delButton.toolTipText = "Delete"
        btnPanel.add(delButton, BorderLayout.SOUTH)
        delButton.addActionListener {
            if (table.selectedRow >= 0) {
                rows.removeAt(table.selectedRow)
                (table.model as DefaultTableModel).removeRow(table.selectedRow)
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