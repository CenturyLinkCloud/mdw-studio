package com.centurylink.mdw.studio.action

import com.centurylink.mdw.draw.edit.UpdateListeners
import com.centurylink.mdw.draw.edit.UpdateListenersDelegate
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.studio.MdwSettings
import com.centurylink.mdw.studio.file.AttributeVirtualFile
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import java.awt.Component
import java.awt.Dimension
import java.io.IOException

/**
 * For activities whose main attribute can be opened in an inline editor.
 */
class ActivityEditAction(private val parent: Component, var workflowObj: WorkflowObj, var virtualFile: AttributeVirtualFile) :
        AnAction("Open " + FileTypeManager.getInstance().getFileTypeByExtension(virtualFile.ext).name, null,
                FileTypeManager.getInstance().getFileTypeByExtension(virtualFile.ext).icon),
        UpdateListeners by UpdateListenersDelegate() {

    val attributeName: String
        get() = if (workflowObj.getAttribute("Java") != null) "Java" else "Rule"

    override fun actionPerformed(event: AnActionEvent) {
        event.getData(CommonDataKeys.PROJECT)?.let { project ->
            showEditDialog(project)
        }
    }

    override fun update(event: AnActionEvent) {
        // dynamically determined in ProcessCanvas
        event.presentation.isVisible = true
        event.presentation.isEnabled = true
    }

    internal fun showEditDialog(project: Project) {
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        document ?: throw IOException("No document: " + virtualFile.path)

        document.addDocumentListener(object : DocumentListener {
            override fun beforeDocumentChange(e: DocumentEvent) {
            }
            override fun documentChanged(e: DocumentEvent) {
                workflowObj.setAttribute(attributeName, e.document.text)
                notifyUpdateListeners(workflowObj)
            }
        })

        workflowObj.getAttribute(attributeName)?.let { attr ->
            WriteAction.compute<Boolean,Throwable> {
                document.setText(attr.replace("\r", ""))
                true
            }
        }

        if (virtualFile.extension == "java") {
            ApplicationManager.getApplication().invokeLater {
                val className = virtualFile.syncDynamicJavaClassName()
                workflowObj.setAttribute("ClassName", className)
                notifyUpdateListeners(workflowObj)
            }
        }

        if (MdwSettings.instance.isOpenAttributeContentInEditorTab) {
            val descriptor = OpenFileDescriptor(project, virtualFile)
            FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
        }
        else {
            FileDocumentManagerImpl.registerDocument(document, virtualFile);
            val editor = PsiAwareTextEditorProvider.getInstance().createEditor(project, virtualFile);
            editor.component.preferredSize = Dimension(800, 600)
            val dialogBuilder = DialogBuilder(parent)
            dialogBuilder.setTitle(workflowObj.titlePath)
            dialogBuilder.setActionDescriptors(DialogBuilder.CloseDialogAction())
            dialogBuilder.setCenterPanel(editor.component)
            dialogBuilder.setDimensionServiceKey("mdw.AttributeSourceDialog")
            dialogBuilder.showNotModal()
        }
    }
}