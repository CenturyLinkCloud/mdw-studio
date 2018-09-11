package com.centurylink.mdw.draw.edit

import com.centurylink.mdw.draw.ext.toJSONObject
import com.centurylink.mdw.model.asset.Pagelet
import com.google.gson.JsonObject

class Template(json: JsonObject) {

    val category: String
    val pagelet: Pagelet

    init {
        category = json.get("category").asString

        val pageletContent = json.get("pagelet")
        if (pageletContent.isJsonPrimitive) {
            pagelet = Pagelet(pageletContent.asString)
        }
        else {
            pagelet = Pagelet(pageletContent.asJsonObject.toJSONObject())
        }
    }

    fun filterWidgets(tab: String): List<Pagelet.Widget> {
        return pagelet.widgets.filter {
            if (it.attributes == null) {
                it.attributes = mutableMapOf<String,String>() // avoid NPE in accessors
            }
            when(category) {
                "object" -> {
                    !it.isHidden && !it.isHelpLink
                }
                "attributes" -> {
                    !it.isHidden && !it.isHelpLink
                }
                "task" -> {
                    !it.isHidden && !it.isHelpLink && (it.section == tab || (it.section == null && tab == "General"))
                }
                else -> {
                    !it.isHidden && !it.isHelpLink && (it.section == tab || (it.section == null && tab == "Design"))
                }
            }
        }
    }
}