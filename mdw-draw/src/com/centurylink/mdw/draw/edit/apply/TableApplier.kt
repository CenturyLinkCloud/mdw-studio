package com.centurylink.mdw.draw.edit.apply

import com.centurylink.mdw.draw.edit.WorkflowObj
import com.centurylink.mdw.model.asset.Pagelet

/**
 * Dummy applier for widgets contained within tables.
 * Does not apply any values since the outer table widget's applier will do that.
 */
class TableApplier : AbstractWidgetApplier() {

    override fun init(widget: Pagelet.Widget, workflowObj: WorkflowObj) {
        super.init(widget, workflowObj)
    }

    override fun update() {
    }
}