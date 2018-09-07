package com.centurylink.mdw.draw.edit.adapt

import com.centurylink.mdw.draw.edit.apply.WidgetApplier
import com.centurylink.mdw.draw.edit.units
import com.centurylink.mdw.model.asset.Pagelet.Widget
import java.lang.Integer.parseInt

/**
 * to/from Int value in seconds
 */
@Suppress("unused")
class DatetimeAdapter(val applier: WidgetApplier) : WidgetAdapter(applier) {

    override fun didInit(widget: Widget) {
        widget.units = workflowObj.getAttribute(widget.name + "_UNITS")
        if (widget.units == null) {
            widget.units = "Hours"
        }
        widget.value?.let {
            when (widget.units) {
                "Minutes" -> widget.value = parseInt(it.toString()) / 60
                "Hours" -> widget.value = parseInt(it.toString()) / 3600
                "Days" -> widget.value = parseInt(it.toString()) / 86400
            }
        }
    }

    override fun willUpdate(widget: Widget) {
        widget.value?.let {
            when (widget.units) {
                "Minutes" -> widget.value = widget.value as Int * 60
                "Hours" -> widget.value = widget.value as Int * 3600
                "Days" -> widget.value = widget.value as Int * 86400
            }
        }
        workflowObj.setAttribute(widget.name + "_UNITS", widget.units)
    }
}