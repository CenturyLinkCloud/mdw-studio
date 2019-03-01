package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.isReadonly
import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.draw.edit.width
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.BrowserUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JLabel
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
            val linkText = widget.attributes["label"] ?: widget.valueString
            val linkHtml = if (UIUtil.isUnderDarcula()) {
                "<html><a href='.' style='color:white;'>$linkText</a></html>"
            }
            else {
                "<html><a href='.'>$linkText</a></html>"
            }
            val link = JLabel(linkHtml)
            link.alignmentX = Component.LEFT_ALIGNMENT
            link.cursor = Cursor(Cursor.HAND_CURSOR)
            link.addMouseListener(object : MouseAdapter() {
                override fun mouseReleased(e: MouseEvent) {
                    val custom = widget.attributes["customWidget"]
                    if (custom == null) {
                        widget.valueString?.let { BrowserUtil.browse(it) }
                    }
                    else {
                        // TODO details dialog
                    }
                }
            })
            add(link)
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
    }}