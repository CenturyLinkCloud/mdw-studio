package com.centurylink.mdw.studio.action

import com.centurylink.mdw.model.task.TaskTemplate
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import org.json.JSONObject

class TaskInstances : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        getAsset(event)?.let { asset ->
            getProjectSetup(event)?.let { projectSetup ->
                val task = TaskTemplate(JSONObject(String(asset.file.contentsToByteArray())))
                val url = projectSetup.hubRootUrl +
                        "?templateId=${asset.id}&taskSpec=${task.taskName.replace(" ", "%20")}#/tasks"
                BrowserUtil.browse(url)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        val presentation = event.presentation
        if (presentation.isVisible && presentation.isEnabled) {
            var applicable = false
            getAsset(event)?.let {
                applicable = it.path.endsWith(".task")
            }
            presentation.isVisible = applicable
            presentation.isEnabled = applicable
        }
    }
}