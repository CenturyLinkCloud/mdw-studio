package com.centurylink.mdw.studio.action

import com.centurylink.mdw.app.Templates
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

    var projectSetup: ProjectSetup? = null
    var assetPackageName: String? = null

    override fun beforeActionPerformedUpdate(event: AnActionEvent) {
        super.beforeActionPerformedUpdate(event)
        projectSetup = event.getData(CommonDataKeys.PROJECT)?.getComponent(ProjectSetup::class.java) ?: return
        val view = LangDataKeys.IDE_VIEW.getData(event.dataContext) ?: return
        val dir = view.getOrChooseDirectory() ?: return
        assetPackageName = projectSetup?.getPackageName(dir.virtualFile)
    }

    override fun createFile(name: String, templatePath: String, dir: PsiDirectory): PsiFile? {
        val fileName = getAssetName(name)
        val content = substitute(loadTemplate(fileName, templatePath))
        return createAndOpen(dir, fileName, content)
    }

    open fun substitute(input: String, values: Map<String,Any?>? = null): String {
        return if (values == null) {
            input
        }
        else {
            Templates.substitute(input, values)
        }
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

    open fun getAssetName(name: String): String {
        var fileName = name
        val lastDot = fileName.lastIndexOf('.')
        if (lastDot > 0) {
            val ext = if (fileName.endsWith('.')) "" else fileName.substring(lastDot + 1)
            if (ext != fileExtension)
                fileName += ".$fileExtension"
        }
        else {
            fileName += ".$fileExtension"
        }
        return fileName
    }

    open fun getBaseName(name: String): String? {
        return getAssetName(name).let {
            it.substring(0, it.length - fileExtension.length - 1)
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
        if (event.presentation.isVisible && event.presentation.isEnabled) {
            val applicable = Locator(event).potentialPackageDir != null
            event.presentation.isVisible = applicable
            event.presentation.isEnabled = applicable
        }
    }
}