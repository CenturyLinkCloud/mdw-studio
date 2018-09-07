package com.centurylink.mdw.studio.action

import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.java.JavaNaming
import com.centurylink.mdw.studio.file.Icons
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import java.io.IOException

class NewActivityJava : NewAssetAction("Java Activity", "Create an activity implementor", Icons.JAVA) {

    override val fileExtension = "java"
    override val fileType = JavaFileType.INSTANCE

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(title)
            .addKind("General Activity", Icons.JAVA, "assets/code/activity/general_java")
            .addKind("Adapter Activity", Icons.JAVA, "assets/code/activity/adapter_java")
            .addKind("Evaluator Activity", Icons.JAVA, "assets/code/activity/evaluator_java")
    }

    override fun substitute(input: String): String {
        assetName?.let { fileName ->
            projectSetup?.let { projectSetup ->
                assetPackage?.dir?.let { dir ->
                    val values = mutableMapOf<String,Any?>()
                    values["className"] = baseName
                    values["packageName"] = assetPackage?.name
                    return Templates.substitute(input, values)
                }
            }
        }
        return ""  // better indication of failure than unsubstituted input
    }

    override fun createFile(name: String, templatePath: String, dir: PsiDirectory): PsiFile? {
        getFileName(name)
        if (baseName != JavaNaming.getValidClassName(baseName)) {
            throw IOException("Bad java file name")
        }
        val psiFile = super.createFile(name, templatePath, dir)
        // create the .impl file
        val implFileName = "$baseName.impl"
        var implTemplatePath = "assets/" + templatePath.substring(21, templatePath.lastIndexOf("_")) + ".impl"
        var content = substitute(loadTemplate(implFileName, implTemplatePath))
        createAndOpen(dir, implFileName, content)
        // focus back to java
        if (psiFile != null) {
            FileEditorManager.getInstance(psiFile.project).openFile(psiFile.virtualFile, true)
        }
        return psiFile
    }
}