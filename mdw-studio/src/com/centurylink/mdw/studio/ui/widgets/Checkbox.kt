package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.model.asset.Pagelet
import javax.swing.BorderFactory
import javax.swing.JCheckBox

@Suppress("unused")
class Checkbox (widget: Pagelet.Widget) : SwingWidget(widget) {

    val checkbox = JCheckBox()

    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(0, 1, 0, 0)

        checkbox.isOpaque = false
        checkbox.isSelected = widget.value as Boolean

        checkbox.addItemListener {
            widget.value = checkbox.isSelected
            applyUpdate()
        }

        add(checkbox)
    }
}