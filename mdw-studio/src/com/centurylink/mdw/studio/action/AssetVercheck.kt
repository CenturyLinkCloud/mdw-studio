package com.centurylink.mdw.studio.action

import com.centurylink.mdw.cli.Vercheck
import com.centurylink.mdw.studio.console.MdwConsole
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.wm.ToolWindowManager
import java.nio.charset.Charset
import com.intellij.execution.process.DefaultJavaProcessHandler




class VercheckAssets : AssetToolsAction() {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).getProjectSetup()?.let { projectSetup ->
            AssetVercheck(projectSetup).performCheck()
        }
    }
}

class AssetVercheck(private val projectSetup: ProjectSetup) {

    fun performCheck(): Boolean {
        val vercheck = Vercheck()
        vercheck.isWarn = true
        // vercheck.isFix = true

//        projectSetup.git?.let { git ->
//            vercheck.branch = git.branch
//            ProgressManager.getInstance().runProcessWithProgressSynchronously({
//                try {
//                    ProgressManager.getInstance().progressIndicator?.isIndeterminate = true
//                    vercheck.run()
//                } catch (ex: Exception) {
//                    LOG.warn(ex)
//                    Notifications.Bus.notify(Notification("MDW", "Vercheck Error", ex.toString(),
//                            NotificationType.ERROR), projectSetup.project)
//                }
//            }, "Asset Vercheck", false, projectSetup.project)
//
//            VfsUtil.markDirtyAndRefresh(true, true, true, projectSetup.assetDir)
//        }


        MdwConsole.instance.run(vercheck)

        return true
    }

    companion object {
        val LOG = Logger.getInstance(AssetVercheck::class.java)
    }
}