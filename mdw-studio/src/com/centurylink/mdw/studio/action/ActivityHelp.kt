package com.centurylink.mdw.studio.action

import com.centurylink.mdw.draw.edit.isHelpLink
import com.centurylink.mdw.draw.edit.url
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.model.project.Data
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ActivityHelp : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        getHelpUrl(event)?.let { url ->
            BrowserUtil.browse("${Data.DOCS_URL}/$url")
        }
    }

    override fun update(event: AnActionEvent) {
        val applicable = getHelpUrl(event) != null
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }

    private fun getHelpUrl(event: AnActionEvent): String? {
        return Locator(event).implementor?.let { impl ->
            impl.pagelet?.let { p ->
                Pagelet(impl.category, p).widgets.find { w -> w.isHelpLink }?.url
            }
        }
    }
}