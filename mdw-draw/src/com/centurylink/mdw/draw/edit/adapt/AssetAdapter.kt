package com.centurylink.mdw.draw.edit.adapt

import com.centurylink.mdw.draw.edit.apply.WidgetApplier
import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.model.asset.Pagelet

@Suppress("unused")
class AssetAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {
    override fun didInit(widget: Pagelet.Widget) {
        widget.valueString?.let {
            if (widget.name == "processname" && !it.endsWith(".proc")) {
                widget.value = it + ".proc"
            }
        }
    }
}