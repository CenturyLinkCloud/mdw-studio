package com.centurylink.mdw.draw.edit.adapt

import com.centurylink.mdw.draw.edit.apply.WidgetApplier
import com.centurylink.mdw.draw.edit.units
import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.model.asset.Pagelet.Widget
import java.lang.Integer.parseInt

/**
 * to/from Int value in seconds
 */
@Suppress("unused")
class DatetimeAdapter(val applier: WidgetApplier) : WidgetAdapter(applier) {

    private fun getOldStyleUnitsAttr(widget: Widget): String {
        return when (widget.name) {
            "TIMER_WAIT" -> "Unit"
            else -> "SLA_UNIT"
        }
    }

    /**
     * Old units attr saved by Designer (converted to seconds)
     */
    private fun getOldStyleSeconds(widget: Widget): Int? {
        val oldUnitsAttr = getOldStyleUnitsAttr(widget)
        val oldUnitsValue = workflowObj.getAttribute(oldUnitsAttr)
        if (oldUnitsValue != null) {
            widget.setAttribute(oldUnitsAttr, null)
            if (!widget.valueString.isNullOrBlank()) {
                return when (oldUnitsValue) {
                    "Minutes" -> parseInt(widget.valueString) * 60
                    "Hours" -> parseInt(widget.valueString) * 3600
                    "Days" -> parseInt(widget.valueString) * 86400
                    else -> parseInt(widget.valueString)
                }
            }
        }
        return null
    }

    override fun didInit(widget: Widget) {
        val oldStyleUnitsAttr = getOldStyleUnitsAttr(widget)

        widget.units = workflowObj.getAttribute("${widget.name}_DISPLAY_UNITS")
        if (widget.units == null) {
            // try old value saved by hub/studio
            widget.units = workflowObj.getAttribute("SLA_UNITS")
            workflowObj.setAttribute("SLA_UNITS", null)
        }
        if (widget.units == null) {
            // try old Designer style
            val oldStyleUnits = workflowObj.getAttribute(oldStyleUnitsAttr)
            if (oldStyleUnits != null) {
                widget.units = oldStyleUnits
            }
        }
        if (widget.units == null) {
            widget.units = "Hours"
        }

        val oldStyleSeconds = try {
            getOldStyleSeconds(widget)
        } catch (ex: NumberFormatException) {
            null
        }
        if (oldStyleSeconds != null) {
            widget.value = oldStyleSeconds
        }
        widget.value?.let {
            try {
                when (widget.units) {
                    "Minutes" -> widget.value = parseInt(it.toString()) / 60
                    "Hours" -> widget.value = parseInt(it.toString()) / 3600
                    "Days" -> widget.value = parseInt(it.toString()) / 86400
                }
            } catch (ex: NumberFormatException) {
                // value must be an expression
            }

        }
        workflowObj.setAttribute(oldStyleUnitsAttr, null)
        workflowObj.setAttribute("${widget.name}_DISPLAY_UNITS", widget.units)
    }

    override fun willUpdate(widget: Widget) {
        widget.value?.let {
            try {
                when (widget.units) {
                    "Minutes" -> widget.value = parseInt(it.toString()) * 60
                    "Hours" -> widget.value = parseInt(it.toString()) * 3600
                    "Days" -> widget.value = parseInt(it.toString()) * 86400
                }
            } catch (ex: NumberFormatException) {
                // must be an expression
            }
        }
        workflowObj.setAttribute("${widget.name}_DISPLAY_UNITS", widget.units)
    }
}