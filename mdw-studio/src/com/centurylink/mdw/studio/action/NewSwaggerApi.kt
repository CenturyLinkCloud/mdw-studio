package com.centurylink.mdw.studio.action

import com.intellij.openapi.actionSystem.AnActionEvent

class NewSwaggerApi : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val locator = Locator(event)
        val projectSetup = locator.getProjectSetup()
        if (projectSetup != null) {
            locator.getPackage()?.let { pkg ->
                println("NEW SWAGGER IN: " + pkg)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        val applicable = Locator(event).getPackage() != null
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }
}