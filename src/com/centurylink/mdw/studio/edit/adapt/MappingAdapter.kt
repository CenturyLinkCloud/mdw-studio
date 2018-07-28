package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.edit.valueString
import org.json.JSONObject

@Suppress("unused")
class MappingAdapter(val applier: WidgetApplier) : WidgetAdapter(applier) {

    /**
     * Convert to a JSONObject mapping.
     */
    override fun didInit(widget: Widget) {
        if (widget.valueString.isNullOrEmpty()) {
            widget.value = JSONObject()
        }
        widget.valueString?.let {
            widget.value = toMappings(it)
        }
    }

    override fun willUpdate(widget: Widget) {
        widget.value?.let {
            val jsonObject = it as JSONObject
            if (jsonObject.keySet().size == 0) {
                widget.value = null
            }
            else {
                widget.value = jsonObject.toString()
            }
        }
    }

    /**
     * With compatibility for old-style mapping attributes.
     */
    private fun toMappings(value: String): JSONObject {
        if (value.startsWith("{")) {
            return JSONObject(value)
        }
        else {
            val jsonObject = JSONObject()
            val mappings = safeSplit(value, ";")
            for (i in 0 until mappings.length()) {
                val mapping = mappings.getString(i)
                val eq = mapping.indexOf("=")
                jsonObject.put(mapping.substring(0, eq), mapping.substring(eq + 1))
            }
            return jsonObject
        }
    }
}