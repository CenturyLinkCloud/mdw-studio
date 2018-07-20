package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.apply.WidgetApplier

class CheckboxAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {

    override fun didInit(widget: Widget) {
        widget.value = widget.value != null && widget.value.toString().toBoolean()
    }

    override fun willUpdate(widget: Widget) {
        widget.value = widget.value.toString()
    }
}