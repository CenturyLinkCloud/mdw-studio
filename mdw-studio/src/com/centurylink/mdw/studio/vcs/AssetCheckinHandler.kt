package com.centurylink.mdw.studio.vcs

import com.centurylink.mdw.studio.action.AssetVercheck
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.intellij.openapi.project.Project

class AssetCheckinHandlerFactory : CheckinHandlerFactory() {

    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return AssetCheckinHandler(panel.project)
    }
}

class AssetCheckinHandler(private val project: Project) : CheckinHandler() {

    override fun beforeCheckin(): ReturnResult {
        project.getComponent(ProjectSetup::class.java)?.let { projectSetup: ProjectSetup ->
            if (projectSetup.isMdwProject) {
                AssetVercheck(projectSetup).performCheck()
            }
        }

        return ReturnResult.COMMIT
    }
}