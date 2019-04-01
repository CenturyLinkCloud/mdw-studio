package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.centurylink.mdw.util.HttpHelper
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import java.io.IOException
import java.net.URL
import javax.swing.JOptionPane
import kotlin.concurrent.thread

abstract class ServerAction : AnAction() {

    override fun update(event: AnActionEvent) {
        val project = event.getData(CommonDataKeys.PROJECT)
        project?.let {
            val projectSetup = project.getComponent(ProjectSetup::class.java)
            if (projectSetup.isMdwProject) {
                event.presentation.isVisible = true
                event.presentation.isEnabled = projectSetup.isServerRunning
                return
            }
        }
        event.presentation.isVisible = false
        event.presentation.isEnabled = false
    }
}

class HubAction : ServerAction() {

    override fun actionPerformed(event: AnActionEvent) {
        event.getData(CommonDataKeys.PROJECT)?.getComponent(ProjectSetup::class.java)?.let {
            val hubUrl = it.hubRootUrl
            if (hubUrl == null) {
                JOptionPane.showMessageDialog(null, "No mdw.hub.url found",
                        "Open MDWHub", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)
            }
            else {
                BrowserUtil.browse(hubUrl)
            }
        }
    }

}

class SyncServer : ServerAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(CommonDataKeys.PROJECT)
        project?.getComponent(ProjectSetup::class.java)?.let {
            triggerSaveAll()
            val url = it.hubRootUrl + "/services/WorkflowCache"
            val httpHelper = HttpHelper(URL(url))
            thread {
                try {
                    httpHelper.post("{}")
                    val note = Notification("MDW", "Synced", "MDW Server refresh completed",
                            NotificationType.INFORMATION)
                    Notifications.Bus.notify(note, project)
                    Thread.sleep(2000)
                    note.expire()
                }
                catch (e: IOException) {
                    LOG.warn(e)
                    JOptionPane.showMessageDialog(null, "MDW server not reachable at:\n$url",
                            "Sync Server", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)
                }
            }
        }
    }

    private fun triggerSaveAll() {
        val actionEvent = AnActionEvent(null, DataManager.getInstance().getDataContext(), ActionPlaces.UNKNOWN,
                Presentation(), ActionManager.getInstance(), 0)
        val saveAllAction = ActionManager.getInstance().getAction("SaveAll")
        saveAllAction.actionPerformed(actionEvent)
    }

    companion object {
        val LOG = Logger.getInstance(SyncServer::class.java)
    }
}