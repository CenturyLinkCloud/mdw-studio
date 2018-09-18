package com.centurylink.mdw.studio.action

import com.intellij.openapi.actionSystem.AnActionEvent

class ImportProcess  : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val ext = when (templatePresentation.text) {
            "From draw.io Diagram" -> "xml"
            else -> "bpmn"
        }

        getPackage(event)?.let { pkg ->

        }

    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        if (event.presentation.isVisible) {
            getPackage(event)?.let {
                event.presentation.isVisible = true
                event.presentation.isEnabled = true
            }
        }
    }
}