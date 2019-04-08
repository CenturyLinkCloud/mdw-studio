package com.centurylink.mdw.studio.action

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent

class RunAutotest : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val locator = Locator(event)
        locator.asset?.let { asset ->
            locator.projectSetup?.let { projectSetup ->
                val url = projectSetup.hubRootUrl + "#/tests/" + asset.pkg.name + "/" + asset.encodedName
                BrowserUtil.browse(url)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        val locator = Locator(event)
        if (event.presentation.isVisible) {
            locator.asset?.let { asset ->
                event.presentation.isVisible = asset.ext == "test"
                event.presentation.isEnabled = locator.projectSetup?.isServerRunning ?: false
            }
        }
    }
}