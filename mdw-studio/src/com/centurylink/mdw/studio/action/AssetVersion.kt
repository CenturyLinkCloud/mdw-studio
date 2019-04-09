package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.file.Icons
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.JOptionPane
import javax.swing.JOptionPane.PLAIN_MESSAGE

class AssetVersion() : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val locator = Locator(event)
        locator.projectSetup?.let { projectSetup ->
            locator.asset?.let { asset ->
                showDialog("${asset.name} v${asset.verString}")?.let {
                    when (it) {
                        INCREMENT_MAJOR_VERSION -> projectSetup.setVersion(asset, asset.nextMajorVersion)
                        INCREMENT_MINOR_VERSION -> projectSetup.setVersion(asset, asset.nextMinorVersion)
                    }
                }
            }
        }
    }

    private fun showDialog(curVer: String): Any? {
        return JOptionPane.showInputDialog(null, curVer, "MDW Asset Version", PLAIN_MESSAGE,
                 Icons.MDWDLG, arrayOf(INCREMENT_MAJOR_VERSION, INCREMENT_MINOR_VERSION), INCREMENT_MAJOR_VERSION)
    }

    companion object {
        const val INCREMENT_MAJOR_VERSION = "Increment Major Version"
        const val INCREMENT_MINOR_VERSION = "Increment Minor Version"
    }
}