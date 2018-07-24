package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import org.json.JSONArray

class TableAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {
    override fun didInit(widget: Widget) {
        // initialize with single empty row
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

    override fun willUpdate(widget: Widget) {
        // TODO: clean out empty rows (value is null if left with none)

        widget.value?.let {
            widget.value = (it as JSONArray).toString()
        }
    }
}