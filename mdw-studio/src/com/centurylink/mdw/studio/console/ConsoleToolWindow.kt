package com.centurylink.mdw.studio.console

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class ConsoleToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    }

}

class ConsoleToolWindowCondition : Condition<Project> {
    override fun value(project: Project): Boolean {
        project.getComponent(ProjectSetup::class.java)?.let { projectSetup ->
            return projectSetup.isMdwProject
        }
        return false
    }
}

