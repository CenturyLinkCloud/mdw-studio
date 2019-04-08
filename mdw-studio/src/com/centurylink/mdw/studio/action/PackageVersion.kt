package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.file.Icons
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.JOptionPane

class PackageVersion : PackageAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val locator = Locator(event)
        locator.projectSetup?.let { projectSetup ->
            locator.selectedPackage?.let { pkg ->
                showDialog("${pkg.name} v${pkg.verString}")?.let {
                    when (it) {
                        INCREMENT_MAJOR -> projectSetup.setVersion(pkg, pkg.nextMajorVersion)
                        INCREMENT_MINOR -> projectSetup.setVersion(pkg, pkg.nextMinorVersion)
                        INCREMENT_SUB -> projectSetup.setVersion(pkg, pkg.nextSubVersion)
                    }
                }
            }
        }
    }

    private fun showDialog(curVer: String): Any? {
        return JOptionPane.showInputDialog(null, curVer, "MDW Asset Version", JOptionPane.PLAIN_MESSAGE,
                Icons.MDWDLG, arrayOf(INCREMENT_MAJOR, INCREMENT_MINOR, INCREMENT_SUB), INCREMENT_MAJOR)
    }

    companion object {
        const val INCREMENT_MAJOR = "Increment Major Version"
        const val INCREMENT_MINOR = "Increment Minor Version"
        const val INCREMENT_SUB = "Increment Sub Version"
    }
}