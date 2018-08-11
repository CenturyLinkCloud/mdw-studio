package com.centurylink.mdw.studio.ext

import com.centurylink.mdw.model.task.TaskTemplate
import org.json.JSONObject

fun TaskTemplate.update(obj: JSONObject) {
    // TODO save task info back to TaskTemplate
    println("UPDATE: " + obj.toString(2))
}