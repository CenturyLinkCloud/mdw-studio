package com.centurylink.mdw.draw.edit.adapt

import com.centurylink.mdw.draw.edit.apply.WidgetApplier
import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.model.asset.Pagelet
import org.json.JSONArray

@Suppress("unused")
class SearchTableAdapter(applier: WidgetApplier) : TableAdapter(applier) {

    override fun didInit(widget: Pagelet.Widget) {
        if (widget.value == null) {
            widget.value = JSONArray()
        }
        else {
            widget.value = JSONArray(widget.value.toString())
        }
        println(widget.valueString)
    }

    override fun willUpdate(widget: Pagelet.Widget) {
        super.willUpdate(widget)
    }
}