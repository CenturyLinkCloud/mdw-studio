package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.proc.ActivityIncrementProvider
import com.centurylink.mdw.studio.proc.CanvasActions
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project

class ActivityIncrement  : AnAction("Increment Activity ID") {

    override fun actionPerformed(event: AnActionEvent) {
        getProject(event)?.let { project ->
            project.getComponent(ProjectSetup::class.java)?.let { projectSetup ->
                getProvider(event)?.let { incrementProvider ->
                    if (incrementProvider.canIncrement()) {
                        incrementProvider.doIncrement()
                    }
                }
            }
        }
    }

    override fun update(event: AnActionEvent) {
        var applicable = false
        getProject(event)?.let { project ->
            project.getComponent(ProjectSetup::class.java)?.let { projectSetup ->
                getProvider(event)?.let { incrementProvider ->
                    applicable = incrementProvider.canIncrement()
                }
            }
        }
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }

    private fun getProvider(event: AnActionEvent): ActivityIncrementProvider? {
        return CanvasActions.ACTIVITY_INCREMENT_PROVIDER.getData(event.dataContext)
    }

    private fun getProject(event: AnActionEvent): Project? {
        return event.getData(CommonDataKeys.PROJECT)
    }
}