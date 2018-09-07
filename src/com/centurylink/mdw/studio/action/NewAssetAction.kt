package com.centurylink.mdw.studio.action

import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.studio.file.AssetPackage
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.SmartPointerManager
import javax.swing.Icon

abstract class NewAssetAction(val title: String, description: String, icon: Icon) :
        CreateFileFromTemplateAction(title, description, icon) {

    abstract val fileExtension: String
    abstract val fileType: FileType

    // these are null until createFile() has been called
    val baseName: String?
        get() = assetName?.let{ it.substring(0, it.length - fileExtension.length - 1) }
    var projectSetup: ProjectSetup? = null
    var assetName: String? = null
    var assetPackage: AssetPackage? = null

    override fun beforeActionPerformedUpdate(event: AnActionEvent) {
        super.beforeActionPerformedUpdate(event)
        projectSetup = event.getData(CommonDataKeys.PROJECT)?.getComponent(ProjectSetup::class.java)
    }

    override fun createFile(name: String, templatePath: String, dir: PsiDirectory): PsiFile? {
        val fileName = getFileName(name)
        this.assetPackage = projectSetup?.getPackage(dir.virtualFile)
        val content = substitute(loadTemplate(fileName, templatePath))
        return createAndOpen(dir, fileName, content)
    }

    open fun substitute(input: String): String {
        return input
    }

    open fun createAndOpen(dir: PsiDirectory, fileName: String, content: String): PsiFile? {
        val project = dir.project
        var psiFile = PsiFileFactory.getInstance(dir.project).createFileFromText(fileName, fileType, content)
        psiFile = dir.add(psiFile) as PsiFile
        val virtualFile = psiFile.virtualFile
        if (virtualFile != null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
            val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(psiFile)
            return pointer.element
        }
        else {
            return null
        }
    }

    open fun getFileName(name: String): String {
        var fileName = name
        val lastDot = fileName.lastIndexOf('.')
        if (lastDot > 0) {
            val ext = if (fileName.endsWith('.')) "" else fileName.substring(lastDot + 1)
            if (ext != fileExtension)
                fileName += "." + fileExtension
        }
        else {
            fileName += "." + fileExtension
        }
        this.assetName = fileName
        return fileName
    }

    open fun loadTemplate(fileName: String, path: String): String {
        return Templates.get(path)
    }

    override fun getActionName(directory: PsiDirectory, newName: String, templateName: String): String {
        return title
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        val presentation = event.getPresentation()
        if (presentation.isVisible && presentation.isEnabled) {
            var applicable = false
            val project = event.getData(CommonDataKeys.PROJECT)
            project?.let {
                val projectSetup = project.getComponent(ProjectSetup::class.java)
                if (projectSetup.isMdwProject) {
                    val view = event.getData(LangDataKeys.IDE_VIEW)
                    view?.let {
                        val directories = view.directories
                        for (directory in directories) {
                            if (projectSetup.isAssetSubdir(directory.virtualFile)) {
                                applicable = true
                                break
                            }
                        }
                    }
                }
            }
            presentation.isVisible = applicable
            presentation.isEnabled = applicable
        }
    }
}