package com.centurylink.mdw.studio.edit.apply

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.WorkflowObj
import com.centurylink.mdw.studio.edit.valueString

open class AttributeApplier : AbstractWidgetApplier() {

    override fun init(widget: Pagelet.Widget, workflowObj: WorkflowObj) {
        super.init(widget, workflowObj)
        widget.value = workflowObj.getAttribute(widget.name)
        if (widget.value == null)
            widget.value = workflowObj.get(widget.name)
    }

    override fun update() {
        workflowObj.setAttribute(widget.name, widget.valueString)
    }
}