package com.centurylink.mdw.draw.edit.apply

import com.centurylink.mdw.draw.model.Data
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.draw.edit.url
import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.model.asset.Pagelet
import java.io.File

/**
 * TODO: support kotlin implementors
 */
@Suppress("unused")
class ActivityImplApplier : ObjectApplier() {

    override fun init(widget: Pagelet.Widget, workflowObj: WorkflowObj) {
        super.init(widget, workflowObj)
        widget.valueString?.let {
            if (it.startsWith("com.centurylink.mdw.workflow.")) {
                // mdw built-in (GitHub)
                val filePath = it.replace('.', '/')
                widget.url = Data.SOURCE_REPO_URL + "/blob/master/mdw-workflow/src/" + filePath + ".java"
            }
            else {
                val lastDot = it.lastIndexOf('.')
                val pkgName = it.substring(0, lastDot)
                val assetName = it.substring(lastDot + 1)
                var ext = ".java"
                if (File(workflowObj.project.assetRoot.path + "/" + it.replace('.', '/') + ".kt").exists()) {
                    ext = ".kt"
                }

                widget.value = "$pkgName/$assetName$ext"
            }
        }
    }

    override fun update() {
        super.update()
    }
}