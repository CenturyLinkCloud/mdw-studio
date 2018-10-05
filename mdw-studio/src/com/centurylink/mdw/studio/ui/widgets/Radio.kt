package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.model.asset.Pagelet
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JRadioButton

@Suppress("unused")
class Radio(widget: Pagelet.Widget) : SwingWidget(widget) {
    init {
        isOpaque = false


        val buttonGroup = ButtonGroup()

        for (option in widget.options) {
            val radioButton = JRadioButton(option)
            radioButton.isOpaque = false
            radioButton.border = BorderFactory.createEmptyBorder(0, 0, 0, 7)
            radioButton.actionCommand = option
            if (option == widget.valueString) {
                radioButton.isSelected = true
            }
            radioButton.addActionListener {
                widget.value = it.actionCommand
                applyUpdate()
            }
            buttonGroup.add(radioButton)
            add(radioButton)
        }
    }
}