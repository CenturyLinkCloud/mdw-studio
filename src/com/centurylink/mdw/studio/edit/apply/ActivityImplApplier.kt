package com.centurylink.mdw.studio.edit.apply

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.WorkflowObj
import com.centurylink.mdw.studio.edit.url
import com.centurylink.mdw.studio.edit.valueString
import com.centurylink.mdw.studio.proj.ProjectSetup

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
                var filePath = it.replace('.', '/')
                widget.url = ProjectSetup.SOURCE_REPO_URL + "/blob/master/mdw-workflow/src/" + filePath + ".java"
            }
            else {
                var lastDot = it.lastIndexOf('.')
                var pkgName = it.substring(0, lastDot)
                var assetName = it.substring(lastDot + 1)
                widget.value = "$pkgName/$assetName.java"
            }
        }
    }

    override fun update() {
        super.update()
    }
}