package com.centurylink.mdw.studio.inspect

import com.centurylink.mdw.model.PackageDependency
import com.centurylink.mdw.model.project.Data
import com.centurylink.mdw.studio.action.DependenciesCheck
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
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
                            if (DependenciesCheck.unmetDependencies.contains(PackageDependency(pkgVer))) {
                                problemsHolder.registerProblem(element, pkgVer,
                                        ProblemHighlightType.ERROR, ImportPackageQuickFix(pkgVer))
                            }
                        }
                    }
                }
            }
        }
    }
}

class ImportPackageQuickFix(private val pkgVer: String) : LocalQuickFix {
    override fun getFamilyName(): String {
        return name
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        println("My Fix")
    }

    override fun getName(): String {
        return "Attempt to import package"
    }
}