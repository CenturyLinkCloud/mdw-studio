package com.centurylink.mdw.draw.edit.apply

import com.centurylink.mdw.draw.edit.WorkflowObj
import com.centurylink.mdw.model.asset.Pagelet

interface WidgetApplier {
    val workflowObj: WorkflowObj
    fun init(widget: Pagelet.Widget, workflowObj: WorkflowObj)
    fun update()
}

/**
 * Convert widget.value to/from model for persistence.
 */
abstract class AbstractWidgetApplier() : WidgetApplier {

    lateinit var widget: Pagelet.Widget
    override lateinit var workflowObj: WorkflowObj

    /**
     * Initialize the widget and set its value from the model.
     * Always call super.init() when overriding
     */
    override fun init(widget: Pagelet.Widget, workflowObj: WorkflowObj) {
        this.widget = widget
        this.workflowObj = workflowObj
    }

    /**
     * Update the workflowObj model from widget value.
     */
    abstract override fun update()
}
