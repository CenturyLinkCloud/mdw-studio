package com.centurylink.mdw.studio.action

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.JOptionPane

class ProcessInstances : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val locator = Locator(event)
        locator.asset?.let { asset ->
            locator.projectSetup?.let { projectSetup ->
                val hubUrl = projectSetup.hubRootUrl
                if (hubUrl == null) {
                    JOptionPane.showMessageDialog(null, "No mdw.hub.url found",
                            "Process Instances", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)
                }
                else {
                    BrowserUtil.browse(hubUrl +
                            "?definitionId=${asset.id}&processSpec=${asset.encodedName}#/workflow/processes")
                }
            }
        }
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        val locator = Locator(event)
        if (event.presentation.isVisible) {
            locator.asset?.let { asset ->
                event.presentation.isVisible = asset.ext == "proc"
                event.presentation.isEnabled = locator.projectSetup?.isServerRunning ?: false
            }
        }
    }
}