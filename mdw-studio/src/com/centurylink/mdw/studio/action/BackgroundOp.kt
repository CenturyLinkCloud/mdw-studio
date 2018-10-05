package com.centurylink.mdw.studio.action

import com.centurylink.mdw.cli.Operation
import com.centurylink.mdw.cli.Setup
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.centurylink.mdw.studio.ui.ProgressMonitor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator

class BackgroundOp(title: String, projectSetup: ProjectSetup, val operation: Operation) :
        Task.Backgroundable(projectSetup.project, title) {

    private var finished: (() -> Unit)? = null
    var status: Status = Status(false, "")
        private set

    init {
        if (operation is Setup) {
            operation.configLoc = projectSetup.configLoc
            operation.assetLoc = projectSetup.assetRoot.path
            val mdwVersion = projectSetup.mdwVersion
            operation.mdwVersion = mdwVersion.toString()
            operation.isSnapshots = mdwVersion.isSnapshot
        }
    }

    fun runAsync(onFinished: (() -> Unit)? = null) {
        this.finished = onFinished
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(this,
                BackgroundableProcessIndicator(this))
    }

    override fun onSuccess() {
        status = Status(true, "Succeeded")
    }

    override fun onCancel() {
        status = Status(false, "Cancelled")
    }

    override fun onThrowable(error: Throwable) {
        LOG.warn("Error executing '$title'", error)
        status = Status(false, "Error: ${error.message}")
    }

    override fun onFinished() {
        finished?.invoke()
    }

    override fun run(indicator: ProgressIndicator) {
        operation.run(ProgressMonitor(indicator))
    }

    companion object {
        data class Status(val isSuccess: Boolean, val message: String)
        val LOG = Logger.getInstance(BackgroundOp::class.java)
    }
}