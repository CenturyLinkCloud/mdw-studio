package com.centurylink.mdw.studio.action

import com.centurylink.mdw.cli.Vercheck
import com.centurylink.mdw.studio.console.MdwConsole
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.vfs.VfsUtil


class VercheckAssets : AssetToolsAction() {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).getProjectSetup()?.let { projectSetup ->
            val errorCount = AssetVercheck(projectSetup).performCheck()
            if (errorCount > 0) {
                if (MessageDialogBuilder.yesNo("Asset Version Conflict(s)",
                        "Vercheck failed with $errorCount errors.  Fix?").isYes) {
                    AssetVercheck(projectSetup, true).performCheck()
                    VfsUtil.markDirtyAndRefresh(true, true, true, projectSetup.assetDir)
                }
            }
        }
    }
}

class AssetVercheck(private val projectSetup: ProjectSetup, private val isFix: Boolean = false) {

    fun performCheck(): Int {
        val vercheck = Vercheck()
        vercheck.isWarn = true
        vercheck.isFix = isFix

        MdwConsole.instance.run(vercheck)
        return vercheck.errorCount
    }

    companion object {
        val LOG = Logger.getInstance(AssetVercheck::class.java)
    }
}
