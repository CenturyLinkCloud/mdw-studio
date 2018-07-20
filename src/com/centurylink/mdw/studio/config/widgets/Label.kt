package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.label
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JLabel
import javax.swing.JPanel

class Label(private val widget: Widget) : JPanel() {

    init {
        isOpaque = false
        add(JLabel(widget.label))
    }
}