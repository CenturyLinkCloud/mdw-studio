package com.centurylink.mdw.studio.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class AssetAction : AnAction() {

    override fun update(event: AnActionEvent) {
        val presentation = event.presentation
        var applicable = false
        Locator(event).getAsset()?.let {
            applicable = true
        }
        presentation.isVisible = applicable
        presentation.isEnabled = applicable
    }
}