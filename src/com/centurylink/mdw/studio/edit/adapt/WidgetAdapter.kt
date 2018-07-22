package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import org.json.JSONArray

open class WidgetAdapter(applier: WidgetApplier) : com.centurylink.mdw.model.asset.WidgetAdapter,
        WidgetApplier by applier {

    /**
     * Default implementation performs no conversion on widget.value string.
     */
    override fun didInit(widget: Widget) {
    }

    /**
     * Convert widget.value to something whose toString() will populate the workflowObj.
     * Default implementation performs no conversion on widget.value string.
     */
    override fun willUpdate(widget: Widget) {
    }

    /**
     * handles compatibility for old process defs
     */
    fun toArray(value: String): JSONArray? {
        return if (value.startsWith('[')) {
            JSONArray(value.toString())
        }
        else {
            safeSplit(value, "#")
        }
    }

    /**
     * split a string based on a delimiter except when preceded by \
     */
    private fun safeSplit(value: String, delim: String): JSONArray {
        val res = JSONArray()
        val segs = value.split(delim)
        var accum = ""
        for (i in segs.indices) {
            val seg = segs[i]
            if (seg.endsWith('\\')) {
                accum += seg.substring(0, seg.length - 1) + delim
            }
            else {
                accum += seg
                res.put(accum)
                accum = ""
            }
        }
        return res
    }
}