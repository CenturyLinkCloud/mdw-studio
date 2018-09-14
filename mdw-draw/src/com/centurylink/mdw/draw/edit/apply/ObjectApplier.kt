package com.centurylink.mdw.draw.edit.apply

import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.model.asset.Pagelet

open class ObjectApplier : AbstractWidgetApplier() {

    override fun init(widget: Pagelet.Widget, workflowObj: WorkflowObj) {
        super.init(widget, workflowObj)
        widget.value = workflowObj.get(widget.name)
    }

    override fun update() {
        if (widget.valueString.isNullOrBlank()) {
            workflowObj.remove(widget.name)
        }
        else {
            workflowObj.set(widget.name, widget.value)
        }
    }
}