package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import javax.swing.JOptionPane

class HubAction : ServerAction() {

    override fun actionPerformed(event: AnActionEvent) {
        event.getData(CommonDataKeys.PROJECT)?.getComponent(ProjectSetup::class.java)?.let {
            val hubUrl = it.hubRootUrl
            if (hubUrl == null) {
                JOptionPane.showMessageDialog(null, "No mdw.hub.url found",
                        "Open MDWHub", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)
            }
            else {
                BrowserUtil.browse(hubUrl)
            }
        }
    }

}