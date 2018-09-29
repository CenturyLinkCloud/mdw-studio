package com.centurylink.mdw.draw.edit.adapt

import com.centurylink.mdw.draw.edit.apply.WidgetApplier
import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.draw.edit.version
import com.centurylink.mdw.model.asset.Asset
import com.centurylink.mdw.model.asset.AssetVersionSpec
import com.centurylink.mdw.model.asset.Pagelet

@Suppress("unused")
class AssetAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {
    override fun didInit(widget: Pagelet.Widget) {
        widget.valueString?.let {
            if (widget.name == "processname" && !it.endsWith(".proc")) {
                widget.value = it + ".proc"
            }
        }
    }

    override fun willUpdate(widget: Pagelet.Widget) {
        val value = widget.valueString
        val version = if (value.isNullOrBlank()) {
            null
        } else {
            val verSpec = AssetVersionSpec.parse(value)
            if (verSpec.version != null && verSpec.version != "0") {
                // manually specified
                widget.value = verSpec.qualifiedName
                verSpec.version
            }
            else {
                widget.version?.let {
                    AssetVersionSpec.getDefaultSmartVersionSpec(Asset.formatVersion(it))
                }
            }
        }
        if (widget.name == "processname") {
            workflowObj.setAttribute("processversion", version)
        }
        else {
            workflowObj.setAttribute("${widget.name}_assetVersion", version)
        }
    }

    override fun update() {
        super.update()
    }
}