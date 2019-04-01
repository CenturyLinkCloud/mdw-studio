package com.centurylink.mdw.studio.console

import com.centurylink.mdw.cli.Setup
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.centurylink.mdw.studio.tool.ToolboxWindowFactory
import com.centurylink.mdw.studio.ui.ProgressMonitor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

class ConsoleToolWindowFactory : ToolWindowFactory, Condition<Project> {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        project.getComponent(ProjectSetup::class.java)?.let { projectSetup ->
            val consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(projectSetup.project)
            val consoleView = consoleBuilder.console
            val toolWindowPanel = object: SimpleToolWindowPanel(false) {
                override fun isToolbarVisible(): Boolean {
                    return true
                }
            }
            toolWindowPanel.setContent(consoleView.component)
            MdwConsole.instance = MdwConsole(projectSetup, consoleView)
            toolWindowPanel.toolbar = createToolbar(MdwConsole.instance).component
            val content = toolWindow.contentManager.factory.createContent(toolWindowPanel, "Console", false)
            toolWindow.contentManager.addContent(content)
            Disposer.register(project, content)
            Disposer.register(project, consoleView)
        }
    }

    override fun value(project: Project): Boolean {
        project.getComponent(ProjectSetup::class.java)?.let { projectSetup ->
            return projectSetup.isMdwProject
        }
        return false
    }

    private fun createToolbar(console: MdwConsole): ActionToolbar {
        val group = DefaultActionGroup()
        group.add(object: ToggleAction("Verbose", "Debug level output", AllIcons.Actions.ShowHiddens) {
            override fun isSelected(event: AnActionEvent): Boolean {
                return console.isDebug
            }
            override fun setSelected(event: AnActionEvent, state: Boolean) {
                console.isDebug = state
            }
        })
        group.add(object: AnAction("Clear All", "Clear console", AllIcons.Actions.GC) {
            override fun update(event: AnActionEvent) {
                event.presentation.isEnabled = console.consoleView.contentSize > 0
            }
            override fun actionPerformed(event: AnActionEvent) {
                console.clear()
            }
        })
        return ActionManager.getInstance().createActionToolbar("MDW Console", group, false)
    }
}

class MdwConsole(val projectSetup: ProjectSetup, val consoleView: ConsoleView) {

    var isDebug = false

    fun print(output: String) {
        consoleView.print(output, ConsoleViewContentType.NORMAL_OUTPUT)
    }

    fun clear() {
        consoleView.clear()
        consoleView.isOutputPaused = false
    }

    fun run(operation: Setup) {
        show(projectSetup.project)
        clear()
        operation.configLoc = projectSetup.configLoc
        operation.assetLoc = projectSetup.assetRoot.path
        operation.gitRoot = projectSetup.gitRoot
        operation.isDebug = isDebug
        operation.out = ConsolePrintStream(consoleView)
        operation.err = ConsolePrintStream(consoleView, true)

        ProgressManager.getInstance().runProcessWithProgressSynchronously({
            val progressMonitor = ProgressManager.getInstance().progressIndicator?.let {
                ProgressMonitor(it)
            }

            try {
                print("Running ${operation::class.simpleName}...\n")
                operation.out.flush()
                operation.err.flush()
                if (isDebug) {
                    operation.debug()
                    operation.out.flush()
                    operation.err.flush()
                }
                operation.run(progressMonitor)
            }
            catch (ex: IOException) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                ex.printStackTrace(pw)
                consoleView.print("${sw}", ConsoleViewContentType.ERROR_OUTPUT)
            }
            finally {
                operation.out.flush()
                operation.err.flush()
            }
        }, "Executing ${operation::class.simpleName}...", false, projectSetup.project)
    }

    companion object {
        const val ID = "MDW"
        val ICON = IconLoader.getIcon("/icons/console.png")

        lateinit var instance: MdwConsole

        fun show(project: Project) {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            if (toolWindowManager.getToolWindow(ID) == null) {
                val console = toolWindowManager.registerToolWindow(ToolboxWindowFactory.ID, false, ToolWindowAnchor.BOTTOM)
                console.icon = ICON
                ToolboxWindowFactory.instance.createToolWindowContent(project, console)
            }
            ToolWindowManager.getInstance(project).getToolWindow(ID).show(null)
        }
    }
}

