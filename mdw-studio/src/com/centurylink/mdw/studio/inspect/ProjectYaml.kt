package com.centurylink.mdw.studio.inspect

import com.centurylink.mdw.model.project.Data
import com.centurylink.mdw.model.system.BadVersionException
import com.centurylink.mdw.model.system.MdwVersion
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.YAMLElementGenerator
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
                if (file.name == "project.yaml" && file.containingDirectory?.virtualFile == projectSetup.baseDir
                        && projectSetup.hasPackageDependencies) {
                    holder.registerProblem(file, "Note: Transitive package dependencies are not checked(see ${Data.DOCS_URL}/development/package-dependencies)",
                                ProblemHighlightType.WEAK_WARNING, null as LocalQuickFix?)
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
                                        for ((lib, ver) in mdwLibs) {
                                            if (ver != version) {
                                                holder.registerProblem(element, "mdw.version does not match dependency version: $lib $ver",
                                                        ProblemHighlightType.ERROR, VersionMismatchQuickFix(projectSetup, element, ver))
                                                break
                                            }
                                        }
                                    } catch (ex: BadVersionException) {
                                        holder.registerProblem(element, "Bad mdw.version: ${ex.message}",
                                                ProblemHighlightType.ERROR, null as LocalQuickFix?)
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

class VersionMismatchQuickFix(private val projectSetup: ProjectSetup, private val versionKeyValue: YAMLKeyValue, private val mdwVersion: MdwVersion) : LocalQuickFix {
    override fun getFamilyName(): String {
        return name
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val dummyKeyValue = YAMLElementGenerator.getInstance(projectSetup.project).createYamlKeyValue(versionKeyValue.keyText, mdwVersion.toString())
        dummyKeyValue.value?.let { versionKeyValue.setValue(it) }
    }

    override fun getName(): String {
        return "Set mdw.version to $mdwVersion"
    }
}