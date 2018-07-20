package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.isReadonly
import com.google.gson.JsonObject
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import java.awt.FlowLayout
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.text.JTextComponent

@Suppress("unused")
class Dropdown(widget: Widget) : SwingWidget(widget) {

    init {
        isOpaque = false

        val combo = ComboBox(widget.options?.toTypedArray() ?: emptyArray<String>())
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