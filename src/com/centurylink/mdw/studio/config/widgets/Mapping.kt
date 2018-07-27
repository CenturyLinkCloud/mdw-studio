package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.model.variable.Variable
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.edit.source
import com.centurylink.mdw.studio.proj.ProjectSetup
import org.json.JSONObject
import java.io.IOException
import java.lang.IllegalArgumentException

/**
 * Map will have been converted to a table by its adapter.
 */
@Suppress("unused")
class Mapping(widget: Pagelet.Widget) : Table(widget, false) {


    init {
        val workflowObj = (widget.adapter as WidgetApplier).workflowObj
        val projectSetup = workflowObj.project as ProjectSetup

        val bindings = {
            if (widget.source == "Subprocess") {
                val procName = workflowObj.getAttribute("processname")
                procName ?: throw IllegalArgumentException("Missing processname attribute")
                val file = projectSetup.getAssetFile(procName)
                file ?: throw IOException("Missing subprocess asset: " + procName)
                val process = Process(JSONObject(String(file.contentsToByteArray())))
                getBindingVariables(process, true)
            } else {
                getBindingVariables(workflowObj.process, false)
            }
        }
    }

    private fun getBindingVariables(process: Process, includeOuts: Boolean): List<Variable> {
        val bindingVars = mutableListOf<Variable>()
        process.variables?.let {
            for (variable in it) {
                if (variable.category == "INPUT" || variable.category == "INOUT" ||
                        (includeOuts && variable.category == "OUTPUT")) {
                    bindingVars.add(variable)
                }
            }
        }

        bindingVars.sortBy { it.name }
        return bindingVars
    }
}