package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.centurylink.mdw.util.HttpHelper
import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import java.io.IOException
import java.lang.Thread.sleep
import java.net.URL
import javax.swing.JOptionPane
import kotlin.concurrent.thread


class SyncServer : ServerAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(CommonDataKeys.PROJECT)
        project?.getComponent(ProjectSetup::class.java)?.let {
            val url = it.hubRootUrl + "/services/WorkflowCache"
            val httpHelper = HttpHelper(URL(url))
            thread {
                try {
                    httpHelper.post("{}")
                    val note = Notification("MDW", "Synced", "MDW Server refresh completed",
                            NotificationType.INFORMATION)
                    Notifications.Bus.notify(note, project)
                    sleep(2000)
                    note.expire()
                }
                catch (e: IOException) {
                    LOG.error(e)
                    JOptionPane.showMessageDialog(null, "MDW server not reachable at:\n$url",
                            "Sync Server", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)
                }
            }
        }
    }

    companion object {
        val LOG = Logger.getInstance(SyncServer::class.java)
    }
}