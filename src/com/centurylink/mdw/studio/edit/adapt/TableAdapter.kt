package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import org.json.JSONArray

class TableAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {

    /**
     * Initialize with a single empty row if null or empty.
     */
    override fun didInit(widget: Widget) {
        if (widget.value == null) {
            widget.value = JSONArray()
            val row = JSONArray()
            for (columnWidget in widget.widgets) {
                row.put("")
            }
            (widget.value as JSONArray).put(row)
        }
        else {
            widget.value = toTable(widget.value.toString())
        }
    }

    /**
     * Clean out any empty rows (value is null if left with none).
     */
    override fun willUpdate(widget: Widget) {
        widget.value?.let {
            val rows = (it as JSONArray)
            val nonEmptyRows = JSONArray()
            for (i in 0 until rows.length()) {
                val row = rows.getJSONArray(i)
                if (!isEmpty(row)) {
                    nonEmptyRows.put(row)
                }
            }
            widget.value = if (nonEmptyRows.length() == 0) null else nonEmptyRows.toString()
        }
    }

    private fun isEmpty(row: JSONArray): Boolean {
        for (i in 0 until row.length()) {
            if (row.getString(i).isNotEmpty()) {
                return false
            }
        }
        return true
    }
}