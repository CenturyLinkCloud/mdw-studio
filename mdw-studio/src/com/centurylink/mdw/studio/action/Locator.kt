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

    val project: Project?
        get() = event.getData(CommonDataKeys.PROJECT)

    val projectSetup: ProjectSetup?
        get() {
            return project?.getComponent(ProjectSetup::class.java)?.let { projectSetup ->
                if (projectSetup.isMdwProject) projectSetup else null
            }
        }

    val selectedPackage: AssetPackage?
        get() {
            return projectSetup?.let { projectSetup ->
                val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
                file?.let {
                    projectSetup.getPackage(it)
                }
            }
        }

    /**
     * Unlike selectedPackage, returns non-null if parent dir represents a package.
     */
    val `package`: AssetPackage?
        get() {
            return projectSetup?.let { projectSetup ->
                val view = event.getData(LangDataKeys.IDE_VIEW)
                view?.let {
                    if (it.directories.size == 1) {
                        projectSetup.getPackage(it.directories[0].virtualFile)
                    } else {
                        null
                    }
                }
            }
        }

    /**
     * A directory under project asset dir.
     */
    val potentialPackageDir: VirtualFile?
        get() {
            projectSetup?.let{ projectSetup ->
                event.getData(LangDataKeys.IDE_VIEW)?.let { view ->
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

    val asset: Asset?
        get() {
            return projectSetup?.let { projectSetup ->
                if (projectSetup.isMdwProject) {
                    val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
                    file?.let { projectSetup.getAsset(it) }
                } else {
                    null
                }
            }
        }
}