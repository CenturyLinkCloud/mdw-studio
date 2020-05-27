package com.centurylink.mdw.draw.ext

import com.centurylink.mdw.model.Attributes
import com.centurylink.mdw.model.asset.AssetVersion
import com.centurylink.mdw.model.project.Project
import com.centurylink.mdw.model.task.TaskTemplate
import org.json.JSONObject

fun TaskTemplate.update(project: Project, obj: JSONObject) {
    taskName = if (obj.has("name")) {
        obj.getString("name")
    } else {
        val lastDot = name.lastIndexOf('.')
        if (lastDot > 0) name.substring(0, lastDot) else name
    }
    logicalId = if (obj.has("logicalId")) obj.getString("logicalId") else taskName
    if (obj.has("category")) {
        taskCategory = project.data.getTaskCategories().get(obj.getString("category"))
        if (taskCategory.isNullOrEmpty()){
            taskCategory = obj.getString("category")
        }
    }
    version = if (obj.has("version")) AssetVersion.parseVersion(obj.getString("version")) else 0

//    var attrsJson: JSONObject? = null
//    if (obj.has("attributes")) {
//        attrsJson = obj.getJSONObject("attributes")
//    }
//    if (obj.has("description")) {
//        attrsJson?.put("TaskDescription", obj.getString("description"))
//    }

    setVariablesFromAttribute("Variables", null)
    val groups = getAttribute("Groups")
    if (groups != null) {
        setUserGroupsFromString(groups)
    }
    attributes.remove("TaskSLA_UNITS");
    attributes.remove("ALERT_INTERVAL_UNITS");
}