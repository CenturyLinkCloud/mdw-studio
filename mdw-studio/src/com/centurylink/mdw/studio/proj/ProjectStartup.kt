package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.model.system.MdwVersion
import com.centurylink.mdw.studio.MdwSettings
import com.centurylink.mdw.studio.action.AssetUpdate
import com.centurylink.mdw.studio.action.UpdateNotificationAction
import com.centurylink.mdw.util.HttpHelper
import com.intellij.ide.plugins.PluginManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import kotlin.concurrent.thread

class ProjectStartup : StartupActivity {
    override fun runActivity(project: Project) {

        val projectSetup = project.getComponent(ProjectSetup::class.java)
        if (!projectSetup.isMdwProject) {
            // one more try in case new project
            projectSetup.assetDir = projectSetup.initialize()
            projectSetup.configure(false)
        }
        if (projectSetup.isMdwProject) {
            val pluginVer = projectSetup.getPluginVersion()
            try {
                LOG.info("MDW Studio: $pluginVer (${MdwVersion.getRuntimeVersion()})")
                val ideaVer = ApplicationInfo.getInstance().build.asString()
                LOG.info("  IDEA: $ideaVer")
                val kotlinVer = PluginManager.getPlugin(PluginId.getId("org.jetbrains.kotlin"))?.version
                LOG.info("  Kotlin: $kotlinVer")
            }
            catch (ex: Exception) {
                LOG.info("MDW Studio: $pluginVer")
                LOG.warn(ex)
            }

            // reload implementors (after dumb) to pick up annotation-driven impls
            projectSetup.reloadImplementors()

            if (!projectSetup.isFramework) {
                // check mdw assets
                val updateStatus = AssetUpdate(projectSetup).status
                if (updateStatus.isUpdateNeeded) {
                    val note = Notification("MDW", "MDW Assets", updateStatus.reason, NotificationType.WARNING)
                    note.addAction(UpdateNotificationAction(projectSetup, "Update MDW Assets"))
                    Notifications.Bus.notify(note, project)
                }
            }

            // associate .test files with groovy
            FileTypeManager.getInstance().getFileTypeByExtension("groovy").let {
                WriteAction.run<RuntimeException> {
                    FileTypeManager.getInstance().associateExtension(it, "test")
                }
            }

            // start the server detection background thread
            if (!MdwSettings.instance.isSuppressServerPolling) {
                projectSetup.hubRootUrl?.let {
                    val serverCheckUrl = URL("$it/services/AppSummary")
                    thread(true, true, name = "mdwServerDetection") {
                        while (true) {
                            Thread.sleep(ProjectSetup.SERVER_DETECT_INTERVAL)
                            try {
                                val response = HttpHelper(serverCheckUrl).get()
                                projectSetup.isServerRunning = JSONObject(response).has("mdwVersion")
                            } catch (e: IOException) {
                                projectSetup.isServerRunning = false
                            } catch (e: JSONException) {
                                projectSetup.isServerRunning = false
                            }
                        }
                    }
                }
            }

            MdwSettings.instance.getOrMakeMdwHome()
            System.setProperty("mdw.studio.version", pluginVer)

            // exclude temp and node_modules dirs
            projectSetup.tempDir?.let { tempDir ->
                projectSetup.getVirtualFile(tempDir)?.let { tempVirtualFile ->
                    if (tempVirtualFile.isDirectory) {
                        projectSetup.markExcluded(tempVirtualFile)
                    }
                }
            }
            projectSetup.getPackage("com.centurylink.mdw.node")?.let { nodePkg ->
                nodePkg.dir.findFileByRelativePath("node_modules")?.let { nodeMods ->
                    if (nodeMods.isDirectory) {
                        projectSetup.markExcluded(nodeMods)
                    }
                }
            }
        }
    }
    companion object {
        val LOG = Logger.getInstance(ProjectStartup::class.java)
    }
}

