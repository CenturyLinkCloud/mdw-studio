package com.centurylink.mdw.studio.inspect

import com.centurylink.mdw.model.project.Data
import com.centurylink.mdw.model.system.BadVersionException
import com.centurylink.mdw.model.system.MdwVersion
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import org.jetbrains.yaml.psi.YAMLKeyValue


class ProjectYaml : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val projectSetup: ProjectSetup = holder.project.getComponent(ProjectSetup::class.java)
        return object : PsiElementVisitor() {

            /**
             * This is mainly to avoid dorky "no suspicious code found" message when checking dependencies.
             */
            override fun visitFile(file: PsiFile) {
                super.visitFile(file)
                if (file.name == "project.yaml" && file.containingDirectory?.virtualFile == projectSetup.baseDir) {
                    //
                    holder.registerProblem(file, "Transitive package dependencies are not checked(see ${Data.DOCS_URL}/development/package-dependencies)",
                            ProblemHighlightType.WARNING, null as LocalQuickFix?)
                }
            }

            /**
             * Check MDW version versus project.yaml version.
             */
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                element.containingFile?.let { yamlFile ->
                    if (yamlFile.name == "project.yaml" && yamlFile.containingDirectory?.virtualFile == projectSetup.baseDir) {
                        if (element is YAMLKeyValue && element.keyText == "version") {
                            element.parent?.parent?.let { gp ->
                                if (gp is YAMLKeyValue && gp.keyText == "mdw") {
                                    try {
                                        val version = MdwVersion(element.valueText)
                                        val mdwLibs = projectSetup.mdwLibraryDependencies
                                        println("mdwLibs")
                                        for ((lib, ver) in mdwLibs) {
                                            if (ver != version) {
                                                holder.registerProblem(element, "mdw.version does not match dependency version: $lib $ver", ProblemHighlightType.ERROR, null as LocalQuickFix?)
                                                break
                                            }
                                        }
                                    } catch (ex: BadVersionException) {
                                        holder.registerProblem(element, "Bad mdw.version: ${ex.message}", ProblemHighlightType.ERROR, null as LocalQuickFix?)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}