package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.units
import com.centurylink.mdw.studio.edit.valueString
import com.intellij.ui.JBIntSpinner
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JRadioButton
import groovyjarjarantlr.FileLineFormatter.getFormatter
import javax.swing.text.StyleConstants.getComponent
import javax.swing.JFormattedTextField
import javax.swing.JComponent
import javax.swing.text.DefaultFormatter


/**
 * This is actually for specifying a time interval
 */
@Suppress("unused")
class Datetime(widget: Pagelet.Widget) : SwingWidget(widget) {

    init {
        isOpaque = false

        var num = 0
        widget.value?.let {
            num = it as Int
        }
        val spinner = JBIntSpinner(num, 0, 10000)
        val field = spinner.editor.getComponent(0) as JFormattedTextField
        (field.formatter as DefaultFormatter).commitsOnValidEdit = true
        spinner.addChangeListener {
            widget.value = spinner.number
            applyUpdate()
        }
        spinner.editor.addPropertyChangeListener {  }
        add(spinner)

        // units
        val buttonGroup = ButtonGroup()
        val units = widget.attributes["units"] ?: "Minutes,Hours,Days"
        for (option in units.split(",")) {
            val radioButton = JRadioButton(option)
            radioButton.isOpaque = false
            radioButton.border = BorderFactory.createEmptyBorder(0, 0, 0, 7)
            radioButton.actionCommand = option
            if (option == widget.units) {
                radioButton.isSelected = true
            }
            radioButton.addActionListener {
                // convert value to reflect new units
                widget.units = it.actionCommand
                applyUpdate()
            }
            buttonGroup.add(radioButton)
            add(radioButton)
        }
    }
}