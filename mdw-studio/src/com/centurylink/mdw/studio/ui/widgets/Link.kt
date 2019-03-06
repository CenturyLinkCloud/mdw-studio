package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.*
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.BrowserUtil
import com.intellij.ui.DocumentAdapter
import java.awt.Component
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JTextField
import javax.swing.UIManager
import javax.swing.event.DocumentEvent

/**
 * Editable links are displayed as text widgets.
 */
@Suppress("unused")
class Link(widget: Pagelet.Widget) : SwingWidget(widget) {

    init {
        background = UIManager.getColor("EditorPane.background")
        border = BorderFactory.createEmptyBorder()

        if (widget.isReadonly) {
            val linkLabel = LinkLabel(widget.url ?: widget.label ?: widget.valueString ?: "")
            linkLabel.alignmentX = Component.LEFT_ALIGNMENT
            linkLabel.clickListener = {
                widget.valueString?.let { BrowserUtil.browse(it) }
            }
            add(linkLabel)
        }
        else {
            val textField = JTextField()
            textField.preferredSize = Dimension(widget.width, textField.preferredSize.height - 4)
            add(textField)
            if (ProjectSetup.isWindows) {
                textField.isEditable = !widget.isReadonly
            } else {
                textField.isEnabled = !widget.isReadonly
            }
            textField.text = widget.valueString
            textField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    widget.value = e.document.getText(0, e.document.length)
                    applyUpdate()
                }
            })
        }
    }
}