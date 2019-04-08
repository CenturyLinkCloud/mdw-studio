package com.centurylink.mdw.studio.console

import com.centurylink.mdw.cli.Setup
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.centurylink.mdw.studio.ui.ProgressMonitor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

class MdwConsole(private val projectSetup: ProjectSetup, toolWindow: ToolWindow) {

    var isDebug = false
    val consoleView: ConsoleView

    init {
        val consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(projectSetup.project)
        consoleView = consoleBuilder.console
        val toolWindowPanel = object: SimpleToolWindowPanel(false) {
            override fun isToolbarVisible(): Boolean {
                return true
            }
        }
        toolWindowPanel.setContent(consoleView.component)
        toolWindowPanel.toolbar = createToolbar().component
        val content = toolWindow.contentManager.factory.createContent(toolWindowPanel, "Console", false)
        toolWindow.contentManager.addContent(content)
        Disposer.register(projectSetup.project, content)
        Disposer.register(projectSetup.project, consoleView)
    }

    fun print(output: String) {
        consoleView.print(output, ConsoleViewContentType.NORMAL_OUTPUT)
    }

    fun clear() {
        consoleView.clear()
        consoleView.isOutputPaused = false
    }

    fun run(operation: Setup, title: String = "Executing ${operation::class.simpleName}...") {
        show()
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
        }, title, false, projectSetup.project)
    }

    fun show() {
        ToolWindowManager.getInstance(projectSetup.project).getToolWindow(ID).show(null)
    }

    private fun createToolbar(): ActionToolbar {
        val group = DefaultActionGroup()
        group.add(object: ToggleAction("Verbose", "Debug level output", AllIcons.Actions.ShowHiddens) {
            override fun isSelected(event: AnActionEvent): Boolean {
                return isDebug
            }
            override fun setSelected(event: AnActionEvent, state: Boolean) {
                isDebug = state
            }
        })
        group.add(object: AnAction("Clear All", "Clear console", AllIcons.Actions.GC) {
            override fun update(event: AnActionEvent) {
                event.presentation.isEnabled = consoleView.contentSize > 0
            }
            override fun actionPerformed(event: AnActionEvent) {
                clear()
            }
        })
        return ActionManager.getInstance().createActionToolbar("MDW Console", group, false)
    }

    companion object {
        const val ID = "MDW"
        val ICON = IconLoader.getIcon("/icons/console.png")
    }
}
