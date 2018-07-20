package com.centurylink.mdw.studio.edit

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.proj.ProjectSetup

class Configurator(private val tab: String, private val template: Template, val workflowObj: WorkflowObj,
        private val isReadonly: Boolean = false) {

    private val isMainTab: Boolean
        get() = tab == "Design" || tab == "General"

    init {
        filterWidgets()

        for (widget in template.pagelet.widgets) {
            if (isReadonly) {
                widget.isReadonly = true
            }

            // options source (assets are handled after setting value)
            when (widget.source) {
                "Variables" -> {
                    widget.options = workflowObj.process.variables.map { it.name }
                }
                "DocumentVariables" -> {
                    widget.options = workflowObj.process.variables.filter {
                        ProjectSetup.documentTypes.keys.contains(it.type)
                    }.map { it.name }
                }
                "UserGroup" -> {
                    widget.options = ProjectSetup.workgroups
                }
                // TODO parameterized
            }

            // init value
            val adapter = widget.createAdapter(template.category)
            widget.adapter = adapter
            adapter.init(widget, workflowObj)
            if (widget.value == null && widget.default != null) {
                widget.value = widget.default
                adapter.update() // reflect in workflowObj
            }
            adapter.didInit(widget)
        }
    }

    private fun filterWidgets() {
        template.pagelet.widgets.find {
            it.isHelpLink && (it.section == tab || (it.section == null && isMainTab))
        }?.let { template.pagelet.widgets.remove(it) }

        if (template.category != "object" && template.category != "attributes") {
            var widgets = mutableListOf<Pagelet.Widget>()
            for (widget in template.pagelet.widgets) {
                // TODO unsupported sections: Bindings (LdapAdapter.impl)
                if (!widget.isHidden) {
                    if (widget.section == tab || (widget.section == null && isMainTab)) {
                        widgets.add(widget)
                    }
                }
            }
            template.pagelet.widgets = widgets
        }
    }

}
