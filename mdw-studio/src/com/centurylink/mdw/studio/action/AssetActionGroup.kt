package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.AssetPackage
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project

open class AssetActionGroup : DefaultActionGroup() {

    override fun update(event: AnActionEvent) {
        val locator = Locator(event)
        val applicable = locator.getPackage() != null || locator.getAsset() != null
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }
}