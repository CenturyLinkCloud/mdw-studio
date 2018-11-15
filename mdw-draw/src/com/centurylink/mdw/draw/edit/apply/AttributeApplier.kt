package com.centurylink.mdw.draw.edit.apply

import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.model.asset.Pagelet

open class AttributeApplier : AbstractWidgetApplier() {

    override fun init(widget: Pagelet.Widget, workflowObj: WorkflowObj) {
        super.init(widget, workflowObj)
        widget.value = workflowObj.getAttribute(widget.name)
    }

    override fun update() {
        workflowObj.setAttribute(widget.name, widget.valueString)
    }
}