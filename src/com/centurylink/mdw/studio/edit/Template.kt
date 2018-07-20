package com.centurylink.mdw.studio.edit

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.ext.toJSONObject
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

}