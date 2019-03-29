package com.centurylink.mdw.studio.console

import com.centurylink.mdw.cli.Setup
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import java.io.IOException

class ConsoleToolWindowFactory : ToolWindowFactory, Condition<Project> {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        project.getComponent(ProjectSetup::class.java)?.let { projectSetup ->
            val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(projectSetup.project).console
            val content = toolWindow.contentManager.factory.createContent(consoleView.component, "Console", false)
            toolWindow.contentManager.addContent(content)
            MdwConsole(projectSetup, consoleView)
        }
    }

    override fun value(project: Project): Boolean {
        project.getComponent(ProjectSetup::class.java)?.let { projectSetup ->
            return projectSetup.isMdwProject
        }
        return false
    }
}

class MdwConsole(val projectSetup: ProjectSetup, val consoleView: ConsoleView) {

    init {
        instance = this
    }

    fun print(output: String) {
        consoleView.print(output, ConsoleViewContentType.NORMAL_OUTPUT)
    }

    fun clear() {
        consoleView.clear()
    }

    fun run(operation: Setup) {
        clear()
        operation.configLoc = projectSetup.configLoc
        operation.assetLoc = projectSetup.assetRoot.path
        operation.gitRoot = projectSetup.gitRoot
        operation.isDebug = true
        operation.out = ConsolePrintStream(consoleView)
        operation.err = ConsolePrintStream(consoleView, true)
        try {
            operation.run()
        }
        catch (ex: IOException) {
            consoleView.print("${ex.stackTrace}", ConsoleViewContentType.ERROR_OUTPUT)

        }
        finally {
            operation.out.flush()
            operation.err.flush()
        }
    }

    companion object {
        lateinit var instance: MdwConsole
    }
}

