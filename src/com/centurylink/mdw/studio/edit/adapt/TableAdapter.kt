package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.edit.default
import org.json.JSONArray

open class TableAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {

    /**
     * Initialize with a single empty row if null or empty.
     */
    override fun didInit(widget: Widget) {
        if (widget.value == null) {
            widget.value = JSONArray()
            val row = createEmptyRow(widget)
            (widget.value as JSONArray).put(row)
        }
        else {
            widget.value = toTable(widget.value.toString())
        }
    }

    override fun willUpdate(widget: Widget) {
        widget.value?.let {
            widget.value = withoutEmptyRows(it as JSONArray)
        }
    }

    protected fun createEmptyRow(tableWidget: Widget): JSONArray {
        val row = JSONArray()
        for (columnWidget in tableWidget.widgets) {
            row.put(columnWidget.default ?: "")
        }
        return row
    }

    /**
     * Clean out any empty rows (returns null if left with none).
     */
    protected fun withoutEmptyRows(rows: JSONArray): JSONArray? {
        val nonEmptyRows = JSONArray()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONArray(i)
            if (!isEmpty(row)) {
                nonEmptyRows.put(row)
            }
        }
        return if (nonEmptyRows.length() == 0) null else nonEmptyRows
    }

    protected fun isEmpty(row: JSONArray): Boolean {
        for (i in 0 until row.length()) {
            if (row.getString(i).isNotEmpty()) {
                return false
            }
        }
        return true
    }

    /**
     * handles compatibility for old process def attributes
     */
    fun toTable(value: String): JSONArray {
        if (value.startsWith('[')) {
            return JSONArray(value)
        }
        else {
            val table = JSONArray()
            val rows = safeSplit(value, ";")
            for (i in 0 until rows.length()) {
                val row = rows.getString(i)
                val cols = safeSplit(row, ",")
                table.put(cols)
            }
            return table
        }
    }
}