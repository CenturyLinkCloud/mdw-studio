package com.centurylink.mdw.studio.action

import com.centurylink.mdw.cli.Update
import com.centurylink.mdw.model.system.MdwVersion
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.centurylink.mdw.studio.tool.ToolboxWindowFactory
import com.centurylink.mdw.studio.ui.MessageDialog
import com.centurylink.mdw.util.file.Packages
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import java.io.File

class UpdateAssets : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).getProjectSetup()?.let { projectSetup ->
            val updater = AssetUpdate(projectSetup)
            val updateStatus = updater.status
            if (updateStatus.isUpdateNeeded) {
                updater.doUpdate()
            }
            else {
                MessageDialog(projectSetup.project, "MDW Asset Update", updateStatus.reason).show()
            }
        }
    }

    override fun update(event: AnActionEvent) {
        var applicable = false
        Locator(event).getProjectSetup()?.let { projectSetup ->
            val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
            applicable = file == projectSetup.project.baseDir || file == projectSetup.assetDir
        }
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
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
                    return Status(true, "Asset update needed: $mdwVersion")
                }
            }
            return Status(false, "Base assets already up-to-date with MDW $mdwVersion.")
        }

    fun doUpdate() {
        val update = Update(File(projectSetup.project.baseDir.path))
        val backgroundOp = BackgroundOp("Update MDW assets", projectSetup, update)
        backgroundOp.runAsync {
            val note = Notification("MDW", backgroundOp.title, backgroundOp.status.message,
                    when (backgroundOp.status.isSuccess) { true -> NotificationType.INFORMATION; else -> NotificationType.ERROR })
            Notifications.Bus.notify(note, projectSetup.project)
            if (backgroundOp.status.isSuccess) {
                note.expire()
                projectSetup.reloadImplementors()
                val toolWindowManager = ToolWindowManager.getInstance(projectSetup.project)
                if (toolWindowManager.getToolWindow(ToolboxWindowFactory.ID) == null) {
                    val toolbox = toolWindowManager.registerToolWindow(ToolboxWindowFactory.ID, false, ToolWindowAnchor.RIGHT)
                    toolbox.icon = ToolboxWindowFactory.ICON
                    ToolboxWindowFactory.instance.createToolWindowContent(projectSetup.project, toolbox)
                }
            }
            projectSetup.assetDir.refresh(true, true)
        }
    }

    companion object {
        data class Status(val isUpdateNeeded: Boolean, val reason: String)
    }
}
