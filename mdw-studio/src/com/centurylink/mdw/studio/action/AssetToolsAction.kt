package com.centurylink.mdw.studio.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

abstract class AssetToolsAction : AnAction() {

    override fun update(event: AnActionEvent) {
        var applicable = false
        Locator(event).getProjectSetup()?.let { projectSetup ->
            applicable = if (event.place == "MainMenu") {
                true
            } else {
                val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
                file == projectSetup.project.baseDir || file == projectSetup.assetDir
            }
        }
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }
}