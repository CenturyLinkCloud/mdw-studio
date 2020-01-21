package com.centurylink.mdw.studio.action

import com.centurylink.mdw.cli.Dependencies
import com.centurylink.mdw.model.system.MdwVersion
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.actions.CodeInspectionAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile

class PackageDependencies : CodeInspectionAction() {

    override fun update(event: AnActionEvent) {
        var applicable = false
        Locator(event).projectSetup?.let { projectSetup ->
            applicable = if (event.place == "MainMenu") {
                true
            } else {
                val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
                file == projectSetup.project.baseDir || file == projectSetup.assetDir
            }
        }
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).projectSetup?.let { projectSetup ->
            val depsCheck = DependenciesCheck(projectSetup)
            val unmet = depsCheck.performCheck()
            val pkgYamlFiles = mutableListOf<VirtualFile>()
            for (pkg in projectSetup.packages) {
                pkgYamlFiles.add(pkg.metaFile)
            }
            analyze(projectSetup.project, AnalysisScope(projectSetup.project, pkgYamlFiles))
            if (unmet.isNotEmpty()) {
                // TODO import dialog
            }
        }
    }
}

class DependenciesCheck(private val projectSetup: ProjectSetup) {

    private val dependencies = Dependencies()

    fun performCheck(): Map<String,MdwVersion> {
        projectSetup.console.run(dependencies, "Checking dependencies...")

        val unmet = mutableListOf<String>()
        for ((pkg, ver) in dependencies.unmetDependencies) {
            val pkgVer = pkg + ver.label
            if (!unmet.contains(pkgVer)) {
                unmet.add(pkgVer)
            }
        }
        unmetDependencies = unmet
        return dependencies.unmetDependencies
    }

    companion object {
        var unmetDependencies = listOf<String>()
    }
}
