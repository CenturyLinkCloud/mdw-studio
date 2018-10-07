package com.centurylink.mdw.studio.tool

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import javax.swing.BorderFactory
import javax.swing.JComponent

class ToolboxWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = object : SimpleToolWindowPanel(true) {
            override fun getComponent(): JComponent? {
                return super.getComponent()
            }

        }
        val projectSetup = project.getComponent(ProjectSetup::class.java)
        val toolboxPanel = ToolboxPanel(projectSetup)
        val scrollPane = JBScrollPane(toolboxPanel)
        scrollPane.border = BorderFactory.createEmptyBorder()
        panel.setContent(scrollPane)
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(panel, null, true)
        contentManager.addContent(content)
    }

    companion object {
        val instance: ToolboxWindowFactory by lazy {
            ToolboxWindowFactory()
        }
        const val ID = "Toolbox";
        val ICON = IconLoader.getIcon("/icons/toolbox.png")
    }
}

class ToolboxWindowCondition : Condition<Project> {
    override fun value(project: Project): Boolean {
        val projectSetup = project.getComponent(ProjectSetup::class.java)
        return projectSetup.isMdwProject
    }
}
