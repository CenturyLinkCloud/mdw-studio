package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

abstract class ServerAction : AnAction() {

    override fun update(event: AnActionEvent) {
        val project = event.getData(CommonDataKeys.PROJECT)
        project?.let {
            val projectSetup = project.getComponent(ProjectSetup::class.java)
            if (projectSetup.isMdwProject) {
                event.presentation.isVisible = true
                event.presentation.isEnabled = projectSetup.isServerRunning
                return
            }
        }
        event.presentation.isVisible = false
        event.presentation.isEnabled = false
    }
}