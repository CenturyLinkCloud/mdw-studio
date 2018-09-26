package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.AssetPackage
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class Locator(private val event: AnActionEvent) {

    fun getProject(): Project? {
        return event.getData(CommonDataKeys.PROJECT)
    }

    fun getProjectSetup(): ProjectSetup? {
        val projectSetup = getProject()?.getComponent(ProjectSetup::class.java)
        return if (projectSetup != null) {
            return if (projectSetup.isMdwProject) projectSetup else null
        } else {
            null
        }
    }

    fun getPackage(): AssetPackage? {
        getProjectSetup()?.let { projectSetup ->
            val view = event.getData(LangDataKeys.IDE_VIEW)
            view?.let {
                if (it.directories.size == 1) {
                    return projectSetup.getPackage(it.directories[0].virtualFile)
                }
            }
        }
        return null
    }

    fun getPotentialPackageDir(): VirtualFile? {
        val projectSetup = getProjectSetup()
        if (projectSetup != null) {
            val view = event.getData(LangDataKeys.IDE_VIEW)
            if (view != null) {
                val directories = view.directories
                for (directory in directories) {
                    if (projectSetup.isAssetSubdir(directory.virtualFile)) {
                        return directory.virtualFile
                    }
                }
            }
        }
        return null
    }

    fun getAsset(): Asset? {
        getProjectSetup()?.let { projectSetup ->
            if (projectSetup.isMdwProject) {
                val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
                file?.let {
                    return projectSetup.getAsset(file)
                }
            }
        }
        return null
    }

}