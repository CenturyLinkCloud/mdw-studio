package com.centurylink.mdw.studio.action

import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.studio.file.ProcessFileType
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.SmartPointerManager
import javax.swing.Icon

abstract class NewAssetAction(val title: String, description: String, icon: Icon) :
        CreateFileFromTemplateAction(title, description, icon) {

    abstract val fileExtension: String

    override fun createFile(name: String, templatePath: String, dir: PsiDirectory): PsiFile? {
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

        val project = dir.project
        val content = loadTemplate(fileName, templatePath)
        var psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, ProcessFileType, content)
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