package com.centurylink.mdw.studio.action

import com.centurylink.mdw.cli.Vercheck
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.centurylink.mdw.studio.vcs.CredentialsDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.vfs.VfsUtil
import java.io.IOException
import java.lang.reflect.InvocationTargetException


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
        val gitUser = projectSetup.settings.gitUser
        if (!gitUser.isBlank()) {
            vercheck.gitUser = gitUser
            vercheck.setGitPassword(projectSetup.settings.gitPassword)
        }

        projectSetup.console.run(vercheck)
        vercheck.exception?.let { ex ->
            if (ex is IOException && ex.cause is InvocationTargetException) {
                (ex.cause as InvocationTargetException).cause?.let { targetEx ->
                    targetEx.message?.let { message ->
                        if (message.endsWith("Authentication is required but no CredentialsProvider has been registered")) {
                            val dialog = CredentialsDialog(projectSetup)
                            if (dialog.showAndGet()) {
                                val retryVercheck = Vercheck()
                                retryVercheck.isWarn = true
                                retryVercheck.isFix = isFix
                                retryVercheck.gitUser = dialog.user
                                retryVercheck.setGitPassword(dialog.password)
                                projectSetup.console.run(retryVercheck)
                                return retryVercheck.errorCount
                            }
                        }
                    }
                }
            }
        }
        return vercheck.errorCount
    }

    companion object {
        val LOG = Logger.getInstance(AssetVercheck::class.java)
    }
}
