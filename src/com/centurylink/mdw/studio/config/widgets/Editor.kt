package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.edit.valueString
import com.centurylink.mdw.studio.file.AttributeVirtualFile
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import java.awt.BorderLayout
import java.io.IOException
import javax.swing.BorderFactory
import javax.swing.UIManager

@Suppress("unused")
class Editor(widget: Pagelet.Widget) : SwingWidget(widget, BorderLayout()) {
    init {
        background = UIManager.getColor("EditorPane.background")
        border = BorderFactory.createEmptyBorder()

        val applier = widget.adapter as WidgetApplier
        val workflowObj = applier.workflowObj

        val virtualFile = AttributeVirtualFile(workflowObj, widget.valueString, widget.attributes["ext"])
        val project = (workflowObj.project as ProjectSetup).project
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        document ?: throw IOException("No document: " + virtualFile.path)

        document.addDocumentListener(object: DocumentListener {
            override fun beforeDocumentChange(e: com.intellij.openapi.editor.event.DocumentEvent) {
            }
            override fun documentChanged(e: com.intellij.openapi.editor.event.DocumentEvent) {
                widget.value = e.document.text
                applyUpdate()
            }
        })
        val editor = EditorFactory.getInstance().createEditor(document, project, virtualFile, false)
        add(editor.component)
    }
}