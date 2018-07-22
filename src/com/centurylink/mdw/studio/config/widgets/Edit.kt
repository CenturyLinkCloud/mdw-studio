package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.edit.isReadonly
import com.centurylink.mdw.studio.edit.valueString
import com.centurylink.mdw.studio.file.AttributeVirtualFile
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.DialogBuilder
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.IOException
import javax.swing.JLabel

@Suppress("unused")
class Edit(widget: Pagelet.Widget) : SwingWidget(widget) {

    init {
        isOpaque = false
        val text = if (widget.isReadonly) "View" else "Edit"
        val linkLabel = JLabel("<html><a href='.'>$text</a></html>")
        linkLabel.toolTipText = "Open ${widget.attributes["languages"]}"
        linkLabel.cursor = Cursor(Cursor.HAND_CURSOR)
        linkLabel.addMouseListener(object: MouseAdapter() {
            override fun mouseReleased(e: MouseEvent?) {
                showEditDialog()
            }
        })
        add(linkLabel)
    }

    fun showEditDialog() {
        val applier = widget.adapter as WidgetApplier
        val workflowObj = applier.workflowObj

        val virtualFile = AttributeVirtualFile(workflowObj, widget.valueString)
        if (virtualFile.contents != widget.valueString) {
            // might have been set from template
            widget.value = virtualFile.contents
            applyUpdate()
        }
        val project = (workflowObj.project as ProjectSetup).project
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        document ?: throw IOException("No document: " + virtualFile.path)

        document.addDocumentListener(object: DocumentListener {
            override fun beforeDocumentChange(e: DocumentEvent) {
            }
            override fun documentChanged(e: DocumentEvent) {
                widget.value = e.document.text
                applyUpdate()
            }
        })
        val editor = EditorFactory.getInstance().createEditor(document, project, virtualFile, false)
        editor.component.preferredSize = Dimension(800, 600)

        val dialogBuilder = DialogBuilder(this)
        dialogBuilder.setTitle(applier.workflowObj.titlePath)
        dialogBuilder.setActionDescriptors(DialogBuilder.CloseDialogAction())

        dialogBuilder.setCenterPanel(editor.component)
        dialogBuilder.setDimensionServiceKey("mdw.AttributeSourceDialog")
        dialogBuilder.showNotModal()
    }
}