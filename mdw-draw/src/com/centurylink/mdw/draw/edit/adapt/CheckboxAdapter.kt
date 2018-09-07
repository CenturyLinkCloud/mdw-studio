package com.centurylink.mdw.draw.edit.adapt

import com.centurylink.mdw.draw.edit.apply.WidgetApplier
import com.centurylink.mdw.model.asset.Pagelet.Widget

@Suppress("unused")
class CheckboxAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {

    override fun didInit(widget: Widget) {
        widget.value = widget.value != null && widget.value.toString().toBoolean()
    }

    override fun willUpdate(widget: Widget) {
        widget.value = widget.value.toString()
    }
}