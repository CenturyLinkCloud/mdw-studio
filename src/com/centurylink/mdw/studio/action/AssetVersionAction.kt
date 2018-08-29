package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.AssetPackage
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import javax.swing.JOptionPane
import javax.swing.JOptionPane.PLAIN_MESSAGE

class AssetVersionAction() : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val res = JOptionPane.showInputDialog(null, "Increment:", "MDW Asset Version",
                PLAIN_MESSAGE, null, arrayOf(MAJOR_VERSION, MINOR_VERSION), MAJOR_VERSION)

        res?.let {
            val asset = getAsset(event)
            if (asset != null) {
                println("ASSET: " + asset)

            }
            else {
                val pkg = getPackage(event)
                pkg?.let {
                    println("PKG: " + pkg)
                }
            }
        }
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        val presentation = event.getPresentation()
        if (presentation.isVisible && presentation.isEnabled) {
            var applicable = getPackage(event) != null
            presentation.isVisible = applicable
            presentation.isEnabled = applicable
        }
    }

    private fun getAsset(event: AnActionEvent): Asset? {
        val project = event.getData(CommonDataKeys.PROJECT)
        project?.let {
            val projectSetup = project.getComponent(ProjectSetup::class.java)
            val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
            file?.let {
                return projectSetup.getAsset(file)
            }
        }
        return null
    }

    private fun getPackage(event: AnActionEvent): AssetPackage? {
        val project = event.getData(CommonDataKeys.PROJECT)
        project?.let {
            val projectSetup = project.getComponent(ProjectSetup::class.java)
            val view = event.getData(LangDataKeys.IDE_VIEW)
            view?.let {
                val directories = view.directories
                if (directories.size == 1) {
                    return projectSetup.getPackage(directories[0].virtualFile)
                }
            }
        }
        return null
    }

    companion object {
        const val MAJOR_VERSION = "Major Version"
        const val MINOR_VERSION = "Minor Version"
    }
}