package com.centurylink.mdw.studio.vcs

import com.centurylink.mdw.studio.MdwSettings
import com.centurylink.mdw.studio.action.AssetVercheck
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.ui.BooleanCommitOption
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.openapi.vfs.VfsUtil
import java.util.function.Consumer

class AssetCheckinHandlerFactory : CheckinHandlerFactory() {

    override fun createHandler(checkinPanel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return AssetCheckinHandler(checkinPanel.project, checkinPanel)
    }
}

class AssetCheckinHandler(private val project: Project, private val checkinPanel: CheckinProjectPanel) : CheckinHandler() {

    override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent? {
        project.getComponent(ProjectSetup::class.java)?.let { projectSetup: ProjectSetup ->
            if (projectSetup.isMdwProject) {
                return BooleanCommitOption(checkinPanel, "Check MDW Asset Versions", true,
                        { !MdwSettings.instance.isSuppressPreCommitAssetVercheck },
                        Consumer { b -> MdwSettings.instance.isSuppressPreCommitAssetVercheck = !b })
            }
        }
        return null
    }

    override fun beforeCheckin(): ReturnResult {
        project.getComponent(ProjectSetup::class.java)?.let { projectSetup: ProjectSetup ->
            if (projectSetup.isMdwProject && !MdwSettings.instance.isSuppressPreCommitAssetVercheck) {
                val errorCount = AssetVercheck(projectSetup).performCheck()
                return if (errorCount > 0) {
                    if (MessageDialogBuilder.yesNo("Asset Version Conflict(s)",
                                    "Vercheck failed with $errorCount errors.  Fix?").isYes) {
                        AssetVercheck(projectSetup, true).performCheck()
                        VfsUtil.markDirtyAndRefresh(true, true, true, projectSetup.assetDir)
                    }
                    CheckinHandler.ReturnResult.CLOSE_WINDOW
                }
                else {
                    CheckinHandler.ReturnResult.COMMIT
                }
            }
        }

        return super.beforeCheckin()
    }
}