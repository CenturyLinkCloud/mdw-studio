package com.centurylink.mdw.studio.action

import com.centurylink.mdw.java.JavaNaming
import com.centurylink.mdw.studio.file.Icons
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import java.io.IOException
import javax.swing.Icon

abstract class NewHandler(title: String, icon: Icon) : NewAssetAction(title, DESCRIPTION, icon) {

    override fun createFile(name: String, templatePath: String, dir: PsiDirectory): PsiFile? {
        val assetName = getAssetName(name)
        val baseName = getBaseName(name)
        if (baseName != JavaNaming.getValidClassName(baseName)) {
            throw IOException("Bad asset name")
        }

        val values = mutableMapOf<String,Any?>(
                "className" to baseName,
                "packageName" to assetPackageName
        )
        val content = substitute(loadTemplate(assetName, templatePath), values)
        val psiFile = createAndOpen(dir, assetName, content)
        projectSetup?.reloadImplementors()
        return psiFile
    }


    companion object {
        const val DESCRIPTION = "Create a request handler"
    }
}

class NewHandlerJava : NewHandler("Java Request Handler", Icons.JAVA) {

    override val fileExtension = "java"
    override val fileType = JavaFileType.INSTANCE

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
                .setTitle(title)
                .addKind("General Handler", Icons.JAVA, "assets/code/handler/general_java")
                .addKind("Process Run", Icons.JAVA, "assets/code/handler/process_run_java")
                .addKind("Process Notify", Icons.JAVA, "assets/code/handler/process_notify_java")
    }
}

class NewHandlerKotlin : NewHandler("Kotlin Request Handler", Icons.KOTLIN) {

    override val fileExtension = "kt"
    override val fileType = FileTypeManager.getInstance().getFileTypeByExtension("kt")

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
                .setTitle(title)
                .addKind("General Handler", Icons.JAVA, "assets/code/handler/general_kt")
                .addKind("Process Run", Icons.JAVA, "assets/code/handler/process_run_kt")
                .addKind("Process Notify", Icons.JAVA, "assets/code/handler/process_notify_kt")
    }
}
