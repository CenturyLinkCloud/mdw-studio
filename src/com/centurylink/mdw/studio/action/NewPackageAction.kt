package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.actions.CreateDirectoryOrPackageAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys

class NewPackageAction : CreateDirectoryOrPackageAction() {

    override fun actionPerformed(e: AnActionEvent) {
        super.actionPerformed(e)

        println("New Package")
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        val presentation = event.getPresentation()
        if (presentation.isVisible && presentation.isEnabled) {
            var applicable = false
            val project = event.getData(CommonDataKeys.PROJECT)
            project?.let {
                val projectSetup = project.getComponent(ProjectSetup::class.java)
                val view = event.getData(LangDataKeys.IDE_VIEW)
                view?.let {
                    val directories = view.directories
                    for (directory in directories) {
                        if (projectSetup.isAssetDir(directory.virtualFile)) {
                            applicable = true
                            break
                        }
                    }
                }
            }
            presentation.isVisible = applicable
            presentation.isEnabled = applicable
        }
    }
}