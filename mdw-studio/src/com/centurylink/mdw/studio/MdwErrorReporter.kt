package com.centurylink.mdw.studio

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.util.Consumer
import java.awt.Component
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.ide.DataManager
import com.centurylink.mdw.util.HttpHelper
import com.intellij.openapi.diagnostic.Logger
import java.net.URL
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream

class MdwErrorReporter : ErrorReportSubmitter() {

    override fun getReportActionText(): String {
        return "Report to MDW"
    }

    override fun submit(events: Array<out IdeaLoggingEvent>, additionalInfo: String?, parentComponent: Component, consumer: Consumer<SubmittedReportInfo>): Boolean {
        val dataContext = DataManager.getInstance().getDataContext(parentComponent)
        val project = PlatformDataKeys.PROJECT.getData(dataContext)
        project?.let {
            try {
                val projectSetup = it.getComponent(ProjectSetup::class.java)
                val url = projectSetup.getMdwCentralUrl() + "/api/errors"
                val json = JSONObject()
                val error = JSONObject()
                json.put("error", error)
                error.put("source", "mdw-designer v" + projectSetup.getPluginVersion())
                error.put("message", events[0].message)
                val throwable = events[0].throwable
                if (throwable != null) {
                    val out = ByteArrayOutputStream()
                    throwable.printStackTrace(PrintStream(out))
                    error.put("stackTrace", String(out.toByteArray()))
                }
                val headers = hashMapOf<String, String>()
                headers["Content-Type"] = "application/json"
                headers["mdw-app-token"] = Secrets.MDW_STUDIO_TOKEN
                val httpHelper = HttpHelper(URL(url))
                httpHelper.headers = headers
                httpHelper.post(json.toString())
            }
           catch (e: IOException) {
                LOG.warn(e)
           }
        }
        return true
    }

    companion object {
        val LOG = Logger.getInstance(MdwErrorReporter::class.java)
    }
}