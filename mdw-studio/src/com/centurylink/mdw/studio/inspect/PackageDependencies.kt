package com.centurylink.mdw.studio.inspect

import com.centurylink.mdw.studio.action.DependenciesCheck
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.elementType

class PackageDependenciesInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return DependenciesVisitor(holder)
    }
}

class DependenciesVisitor(val problemsHolder : ProblemsHolder) : PsiElementVisitor() {

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
                            if (DependenciesCheck.unmetDependencies.contains(pkgVer)) {
                                problemsHolder.registerProblem(element, "Unmet package dependency",
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