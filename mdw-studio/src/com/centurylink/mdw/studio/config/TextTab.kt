package com.centurylink.mdw.studio.config

import com.centurylink.mdw.draw.RoundedBorder
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JPanel
import javax.swing.JTextArea

class TextTab(text: String) : JPanel(BorderLayout()) {

    init {
        val textArea = JTextArea(text)
        textArea.isEditable = false
        textArea.isOpaque = false
        textArea.lineWrap = false
        textArea.font = Font("monospaced", Font.PLAIN, 12)

        val scrollPane = JBScrollPane(textArea)
        scrollPane.border = RoundedBorder(JBColor.border())
        scrollPane.isOpaque = false
        scrollPane.viewport.isOpaque = false
        add(scrollPane)
    }
}