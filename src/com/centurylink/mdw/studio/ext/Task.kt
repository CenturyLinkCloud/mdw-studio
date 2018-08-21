package com.centurylink.mdw.studio.ext

import com.centurylink.mdw.model.asset.Asset
import com.centurylink.mdw.model.attribute.Attribute
import com.centurylink.mdw.model.task.TaskTemplate
import com.centurylink.mdw.model.task.TaskType
import org.json.JSONObject

fun TaskTemplate.update(obj: JSONObject) {
    println("UPDATE: " + obj.toString(2))
    logicalId = obj.getString("logicalId")
    taskName = obj.getString("name")
    version = Asset.parseVersion(obj.getString("version"))
    language = "TASK"
    taskTypeId = TaskType.TASK_TYPE_TEMPLATE
    if (obj.has("category")) {
        taskCategory = obj.getString("category")
    }
    if (obj.has("description")) {
        comment = obj.getString("description")
    }
    if (obj.has("attributes")) {
        attributes = Attribute.getAttributes(obj.getJSONObject("attributes"))
    }
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