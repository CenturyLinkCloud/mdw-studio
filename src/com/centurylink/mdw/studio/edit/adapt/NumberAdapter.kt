package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.edit.units

@Suppress("unused")
class NumberAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {
    override fun didInit(widget: Pagelet.Widget) {
        widget.value?.let {
            widget.value = Integer.parseInt(it.toString())
        }
    }

    override fun willUpdate(widget: Pagelet.Widget) {
        widget.value?.let {
            widget.value = widget.value.toString()
        }
    }
}