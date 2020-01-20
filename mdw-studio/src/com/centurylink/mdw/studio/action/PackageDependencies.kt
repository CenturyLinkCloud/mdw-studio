package com.centurylink.mdw.studio.action

import com.centurylink.mdw.cli.Dependencies
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VfsUtil

class PackageDependencies : AssetToolsAction() {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).projectSetup?.let { projectSetup ->
            val autoImport = false
            val depsCheck = DependenciesCheck(projectSetup, autoImport)
            val unmetCount = depsCheck.performCheck()
            if (autoImport) {
                if (unmetCount > 0) {
                    VfsUtil.markDirtyAndRefresh(true, true, true, projectSetup.assetDir)
                }
            }
            else if (unmetCount > 0) {
                // todo dialog
            }

            println("DEPENDENCIES")
        }
    }
}

class DependenciesCheck(private val projectSetup: ProjectSetup, private val isImport: Boolean = false) {

    val dependencies = Dependencies()

    fun performCheck(): Int {
        dependencies.isImport = isImport
        val title = if (isImport) {
            "Importing dependencies..."
        }
        else {
            "Checking dependencies..."
        }
        projectSetup.console.run(dependencies, title)
        return dependencies.unmetDependencies.size
    }

    companion object {
        val LOG = Logger.getInstance(AssetVercheck::class.java)
    }
}
