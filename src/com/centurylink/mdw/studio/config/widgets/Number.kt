package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.max
import com.centurylink.mdw.studio.edit.min
import com.intellij.ui.JBIntSpinner
import java.awt.Dimension
import javax.swing.JFormattedTextField
import javax.swing.text.DefaultFormatter

@Suppress("unused")
class Number(widget: Pagelet.Widget) : SwingWidget(widget) {

    val spinner: JBIntSpinner

    init {
        isOpaque = false

        var num = 0
        widget.value?.let {
            num = it as Int
        }
        var min = 0
        widget.min?.let {
            min = it as Int
        }
        var max = 1000
        widget.max?.let {
            max = it as Int
        }

        spinner = object : JBIntSpinner(num, min, max) {
            override fun getPreferredSize(): Dimension {
                val size = super.getPreferredSize()
                return Dimension(size.width, size.height - 2)
            }
        }

        val field = spinner.editor.getComponent(0) as JFormattedTextField
        (field.formatter as DefaultFormatter).commitsOnValidEdit = true

        spinner.addChangeListener {
            widget.value = spinner.number.toString()
            applyUpdate()
        }

        add(spinner)
    }
}