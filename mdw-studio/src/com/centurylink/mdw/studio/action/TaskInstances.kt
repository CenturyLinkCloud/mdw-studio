package com.centurylink.mdw.studio.action

import com.centurylink.mdw.model.task.TaskTemplate
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import org.json.JSONObject
import javax.swing.JOptionPane

class TaskInstances : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val locator = Locator(event)
        locator.asset?.let { asset ->
            locator.projectSetup?.let { projectSetup ->
                var hubUrl = projectSetup.hubRootUrl
                if (hubUrl == null) {
                    JOptionPane.showMessageDialog(null, "No mdw.hub.url found",
                            "Task Instances", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)
                }
                else {
                    val task = TaskTemplate(JSONObject(String(asset.file.contentsToByteArray())))
                    BrowserUtil.browse(hubUrl +
                            "?templateId=${asset.id}&taskSpec=${task.taskName.replace(" ", "%20")}#/tasks")
                }
            }
        }
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        val locator = Locator(event)
        if (event.presentation.isVisible) {
            locator.asset?.let { asset ->
                event.presentation.isVisible = asset.ext == "task"
                event.presentation.isEnabled = locator.projectSetup?.isServerRunning ?: false
            }
        }
    }
}