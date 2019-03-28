package com.centurylink.mdw.studio.action

import com.centurylink.mdw.cli.Vercheck
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vfs.VfsUtil

class VercheckAssets : AssetToolsAction() {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).getProjectSetup()?.let { projectSetup ->
            AssetVercheck(projectSetup).performCheck()
        }
    }
}

class AssetVercheck(private val projectSetup: ProjectSetup) {

    fun performCheck(): CheckinHandler.ReturnResult {
        val vercheck = Vercheck()
        vercheck.configLoc = projectSetup.configLoc
        vercheck.assetLoc = projectSetup.assetRoot.path
        vercheck.isWarn = true
        vercheck.isFix = true
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

        println("VERCHECK VERCHECK VERCHECK")

        return CheckinHandler.ReturnResult.CANCEL
    }

    companion object {
        val LOG = Logger.getInstance(AssetVercheck::class.java)
    }
}