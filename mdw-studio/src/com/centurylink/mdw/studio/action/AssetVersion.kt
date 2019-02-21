package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.file.Icons
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import javax.swing.JOptionPane
import javax.swing.JOptionPane.PLAIN_MESSAGE

class AssetVersion() : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(CommonDataKeys.PROJECT)
        val projectSetup = project?.getComponent(ProjectSetup::class.java)
        if (projectSetup != null) {
            val asset = Locator(event).getAsset()
            if (asset != null) {
                showDialog("${asset.name} v${asset.verString}")?.let {
                    when (it) {
                        INCREMENT_MAJOR_VERSION -> projectSetup.setVersion(asset, nextMajor(asset.version))
                        INCREMENT_MINOR_VERSION -> projectSetup.setVersion(asset, nextMinor(asset.version))
                    }
                }

            }
            else {
                val pkg = Locator(event).getPackage()
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
        return JOptionPane.showInputDialog(null, curVer, "MDW Asset Version", PLAIN_MESSAGE,
                 Icons.MDWDLG, arrayOf(INCREMENT_MAJOR_VERSION, INCREMENT_MINOR_VERSION), INCREMENT_MAJOR_VERSION)
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        val presentation = event.getPresentation()
        if (presentation.isVisible && presentation.isEnabled) {
            val locator = Locator(event)
            val applicable = locator.getPackage() != null || locator.getAsset() != null
            presentation.isVisible = applicable
            presentation.isEnabled = applicable
        }
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