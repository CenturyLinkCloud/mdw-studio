package com.centurylink.mdw.draw.edit.apply

import com.centurylink.mdw.draw.edit.url
import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.model.asset.Pagelet
import java.io.File

@Suppress("unused")
class ActivityImplApplier : ObjectApplier() {

    override fun init(widget: Pagelet.Widget, workflowObj: WorkflowObj) {
        super.init(widget, workflowObj)
        widget.valueString?.let {
            // check if impl is an asset
            val lastDot = it.lastIndexOf('.')
            val pkgName = it.substring(0, lastDot)
            val assetName = it.substring(lastDot + 1)
            val path = workflowObj.project.assetRoot.path + "/" + it.replace('.', '/')
            if (File(path + ".kt").exists()) {
                widget.value = "$pkgName/$assetName.kt"
            }
            else if (File(path + ".java").exists()) {
                widget.value = "$pkgName/$assetName.java"
            }
            else {
                widget.url = "class://$it"
            }
        }
    }

    override fun update() {
        val valString = widget.valueString ?: ""
        if (!valString.isBlank()) { // otherwise ignore update
            val lastSlash = valString.lastIndexOf('/')
            if (lastSlash > 0 && valString.length > lastSlash + 1) {
                // asset syntax
                val pkg = valString.substring(0, lastSlash)
                val assetName = valString.substring(lastSlash + 1)
                val className = pkg + '.' + assetName.substring(0, assetName.lastIndexOf('.'))
                workflowObj.set(widget.name, className)
            }
            else {
                workflowObj.set(widget.name, valString)
            }
        }
    }
}