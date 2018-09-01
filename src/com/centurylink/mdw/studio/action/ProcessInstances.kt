package com.centurylink.mdw.studio.action

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.JOptionPane

class ProcessInstances : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        getAsset(event)?.let { asset ->
            getProjectSetup(event)?.let { projectSetup ->
                var hubUrl = projectSetup.hubRootUrl
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
        val presentation = event.presentation
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