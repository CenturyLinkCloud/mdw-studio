package com.centurylink.mdw.studio.inspect

import com.centurylink.mdw.model.PackageDependency
import com.centurylink.mdw.model.project.Data
import com.centurylink.mdw.model.system.BadVersionException
import com.centurylink.mdw.studio.action.DependenciesCheck
import com.centurylink.mdw.studio.action.DependenciesLocator
import com.centurylink.mdw.studio.action.GitImport
import com.centurylink.mdw.studio.action.PackageDependencies
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.codeInspection.*
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType

class PackageDependenciesInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return DependenciesVisitor(holder)
    }
}

class DependenciesVisitor(val problemsHolder : ProblemsHolder) : PsiElementVisitor() {

    private val projectSetup: ProjectSetup = problemsHolder.project.getComponent(ProjectSetup::class.java)

    override fun visitFile(file: PsiFile) {
        super.visitFile(file)
        if (file.name == "project.yaml" && file.containingDirectory?.virtualFile == projectSetup.baseDir) {
            // this is mainly to avoid dorky "no suspicious code found" message
            problemsHolder.registerProblem(file, "Transitive dependencies are not checked (see ${Data.DOCS_URL})",
                    ProblemHighlightType.WARNING, null as LocalQuickFix?)
        }
    }

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        element.containingFile?.let { yamlFile ->
            if (yamlFile.name == "package.yaml" && yamlFile.containingDirectory.name == ".mdw") {
                // mdw package yaml
                if (element.elementType.toString() == "text") {
                    element.parent?.parent?.parent?.let { ggp ->
                        if (ggp.elementType?.toString() == "Sequence") {
                            // TODO assumes the only sequence in package.yaml is dependencies
                            val pkgVer = element.text
                            try {
                                if (DependenciesCheck.unmetDependencies.contains(PackageDependency(pkgVer))) {
                                    problemsHolder.registerProblem(element, pkgVer,
                                            ProblemHighlightType.ERROR, ImportPackageQuickFix(projectSetup, yamlFile.virtualFile, pkgVer))
                                }
                            } catch (ex: BadVersionException) {
                                problemsHolder.registerProblem(element, pkgVer, ProblemHighlightType.ERROR, null as LocalQuickFix?)
                            }
                        }
                    }
                }
            }
        }
    }
}

class ImportPackageQuickFix(private val projectSetup: ProjectSetup, private val file: VirtualFile, private val pkgVer: String) : LocalQuickFix {
    override fun getFamilyName(): String {
        return name
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        ApplicationManager.getApplication().invokeLater() {
            val found = DependenciesLocator(projectSetup, listOf(PackageDependency(pkgVer))).doFind()
            if (found.isEmpty()) {
                val msg = "Cannot find unmet dependency via discovery: $pkgVer"
                PackageDependencies.LOG.warn(msg)
                Notifications.Bus.notify(Notification("MDW", "Dependency Not Found", msg, NotificationType.ERROR), projectSetup.project)
            } else {
                val discovererPackages = found[0]
                for ((ref, dependencies) in discovererPackages.refDependencies) {
                    discovererPackages.discoverer.ref = ref
                    val msg = "Found ${dependencies.joinToString { it.toString() }} in ${discovererPackages.discoverer} (ref=$ref)"
                    PackageDependencies.LOG.info(msg);
                    Notifications.Bus.notify(Notification("MDW", "Importing Dependency...", msg, NotificationType.INFORMATION), projectSetup.project)
                    val gitImport = GitImport(projectSetup, discovererPackages.discoverer)
                    gitImport.doImport(dependencies.map { it.`package` }, object : DependenciesInspector {
                        override fun doInspect(projectSetup: ProjectSetup, files: List<VirtualFile>) {
                            PackageDependencies().doInspect(projectSetup, listOf(file))
                        }
                    })
                }
            }
        }
    }

    override fun getName(): String {
        return "Attempt to import package"
    }
}