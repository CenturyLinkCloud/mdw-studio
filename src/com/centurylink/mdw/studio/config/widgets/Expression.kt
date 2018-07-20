package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.edit.valueString
import com.centurylink.mdw.studio.edit.width
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.EditorTextField
import java.awt.Dimension

/**
 * TODO: background
 */
@Suppress("unused")
class Expression(widget: Pagelet.Widget) : SwingWidget(widget) {

    init {
        isOpaque = false

        val applier = widget.adapter as WidgetApplier
        val workflowObj = applier.workflowObj
        val ext = if (workflowObj.obj.get("implementor") == "com.centurylink.mdw.kotlin.ScriptActivity") {
            "kts"
        } else {
            when(widget.attributes["SCRIPT"]) {
                "Kotlin Script" -> "kts"
                "javax.el" -> "java"
                "JavaScript" -> "js"
                else -> "groovy"
            }
        }
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension(ext)
        val textField = object : EditorTextField(widget.valueString ?: "",
                (workflowObj.project as ProjectSetup).project, fileType) {
            override fun getPreferredSize(): Dimension {
                val size = super.getPreferredSize()
                return Dimension(widget.width, size.height)
            }
        }
        add(textField)
        textField.addDocumentListener(object: com.intellij.openapi.editor.event.DocumentListener {
            override fun beforeDocumentChange(e: DocumentEvent) {
            }
            override fun documentChanged(e: DocumentEvent) {
                widget.value = e.document.text
                applyUpdate()
            }
        })
    }
}