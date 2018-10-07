package com.centurylink.mdw.studio.action

import com.centurylink.mdw.model.workflow.ActivityImplementor
import com.centurylink.mdw.studio.proj.Implementors
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project

class ActivityUsages : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val implementor = getImplementor(event)
        // TODO: usages logic like LoaderPersisterVcs.getProcessListForImplementor()
    }

    override fun update(event: AnActionEvent) {
        val applicable = getImplementor(event) != null && getProject(event) != null
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }

    private fun getImplementor(event: AnActionEvent): ActivityImplementor? {
        return Implementors.IMPLEMENTOR_DATA_KEY.getData(event.dataContext)
    }

    private fun getProject(event: AnActionEvent): Project? {
        return event.getData(CommonDataKeys.PROJECT)
    }
}