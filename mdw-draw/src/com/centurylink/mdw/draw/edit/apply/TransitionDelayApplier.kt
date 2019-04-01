package com.centurylink.mdw.draw.edit.apply

import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.model.asset.Pagelet

@Suppress("unused")
class TransitionDelayApplier : AttributeApplier() {

    override fun init(widget: Pagelet.Widget, workflowObj: WorkflowObj) {
        super.init(widget, workflowObj)
        workflowObj.getAttribute(widget.name)?.let { attr ->
            // translate to seconds for display
            if (attr.endsWith("h")) {
                widget.value = attr.substring(0, attr.length - 1).toInt() * 3600
            } else if (attr.endsWith("m")) {
                widget.value = attr.substring(0, attr.length - 1).toInt() * 60
            } else if (attr.endsWith("s")) {
                widget.value = attr.substring(0, attr.length - 1).toInt()
            } else {
                // no units is designer compatibility (mins)
                widget.value = if (attr == "0") {
                    null
                } else {
                    attr.toInt() * 60
                }
            }
        }
    }

    override fun update() {
        if (widget.value == null) {
            super.update()
        } else if (widget.value == 0 || widget.value == "0") {
            workflowObj.setAttribute(widget.name, null)
        } else {
            workflowObj.setAttribute(widget.name, "${widget.valueString}s")
        }
    }
}