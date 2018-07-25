package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import org.json.JSONArray

/**
 * Converts workflowObj object values to table form for Configurator display.
 * The process Variables tab is one usage.
 */
class ObjectTableAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {
    override fun didInit(widget: Pagelet.Widget) {
        widget.value = JSONArray()
        val row = JSONArray()
        for (columnWidget in widget.widgets) {
            row.put("")
        }
        (widget.value as JSONArray).put(row)
    }

    override fun willUpdate(widget: Pagelet.Widget) {
    }
}