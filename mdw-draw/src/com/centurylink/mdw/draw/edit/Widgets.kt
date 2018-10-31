package com.centurylink.mdw.draw.edit

import com.centurylink.mdw.draw.edit.adapt.WidgetAdapter
import com.centurylink.mdw.draw.edit.apply.*
import com.centurylink.mdw.draw.model.Data
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.model.workflow.Process
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

var Pagelet.Widget.label: String
    get() = attributes["label"] ?: name
    set(value) { attributes["label"] = value.toString() }
val Pagelet.Widget.section: String?
    get() = attributes["section"]
val Pagelet.Widget.isHidden: Boolean
    get() = "true" == attributes["hidden"]
var Pagelet.Widget.isReadonly: Boolean
    get() = "true" == attributes["readonly"]
    set(value) { attributes["readonly"] = value.toString() }
val Pagelet.Widget.source: String?
    get() = attributes["source"]
val Pagelet.Widget.default: String?
    get() = attributes["default"]
var Pagelet.Widget.url: String?
    get() = attributes["url"]
    set(value) { attributes["url"] = value }
var Pagelet.Widget.units: String?
    get() = attributes[name + "_UNITS"]
    set(value) { attributes[name + "_UNITS"] = value }
var Pagelet.Widget.version: Int?
    get() = attributes["version"]?.toInt()
    set(value) { attributes["version"] = value?.toString() }
val Pagelet.Widget.isMultiline: Boolean
    get() = "true" == attributes["multiline"]
val Pagelet.Widget.valueString: String?
    get() {
        value.let {
            return when (it) {
                null -> null
                is JsonPrimitive -> it.asString
                is JsonObject -> GsonBuilder().disableHtmlEscaping().create().toJson(it)
                else -> it.toString()
            }
        }
    }
val Pagelet.Widget.isTableType: Boolean
    get() = type == "table" || type == "mapping"
val Pagelet.Widget.isEditor: Boolean
    get() = type == "editor"
val Pagelet.Widget.width: Int
    get() = attributes["vw"]?.toInt() ?: 500
val Pagelet.Widget.rows: Int?
    get() = attributes["rows"]?.toInt()
val Pagelet.Widget.min: Int?
    get() = attributes["min"]?.toInt()
val Pagelet.Widget.max: Int?
    get() = attributes["max"]?.toInt()
val Pagelet.Widget.isHelpLink: Boolean
    get() = type == "link" && (url?.startsWith("help/") ?: false)

fun Pagelet.Widget.init(category: String, workflowObj: WorkflowObj): WidgetAdapter {

    if (attributes == null) {
        // needed for at least templ placeholders
        attributes = mutableMapOf<String,String>()
    }
    this.isReadonly = workflowObj.isReadonly || this.isReadonly

    // options source
    when (source) {
        "Variables" -> {
            options = (workflowObj.asset as Process).variables.map { it.name }
        }
        "DocumentVariables" -> {
            val docTypes = Data.getDocumentTypes(workflowObj.project).keys
            options = (workflowObj.asset as Process).variables.filter {
                docTypes.contains(it.type)
            }.map { it.name }
        }
        "UserGroup" -> {
            options = Data.getWorkgroups(workflowObj.project)
        }
        "TaskCategory" -> {
            options = Data.getTaskCategories(workflowObj.project).keys.toMutableList()
        }
    }

    val adapter = createAdapter(category)
    this.adapter = adapter
    adapter.init(this, workflowObj)
    if (value == null && default != null) {
        value = default
        adapter.update() // reflect in workflowObj
    }
    adapter.didInit(this)
    return adapter
}

fun Pagelet.Widget.createApplier(category: String): AbstractWidgetApplier {
    var applierClass = attributes["applier"]
    applierClass?.let {
        if (!it.contains('.')) {
            // qualify with default package
            applierClass = Pagelet.Widget::createApplier::class.java.`package`.name + ".apply." + it
        }
        return Class.forName(applierClass).newInstance() as AbstractWidgetApplier
    }

    // fall back to template default
    when (category) {
        "table" -> return TableApplier()
        "object" -> return ObjectApplier()
        else -> return AttributeApplier()
    }
}

/**
 * Exclusively by convention based on widget type
 */
fun Pagelet.Widget.createAdapter(category: String): WidgetAdapter {
    var adapterClass = attributes["adapter"]
    if (adapterClass == null) {
        adapterClass = Pagelet.Widget::createAdapter::class.java.`package`.name + ".adapt." +
                type.substring(0, 1).toUpperCase() + type.substring(1) + "Adapter"
    }
    else {
        if (!adapterClass.contains('.')) {
            // qualify with default package
            adapterClass = Pagelet.Widget::createAdapter::class.java.`package`.name + ".adapt." + adapterClass
        }
    }
    val applier = createApplier(category)
    return try {
        Class.forName(adapterClass).getConstructor(WidgetApplier::class.java).newInstance(applier) as WidgetAdapter
    }
    catch(ex: ClassNotFoundException) {
        // error if expressly specified but not found
        attributes["adapter"]?.let { throw ex }
        // otherwise use default
        WidgetAdapter(applier)
    }
}