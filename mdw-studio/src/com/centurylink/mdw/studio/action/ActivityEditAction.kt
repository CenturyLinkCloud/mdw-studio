package com.centurylink.mdw.studio.action

import com.centurylink.mdw.draw.edit.UpdateListeners
import com.centurylink.mdw.draw.edit.UpdateListenersDelegate
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.studio.file.AttributeVirtualFile
import com.centurylink.mdw.studio.proj.Implementors
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException

/**
 * For activities whose main attribute can be opened in an inline editor.
 */
class ActivityEditAction(var workflowObj: WorkflowObj, var virtualFile: AttributeVirtualFile) :
        AnAction("Open " + FileTypeManager.getInstance().getFileTypeByExtension(virtualFile.ext).name.let { if (it == "UNKNOWN") ".${virtualFile.ext}" else it }, null,
                FileTypeManager.getInstance().getFileTypeByExtension(virtualFile.ext).let { if (it.name == "UNKNOWN") null else it.icon } ),
        UpdateListeners by UpdateListenersDelegate() {

    val attributeName: String
        get() {
            val isJava = workflowObj.getAttribute("Java") != null ||
                    workflowObj.obj.optString("implementor") == Implementors.DYNAMIC_JAVA
            return if (isJava) {
                "Java"
            }
            else {
                "Rule"
            }
        }

    override fun actionPerformed(event: AnActionEvent) {
        event.getData(CommonDataKeys.PROJECT)?.let { project ->
            openEditor(project)
        }
    }

    override fun update(event: AnActionEvent) {
        // dynamically determined in ProcessCanvas
        event.presentation.isVisible = true
        event.presentation.isEnabled = true
    }

    internal fun openEditor(project: Project) {
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        document ?: throw IOException("No document: " + virtualFile.path)

        val projectSetup = workflowObj.project as ProjectSetup
        projectSetup.attributeDocumentHandler.lock(virtualFile)

        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(e: DocumentEvent) {
                workflowObj.setAttribute(attributeName, e.document.text)
                notifyUpdateListeners(workflowObj)
            }
        })

        val connection = project.messageBus.connect(project)
        connection.subscribe<FileEditorManagerListener>(FileEditorManagerListener.FILE_EDITOR_MANAGER, object: FileEditorManagerListener {
            override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                if (file == virtualFile) {
                    connection.disconnect()
                    projectSetup.attributeDocumentHandler.unlock(virtualFile)
                }
            }
        })

        if (virtualFile.extension == "java") {
            ApplicationManager.getApplication().invokeLater {
                val className = virtualFile.syncDynamicJavaClassName()
                workflowObj.setAttribute("ClassName", className)
                notifyUpdateListeners(workflowObj)
            }
        }

        val descriptor = OpenFileDescriptor(project, virtualFile)
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
    }
}