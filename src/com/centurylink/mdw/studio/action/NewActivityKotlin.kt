package com.centurylink.mdw.studio.action

import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.studio.file.Icons
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile

class NewActivityKotlin : NewAssetAction("Kotlin Activity", "Create an activity implementor", Icons.KOTLIN) {

    override val fileExtension = "kt"
    override val fileType = FileTypeManager.getInstance().getFileTypeByExtension("kt")

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(title)
            .addKind("General Activity", Icons.KOTLIN, "assets/code/activity/general_kt")
            .addKind("Adapter Activity", Icons.KOTLIN, "assets/code/activity/adapter_kt")
            .addKind("Evaluator Activity", Icons.KOTLIN, "assets/code/activity/evaluator_kt")
    }

    override fun substitute(input: String): String {
        assetName?.let { fileName ->
            projectSetup?.let { projectSetup ->
                assetPackage?.dir?.let { dir ->
                    val values = mutableMapOf<String,Any?>()
                    values.put("className", baseName)
                    values.put("packageName", assetPackage?.name)
                    return Templates.substitute(input, values)
                }
            }
        }
        return ""  // better indication of failure than unsubstituted input
    }

    override fun createFile(name: String, templatePath: String, dir: PsiDirectory): PsiFile? {
        val psiFile = super.createFile(name, templatePath, dir)
        // create the .impl file
        val implFileName = "$baseName.impl"
        var implTemplatePath = "assets/" + templatePath.substring(21, templatePath.lastIndexOf("_")) + ".impl"
        var content = substitute(loadTemplate(implFileName, implTemplatePath))
        createAndOpen(dir, implFileName, content)
        // focus back to kotlin
        if (psiFile != null) {
            FileEditorManager.getInstance(psiFile.project).openFile(psiFile.virtualFile, true)
        }
        return psiFile
    }

}