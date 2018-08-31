package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.centurylink.mdw.util.HttpHelper
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import java.io.IOException
import java.net.URL
import javax.swing.JOptionPane
import kotlin.concurrent.thread

class SyncServer : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(CommonDataKeys.PROJECT)
        project?.getComponent(ProjectSetup::class.java)?.let {
            val url = it.hubRootUrl + "/services/WorkflowCache"
            val httpHelper = HttpHelper(URL(url))
            thread {
                try {
                    httpHelper.post("{}")
                }
                catch (e: IOException) {
                    LOG.error(e)
                    JOptionPane.showMessageDialog(null, "MDW server not reachable at:\n$url",
                            "Sync Server", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)

                }
            }
        }
    }

    override fun update(event: AnActionEvent) {
        var applicable = false
        val project = event.getData(CommonDataKeys.PROJECT)
        project?.let {
            val projectSetup = project.getComponent(ProjectSetup::class.java)
            applicable = projectSetup.isMdwProject
        }
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }

    companion object {
        val LOG = Logger.getInstance(SyncServer.javaClass)
    }
}