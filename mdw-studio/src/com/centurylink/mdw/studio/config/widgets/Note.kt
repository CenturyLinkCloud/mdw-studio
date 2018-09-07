package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.model.asset.Pagelet
import javax.swing.BorderFactory
import javax.swing.JLabel

/**
 * Note widgets only have a label.
 */
@Suppress("unused")
class Note(widget: Pagelet.Widget) : SwingWidget(widget) {
    init {
        isOpaque = false
        add(JLabel(widget.valueString))
        border = BorderFactory.createEmptyBorder(3, 3, 0, 0)
    }
}