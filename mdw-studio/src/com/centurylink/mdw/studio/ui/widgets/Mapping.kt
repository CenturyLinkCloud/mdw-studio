package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.init
import com.centurylink.mdw.draw.edit.isReadonly
import com.centurylink.mdw.draw.edit.source
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.model.variable.Variable
import com.centurylink.mdw.model.workflow.Process
import org.json.JSONObject
import java.io.IOException

/**
 * Map will have been converted to a table by its adapter.
 */
@Suppress("unused")
class Mapping(widget: Widget) : Table(widget, false, false) {

    override val listenTo = if (widget.source == "Subprocess" ) "processname" else super.listenTo

    override fun initialColumnWidgets(): List<Widget> {
        val colWidgs = mutableListOf<Widget>()

        val label = if (widget.source == null) "Input Variable" else "${widget.source} Variable"
        val varWidget = Widget(label, "text")
        varWidget.init("table", workflowObj)
        varWidget.isReadonly = true
        colWidgs.add(varWidget)

        val typeWidget = Widget("Type", "text")
        typeWidget.init("table", workflowObj)
        typeWidget.isReadonly = true
        colWidgs.add(typeWidget)

        val modeWidget = Widget("Mode", "text")
        modeWidget.init("table", workflowObj)
        modeWidget.isReadonly = true
        colWidgs.add(modeWidget)

        val exprWidget = Widget("Binding Expression", "text")
        exprWidget.init("table", workflowObj)
        colWidgs.add(exprWidget)

        return colWidgs
    }

    private val bindingVariables: List<Variable> by lazy {
        if (widget.source == "Subprocess") {
            val procName = workflowObj.getAttribute("processname")
            if (procName == null) {
                // not set yet
                listOf()
            }
            else {
                var file = projectSetup.getAssetFile(procName)
                if (file == null) {
                    file = projectSetup.getAssetFile(procName + ".proc")
                }
                file ?: throw IOException("Missing subprocess asset: " + procName)
                val process = Process.fromString(String(file.contentsToByteArray()))
                getBindingVars(process, true)
            }
        }
        else {
            getBindingVars(workflowObj.asset as Process, false)
        }
    }

    override fun initialRows(): MutableList<Array<String>> {
        val rows = mutableListOf<Array<String>>()
        // initialize rows from widget value
        widget.value?.let {
            val mappingJson = it as JSONObject
            // clear out invalid mappings (left over from old variables)
            val toRemove = mutableListOf<String>()
            for (key in mappingJson.keySet()) {
                if (bindingVariables.find { it.name == key } == null) {
                    toRemove.add(key)
                }
            }
            if (!toRemove.isEmpty()) {
                toRemove.forEach { mappingJson.remove(it) }
                applyUpdate()
            }
            for (bindingVariable in bindingVariables) {
                rows.add(arrayOf(bindingVariable.name, bindingVariable.type, bindingVariable.category,
                        mappingJson.optString(bindingVariable.name)))
            }
        }
        return rows
    }

    private fun getBindingVars(process: Process, includeOuts: Boolean): List<Variable> {
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

    override fun widgetValueFromRows(rows: List<Array<String>>): Any {
        val updatedMapping = JSONObject()
        for (row in rows) {
            val bindingExpr = row[3]
            if (!bindingExpr.isBlank()) {
                updatedMapping.put(row[0], bindingExpr)
            }
        }
        return updatedMapping
    }
}