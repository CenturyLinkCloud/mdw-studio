package com.centurylink.mdw.studio.edit

import com.centurylink.mdw.model.workflow.Activity
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.model.workflow.TextNote
import com.centurylink.mdw.model.workflow.Transition
import com.centurylink.mdw.studio.draw.*
import com.centurylink.mdw.studio.ext.Json
import org.json.JSONObject

enum class WorkflowType {
    process,
    activity,
    transition,
    subprocess,
    textNote,
    implementor
}

open class WorkflowObj(val project: Any?, val process: Process, val type: WorkflowType, val obj: JSONObject) {

    open var id: String
        get() = if (obj.has("id")) obj.getString("id") else "-1"
        set(value) { obj.put("id", value)}
    open var name: String
        get() = if (obj.has("name")) obj.getString("name") else "New " + type
        set(value) { obj.put("name", value)}

    val titlePath: String
        get() = process.packageName + "/" + process.name + ": " + name.lines().joinToString(" ")

    var isReadonly = false

    init {
        if (obj.has("id")) {
            this.id = obj.getString("id")
        }
        if (obj.has("name")) {
            this.name = obj.getString("name")
        }
    }

    /**
     * returns a json obj value
     */
    fun get(name: String): Any? {
        return if (obj.has(name)) obj.get(name) else null
    }

    /**
     * updates a json obj value
     */
    fun set(name: String, value: Any?) {
        if (value == null || value.toString().isEmpty()) {
            remove(name)
        }
        else {
            obj.put(name, value)
        }
    }

    /**
     * removes a json obj value
     */
    fun remove(name: String) {
        obj.remove(name)
    }

    fun getAttributes(): JSONObject? {
        if (obj.has("attributes")) {
            return obj.getJSONObject("attributes")
        }
        else {
            return null
        }
    }

    /**
     * returns a json obj attribute value
     */
    fun getAttribute(name: String): String? {
        if (obj.has("attributes")) {
            val attrsJson = obj.getJSONObject("attributes")
            if (attrsJson.has(name))  {
                return attrsJson.getString(name)
            }
        }
        return null
    }

    /**
     * updates a json obj attribute value
     */
    fun setAttribute(name: String, value: String?) {
        var attrs = if (obj.has("attributes")) obj.getJSONObject("attributes") else null
        if (value == null) {
            attrs?.remove(name)
        }
        else {
            attrs = attrs ?: JSONObject()
            attrs.put(name, value)
            obj.put("attributes", attrs)
        }
    }

    /**
     * Updates the process to reflect changes to obj attrs
     */
    fun updateProcess() {
        when (type) {
            WorkflowType.process -> {
                val proc = Process(obj)
                process.set(proc)
            }
            WorkflowType.activity -> {
                if (!obj.has("name")) {
                    obj.put("name", "")
                }
                val activity = Activity(obj)
                process.setActivity(id, activity)
            }
            WorkflowType.transition -> {
                val transition = Transition(obj)
                process.setTransition(id, transition)
            }
            WorkflowType.subprocess -> {
                val subprocess = Process(obj)
                process.setSubprocess(id, subprocess)
            }
            WorkflowType.textNote -> {
                val textNote = TextNote(obj)
                process.setTextNote(id, textNote)
            }
        }
    }

    override fun toString(): String {
        return toString(false)
    }

    fun toString(pretty: Boolean): String {
        return type.toString() + ": " + obj.toString(if (pretty) 2 else 0)
    }

    fun toString(property: String): String {
        return Json.toString(obj, property)
    }
}