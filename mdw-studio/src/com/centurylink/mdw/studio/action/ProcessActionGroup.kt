package com.centurylink.mdw.studio.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup

open class ProcessActionGroup : DefaultActionGroup() {

    override fun update(event: AnActionEvent) {
        val applicable = Locator(event).asset?.let { it.ext == "proc" } ?: false
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }
}