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

    fun filterWidgets(tab: String): List<Pagelet.Widget> {
        val isMainTab = tab == "Design" || tab == "General"

        var widgets = pagelet.widgets.toMutableList()

        if (category == "object" || category == "attributes") {
            return widgets
        }
        else {
            // implementors
            var filteredWidgets = mutableListOf<Pagelet.Widget>()
            for (widget in widgets) {
                // TODO unsupported sections: Bindings (LdapAdapter.impl)
                if (!widget.isHidden) {
                    if ( !widget.isHelpLink && (widget.section == tab || (widget.section == null && isMainTab))) {
                        filteredWidgets.add(widget)
                    }
                }
            }
            return filteredWidgets
        }
    }
}