package com.centurylink.mdw.draw.edit.adapt

import com.centurylink.mdw.draw.edit.apply.WidgetApplier
import com.centurylink.mdw.model.asset.Pagelet

class VariableTypeAdapter(private val applier: WidgetApplier) : WidgetAdapter(applier) {

    override fun didInit(widget: Pagelet.Widget) {
        super.didInit(widget)
        widget.options = mutableListOf()
        widget.options.addAll(applier.workflowObj.project.variableTypes.keys)
    }
}