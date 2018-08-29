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
        val project = event.getData(CommonDataKeys.PROJECT)
        val projectSetup = project?.getComponent(ProjectSetup::class.java)
        if (projectSetup != null) {
            val asset = getAsset(event)
            if (asset != null) {
                showDialog("${asset.name} v${asset.verString}")?.let {
                    when (it) {
                        INCREMENT_MAJOR_VERSION -> projectSetup.setVersion(asset, nextMajor(asset.version))
                        INCREMENT_MINOR_VERSION -> projectSetup.setVersion(asset, nextMinor(asset.version))
                    }
                }

            }
            else {
                val pkg = getPackage(event)
                pkg?.let {
                    showDialog("${pkg.name} v${pkg.verString}")?.let {
                        when (it) {
                            INCREMENT_MAJOR_VERSION -> projectSetup.setVersion(pkg, nextMajor(pkg.version))
                            INCREMENT_MINOR_VERSION -> projectSetup.setVersion(pkg, nextMinor(pkg.version))
                        }
                    }
                }
            }
        }
    }

    private fun showDialog(curVer: String): Any? {
        return JOptionPane.showInputDialog(null, "$curVer", "MDW Asset Version", PLAIN_MESSAGE,
                 null, arrayOf(INCREMENT_MAJOR_VERSION, INCREMENT_MINOR_VERSION), INCREMENT_MAJOR_VERSION)
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
        event.getData(CommonDataKeys.PROJECT)?.let {
            val projectSetup = it.getComponent(ProjectSetup::class.java)
            val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
            file?.let {
                return projectSetup.getAsset(file)
            }
        }
        return null
    }

    private fun getPackage(event: AnActionEvent): AssetPackage? {
        event.getData(CommonDataKeys.PROJECT)?.let {
            val projectSetup = it.getComponent(ProjectSetup::class.java)
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
        const val INCREMENT_MAJOR_VERSION = "Increment Major Version"
        const val INCREMENT_MINOR_VERSION = "Increment Minor Version"

        fun nextMajor(version: Int): Int {
            return (version / 1000 + 1) * 1000
        }

        fun nextMinor(version: Int): Int {
            return version + 1
        }
    }
}