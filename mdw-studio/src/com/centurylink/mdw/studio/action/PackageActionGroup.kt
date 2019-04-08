package com.centurylink.mdw.studio.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup

open class PackageActionGroup : DefaultActionGroup() {

    override fun update(event: AnActionEvent) {
        val applicable = Locator(event).selectedPackage?.let { true } ?: false
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }
}