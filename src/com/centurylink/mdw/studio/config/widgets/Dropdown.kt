package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.isReadonly
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import java.awt.Dimension
import javax.swing.event.DocumentEvent
import javax.swing.text.JTextComponent

@Suppress("unused")
class Dropdown(widget: Widget) : SwingWidget(widget) {

    val combo = object : ComboBox<String>(widget.options?.toTypedArray() ?: emptyArray<String>()) {
        override fun getPreferredSize(): Dimension {
            val size = super.getPreferredSize()
            return Dimension(size.width, size.height - 2)
        }
    }

    init {
        isOpaque = false

        combo.isEditable = !widget.isReadonly
        combo.isEnabled = !widget.isReadonly

        combo.editor.item = widget.value

        val doc = (combo.editor.editorComponent as JTextComponent).document
        doc.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                widget.value = doc.getText(0, e.document.length)
                applyUpdate()
            }
        })

        add(combo)
    }
}