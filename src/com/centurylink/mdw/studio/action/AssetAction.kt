package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project

abstract class AssetAction : AnAction() {

    protected fun getProject(event: AnActionEvent): Project? {
        return event.getData(CommonDataKeys.PROJECT)
    }

    protected fun getProjectSetup(event: AnActionEvent): ProjectSetup? {
        return getProject(event)?.getComponent(ProjectSetup::class.java)
    }

    protected fun getAsset(event: AnActionEvent): Asset? {
        getProjectSetup(event)?.let { projectSetup ->
            val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
            file?.let {
                return projectSetup.getAsset(file)
            }
        }
        return null
    }
}