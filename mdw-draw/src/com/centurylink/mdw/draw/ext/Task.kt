package com.centurylink.mdw.draw.ext

import com.centurylink.mdw.draw.Data
import com.centurylink.mdw.model.asset.Asset
import com.centurylink.mdw.model.attribute.Attribute
import com.centurylink.mdw.model.task.TaskTemplate
import com.centurylink.mdw.model.task.TaskType
import org.json.JSONObject

fun TaskTemplate.update(obj: JSONObject) {
    taskName = if (obj.has("name")) {
        obj.getString("name")
    } else {
        val lastDot = name.lastIndexOf('.')
        if (lastDot > 0) name.substring(0, lastDot) else name
    }
    logicalId = if (obj.has("logicalId")) obj.getString("logicalId") else taskName
    if (obj.has("category")) {
        taskCategory = Data.categories.get(obj.getString("category"))
        if (taskCategory.isNullOrEmpty()){
            taskCategory = obj.getString("category")
        }
    }
    else {
        null
    }
    version = if (obj.has("version")) Asset.parseVersion(obj.getString("version")) else 0
    language = "TASK"
    taskTypeId = TaskType.TASK_TYPE_TEMPLATE

    var attrsJson: JSONObject? = null
    if (obj.has("attributes")) {
        attrsJson = obj.getJSONObject("attributes")
    }
    if (obj.has("description")) {
        attrsJson?.put("TaskDescription", obj.getString("description"))
    }
    attributes = Attribute.getAttributes(attrsJson)
    val vars = getAttribute("Variables")
    if (vars != null) {
        setVariablesFromString(vars, null )
    }
    val groups = getAttribute("Groups")
    if (groups != null) {
        setUserGroupsFromString(groups)
    }
    removeAttribute("TaskSLA_UNITS");
    removeAttribute("ALERT_INTERVAL_UNITS");
}