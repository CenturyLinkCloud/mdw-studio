package com.centurylink.mdw.studio.action

import com.centurylink.mdw.cli.Update
import com.centurylink.mdw.model.system.MdwVersion
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.centurylink.mdw.studio.tool.ToolboxWindowFactory
import com.centurylink.mdw.file.Packages
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import java.io.File

/**
 * This is only for MDW public packages which can be downloaded from Maven Central.
 */
class UpdateAssets : AssetToolsAction() {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).projectSetup?.let { projectSetup ->
            AssetUpdate(projectSetup).doUpdate()
        }
    }
}

class UpdateNotificationAction(private val projectSetup: ProjectSetup, text: String) : NotificationAction(text) {

    override fun actionPerformed(event: AnActionEvent, notification: Notification) {
        notification.expire()
        AssetUpdate(projectSetup).doUpdate()
    }
}

class AssetUpdate(private val projectSetup: ProjectSetup) {

    val status: Status
        get() {
            val pkgs = projectSetup.packages
            if (pkgs.isEmpty()) {
                return Status(true, "Assets not found under: ${projectSetup.assetDir}")
            }
            if (pkgs.find { it.name == Packages.MDW_BASE } == null) {
                return Status(true, "Package ${Packages.MDW_BASE} not found")
            }
            val mdwVersion = projectSetup.mdwVersion
            for (pkg in pkgs) {
                if (Packages.isMdwPackage(pkg.name) && MdwVersion(pkg.verString) < mdwVersion) {
                    LOG.warn("Out-of-date MDW asset package: ${pkg.name} (${pkg.verString} < $mdwVersion)")
                    return Status(true, "Asset update needed: $mdwVersion")
                }
            }
            return Status(false, "Base assets already up-to-date with MDW $mdwVersion.")
        }

    fun doUpdate(packages: List<String>? = null) {
        val update = Update(File(projectSetup.project.baseDir.path))
        packages?.let{ update.baseAssetPackages = it }
        val backgroundOp = BackgroundOp("Update MDW assets", projectSetup, update)
        backgroundOp.runAsync {
            val note = Notification("MDW", backgroundOp.title, backgroundOp.status.message,
                    when (backgroundOp.status.isSuccess) { true -> NotificationType.INFORMATION; else -> NotificationType.ERROR })
            Notifications.Bus.notify(note, projectSetup.project)
            if (backgroundOp.status.isSuccess) {
                note.expire()
                val toolWindowManager = ToolWindowManager.getInstance(projectSetup.project)
                if (toolWindowManager.getToolWindow(ToolboxWindowFactory.ID) == null) {
                    val toolbox = toolWindowManager.registerToolWindow(ToolboxWindowFactory.ID, false, ToolWindowAnchor.RIGHT)
                    ToolboxWindowFactory.instance.createToolWindowContent(projectSetup.project, toolbox)
                }
            }
            VfsUtil.markDirty(true, true, projectSetup.assetDir)
            projectSetup.assetDir.refresh(true, true) {
                projectSetup.reloadImplementors()
            }
        }
    }

    companion object {
        val LOG = Logger.getInstance(AssetUpdate::class.java)
        data class Status(val isUpdateNeeded: Boolean, val reason: String)
    }
}
