package com.centurylink.mdw.studio.action

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent

class ProcessInstances : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        getAsset(event)?.let { asset ->
            getProjectSetup(event)?.let { projectSetup ->
                val url = projectSetup.hubRootUrl + "?definitionId=${asset.id}&processSpec=${asset.name}#/workflow/processes"
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