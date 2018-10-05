package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.label
import com.centurylink.mdw.model.asset.Pagelet.Widget
import javax.swing.JLabel
import javax.swing.JPanel

class Label(private val widget: Widget) : JPanel() {

    init {
        isOpaque = false
        add(JLabel(widget.label))
    }
}