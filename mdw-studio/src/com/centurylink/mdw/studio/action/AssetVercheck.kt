package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class VercheckAssets : AssetToolsAction() {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).getProjectSetup()?.let { projectSetup ->
            AssetVercheck(projectSetup).performCheck()
        }
    }
}

class AssetVercheck(private val projectSetup: ProjectSetup) {
    fun performCheck() {
        println("vercheck")
        // TODO perform update
    }
}