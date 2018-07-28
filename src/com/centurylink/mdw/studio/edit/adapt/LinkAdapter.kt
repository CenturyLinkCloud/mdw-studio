package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.edit.url
import com.centurylink.mdw.studio.edit.valueString
import com.centurylink.mdw.studio.proj.ProjectSetup

@Suppress("unused")
class LinkAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {
    override fun didInit(widget: Widget) {
        if (widget.name == "implementor") {
            widget.valueString?.let {
                if (it.startsWith("com.centurylink.mdw.workflow.")) {
                    // mdw built-in (GitHub)
                    var filePath = it.replace('.', '/')
                    widget.url = ProjectSetup.SOURCE_REPO_URL + "/blob/master/mdw-workflow/src/" + filePath + ".java"
                }
                else {
                    // hopefully a java asset
                    // TODO: open source from file system
                    var lastSlash = it.lastIndexOf('.')
                    var pkgName = it.substring(0, lastSlash)
                    var assetName = it.substring(lastSlash + 1)
                    widget.url = ProjectSetup.hubRoot + "/#/asset/" + pkgName + "/" + assetName + ".java"
                }
            }
        }
    }
}