package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import org.json.JSONArray

class PicklistAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {

    override fun didInit(widget: Widget) {
        widget.value?.let {
            widget.value = toArray(it.toString())
        }
    }

    override fun willUpdate(widget: Widget) {
        widget.value?.let {
            widget.value = (it as JSONArray).toString()
        }
    }
}