package com.centurylink.mdw.studio.action

import com.centurylink.mdw.cli.Dependencies
import com.centurylink.mdw.model.PackageDependency
import com.centurylink.mdw.studio.prefs.MdwSettings
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.actions.CodeInspectionAction
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem

class PackageDependencies : CodeInspectionAction() {

    override fun update(event: AnActionEvent) {
        var applicable = false
        Locator(event).projectSetup?.let { projectSetup ->
            applicable = if (event.place == "MainMenu") {
                true
            } else {
                val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
                file == projectSetup.baseDir || file == projectSetup.assetDir
            }
        }
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).projectSetup?.let { projectSetup ->
            val depsCheck = DependenciesCheck(projectSetup)
            val unmet = depsCheck.performCheck()
            if (unmet.isNotEmpty()) {
                var attemptImport = false
                val setting = MdwSettings.SUPPRESS_PROMPT_IMPORT_DEPENDENCIES
                if (!PropertiesComponent.getInstance().getBoolean(setting, false)) {
                    val res = MessageDialogBuilder
                            .yesNo("Unmet Dependencies", "Dependencies check failed with ${unmet.size} unmet.  Attempt import?")
                            .doNotAsk(object : DialogWrapper.DoNotAskOption.Adapter() {
                                override fun rememberChoice(isSelected: Boolean, res: Int) {
                                    if (isSelected) {
                                        attemptImport = res == Messages.YES
                                        PropertiesComponent.getInstance().setValue(setting, isSelected)
                                    }
                                }
                            })
                            .show()
                    attemptImport = res == Messages.YES
                }
                if (attemptImport) {
                    val found = mutableListOf<PackageDependency>()
                    for (discovererPackages in DependenciesLocator(projectSetup, unmet).doFind()) {
                        for ((ref, dependencies) in discovererPackages.refDependencies) {
                            found.addAll(dependencies)
                            discovererPackages.discoverer.ref = ref
                            val gitImport = GitImport(projectSetup, discovererPackages.discoverer)
                            gitImport.doImport(dependencies.map { it.packageName }, this)
                        }
                    }
                    for (packageDependency in unmet) {
                        if (!found.contains(packageDependency)) {
                            val msg = "Cannot find unmet dependency via discovery: $packageDependency"
                            LOG.warn(msg)
                            Notifications.Bus.notify(Notification("MDW", "Dependency Not Found", msg,
                                    NotificationType.ERROR), projectSetup.project)
                        }
                    }
                }
                else {
                    doInspect(projectSetup)
                }
            }
            else {
                doInspect(projectSetup)
            }
        }
    }

    fun doInspect(projectSetup: ProjectSetup) {
        val files = projectSetup.packageMetaFiles.toMutableList()
        LocalFileSystem.getInstance().findFileByIoFile(projectSetup.projectYaml)?.let { files.add(it) }
        analyze(projectSetup.project, AnalysisScope(projectSetup.project, files.toList()))
    }

    companion object {
        val LOG = Logger.getInstance(PackageDependencies::class.java)
    }
}

class DependenciesCheck(private val projectSetup: ProjectSetup) {

    private val dependencies = Dependencies()

    fun performCheck(): List<PackageDependency> {
        projectSetup.console.run(dependencies, "Checking dependencies...")

        val unmet = mutableListOf<PackageDependency>()
        for (packageDependency in dependencies.unmetDependencies) {
            if (!unmet.contains(packageDependency)) {
                unmet.add(packageDependency)
            }
        }
        unmetDependencies = unmet
        return dependencies.unmetDependencies
    }

    companion object {
        var unmetDependencies = listOf<PackageDependency>()
    }
}
