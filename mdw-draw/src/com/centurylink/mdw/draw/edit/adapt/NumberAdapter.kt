package com.centurylink.mdw.draw.edit.adapt

import com.centurylink.mdw.draw.edit.apply.WidgetApplier
import com.centurylink.mdw.model.asset.Pagelet

@Suppress("unused")
class NumberAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {
    override fun didInit(widget: Pagelet.Widget) {
        widget.value?.let {
            // not reassigned if expression
            it.toString().toIntOrNull()?.let { i ->
                widget.value = i
            }
        }
    }

    override fun willUpdate(widget: Pagelet.Widget) {
        widget.value?.let {
            widget.value = widget.value.toString()
        }
    }
}