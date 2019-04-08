package com.centurylink.mdw.studio.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class AssetAction : AnAction() {

    override fun update(event: AnActionEvent) {
        val presentation = event.presentation
        val locator = Locator(event)
        val applicable = locator.getPackage() != null || locator.getAsset() != null
        presentation.isVisible = applicable
        presentation.isEnabled = applicable
    }
}