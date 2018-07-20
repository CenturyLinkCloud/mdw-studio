package com.centurylink.mdw.studio.edit.apply

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.WorkflowObj

class ProcessAttributeApplier : AttributeApplier() {
    override fun init(widget: Pagelet.Widget, workflowObj: WorkflowObj) {
        super.init(widget, workflowObj)
        when (widget.name) {
            "_isService" -> {
                widget.value = workflowObj.getAttribute("PROCESS_VISIBILITY") == "SERVICE"
            }
        }
    }

    override fun update() {
        when (widget.name) {
            "_isService" -> {
                workflowObj.setAttribute("PROCESS_VISIBILITY", if (widget.value == true) "SERVICE" else "PUBLIC")
            }
        }
    }
}