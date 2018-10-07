package com.centurylink.mdw.studio.action

import com.centurylink.mdw.model.workflow.ActivityImplementor
import com.centurylink.mdw.studio.proj.Implementors
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ActivityUsages : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val implementor = getImplementor(event)
        println ("IMPL: " + implementor?.implementorClass)
    }

    override fun update(event: AnActionEvent) {
        val applicable = getImplementor(event) != null
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }

    private fun getImplementor(event: AnActionEvent): ActivityImplementor? {
        return Implementors.IMPLEMENTOR_DATA_KEY.getData(event.dataContext)
    }
}