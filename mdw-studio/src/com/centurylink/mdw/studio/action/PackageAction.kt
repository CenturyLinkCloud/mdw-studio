package com.centurylink.mdw.studio.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class PackageAction : AnAction() {

    override fun update(event: AnActionEvent) {
        val presentation = event.presentation
        val applicable =  Locator(event).selectedPackage?.let { true } ?: false
        presentation.isVisible = applicable
        presentation.isEnabled = applicable
    }
}