package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.AssetPackage
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
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
            if (projectSetup.isMdwProject) {
                val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
                file?.let {
                    return projectSetup.getAsset(file)
                }
            }
        }
        return null
    }

    /**
     * Returns a package based on directory chosen in project tree
     */
    protected fun getPackage(event: AnActionEvent): AssetPackage? {
        getProjectSetup(event)?.let { projectSetup ->
            val view = event.getData(LangDataKeys.IDE_VIEW)
            view?.let {
                if (it.directories.size == 1) {
                    return projectSetup.getPackage(it.directories[0].virtualFile)
                }
            }
        }
        return null
    }



    override fun update(event: AnActionEvent) {
        val presentation = event.presentation
        var applicable = false
        getAsset(event)?.let {
            applicable = true
        }
        presentation.isVisible = applicable
        presentation.isEnabled = applicable
    }
}