package com.centurylink.mdw.draw.edit.adapt

import com.centurylink.mdw.draw.edit.apply.WidgetApplier
import com.centurylink.mdw.draw.edit.label
import com.centurylink.mdw.model.asset.Pagelet.Widget

@Suppress("unused")
class EditAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {
    override fun didInit(widget: Widget) {
        widget.value?.let {
            widget.label += "*"
        }
    }
}