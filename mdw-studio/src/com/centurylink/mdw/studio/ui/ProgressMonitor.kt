package com.centurylink.mdw.studio.ui

import com.intellij.openapi.progress.ProgressIndicator

/**
 * Adapts MDW CLI ProgressMonitor to IntelliJ ProgressIndicator
 */
class ProgressMonitor(private val indicator: ProgressIndicator) : com.centurylink.mdw.cli.ProgressMonitor {

    override fun progress(progress: Int) {
        indicator.fraction = progress / 100.toDouble()
    }

    override fun message(msg: String) {
        indicator.text = msg
    }

    override fun isCanceled(): Boolean {
        return indicator.isCanceled
    }

    override fun isSupportsMessage(): Boolean {
        return true
    }
}