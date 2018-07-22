package com.centurylink.mdw.studio.edit.apply

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.WorkflowObj
import com.centurylink.mdw.studio.edit.valueString

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