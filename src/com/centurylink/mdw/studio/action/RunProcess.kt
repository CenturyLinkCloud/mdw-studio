package com.centurylink.mdw.studio.action

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent

class RunProcess : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        getAsset(event)?.let { asset ->
            getProjectSetup(event)?.let { projectSetup ->
                val url = projectSetup.hubRootUrl + "#/workflow/run/" + asset.pkg.name + "/" + asset.encodedName
                BrowserUtil.browse(url)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        val presentation = event.getPresentation()
        if (presentation.isVisible && presentation.isEnabled) {
            var applicable = false
            getAsset(event)?.let {
                applicable = it.path.endsWith(".proc")
            }
            presentation.isVisible = applicable
            presentation.isEnabled = applicable
        }
    }
}