package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.*
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import java.awt.Dimension
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.text.JTextComponent

/**
 * Expects a normal string widget value
 */
@Suppress("unused")
open class Text(widget: Widget) : SwingWidget(widget) {

    val textComponent: JTextComponent

    init {
        background = UIManager.getColor("EditorPane.background")

        if (widget.isMultiline) {
            border = BorderFactory.createEmptyBorder(0, 3, 0, 0)
            val rows = widget.rows ?: 8
            textComponent = object: JTextArea(rows, 0) {
                override fun getPreferredSize(): Dimension {
                    return Dimension(widget.width - (if (ProjectSetup.isWindows) 11 else 22),
                            super.getPreferredSize().height)
                }
            }
            textComponent.lineWrap = true
            textComponent.wrapStyleWord = true
            textComponent.font = font
            val tcBorder = BorderFactory.createEmptyBorder(2, 4, 0, 0)
            textComponent.border = tcBorder
            val scrollPane = JBScrollPane(textComponent, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
            val spBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, JBColor.border())
            scrollPane.border = spBorder;

            // otherwise focus borders not updated
            textComponent.addFocusListener(object: FocusListener {
                override fun focusLost(e: FocusEvent) {
                    scrollPane.border = spBorder
                    textComponent.border = tcBorder
                    invalidate()
                    repaint()
                }
                override fun focusGained(e: FocusEvent) {
                    if (ProjectSetup.isWindows) {
                        scrollPane.border = BorderFactory.createLineBorder(JTextField().selectionColor)

                    }
                    else {
                        scrollPane.border = JTextField().border
                        textComponent.border = BorderFactory.createEmptyBorder(0, 2, 0, 0)
                    }
                    invalidate()
                    repaint()
                }
            })

            add(scrollPane)
        }
        else {
            border = BorderFactory.createEmptyBorder()
            textComponent = JTextField()
            textComponent.preferredSize = Dimension(widget.width, textComponent.preferredSize.height - 4)
            add(textComponent)
        }

        if (ProjectSetup.isWindows) {
            textComponent.isEditable = !widget.isReadonly
        }
        else {
            textComponent.isEnabled = !widget.isReadonly
        }

        textComponent.text = widget.valueString

        textComponent.document.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                widget.value = e.document.getText(0, e.document.length)
                applyUpdate()
            }
        })
    }
}