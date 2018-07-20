package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.google.gson.JsonObject
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.JCheckBox
import javax.swing.JPanel

@Suppress("unused")
class Checkbox (widget: Pagelet.Widget) : SwingWidget(widget) {

    init {
        isOpaque = false

        val checkbox = JCheckBox()
        checkbox.isOpaque = false
        checkbox.isSelected = widget.value as Boolean

        checkbox.addItemListener {
            widget.value = checkbox.isSelected
            applyUpdate()
        }

        add(checkbox)
    }
}