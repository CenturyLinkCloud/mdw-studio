package com.centurylink.mdw.studio.action

import com.centurylink.mdw.draw.edit.UpdateListeners
import com.centurylink.mdw.draw.edit.UpdateListenersDelegate
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.studio.file.AttributeVirtualFile
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import java.awt.Dimension
import java.io.IOException

/**
 * For activities whose main attribute can be opened in an inline editor.
 */
class ActivityEditAction(var workflowObj: WorkflowObj, var virtualFile: AttributeVirtualFile) :
        AnAction("Open " + FileTypeManager.getInstance().getFileTypeByExtension(virtualFile.getExt()).name, null,
                FileTypeManager.getInstance().getFileTypeByExtension(virtualFile.getExt()).icon),
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

    private fun showEditDialog(project: Project) {
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        document ?: throw IOException("No document: " + virtualFile.path)

        WriteAction.run<Throwable> {
            document.setText(workflowObj.getAttribute(attributeName)?.replace("\r", "") ?: "")
        }

        document.addDocumentListener(object : DocumentListener {
            override fun beforeDocumentChange(e: DocumentEvent) {
            }
            override fun documentChanged(e: DocumentEvent) {
                workflowObj.setAttribute(attributeName, e.document.text)
                notifyUpdateListeners(workflowObj)
            }
        })
        val editor = EditorFactory.getInstance().createEditor(document, project, virtualFile, false)
        editor.component.preferredSize = Dimension(800, 600)

        val dialogBuilder = DialogBuilder(project)
        dialogBuilder.setTitle(workflowObj.titlePath)
        dialogBuilder.setActionDescriptors(DialogBuilder.CloseDialogAction())

        dialogBuilder.setCenterPanel(editor.component)
        dialogBuilder.setDimensionServiceKey("mdw.AttributeSourceDialog")
        dialogBuilder.showNotModal()
    }
}