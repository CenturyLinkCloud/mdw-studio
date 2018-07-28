package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.edit.valueString

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