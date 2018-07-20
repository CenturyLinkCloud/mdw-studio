package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.max
import com.centurylink.mdw.studio.edit.min
import com.centurylink.mdw.studio.edit.valueString
import com.intellij.openapi.application.TransactionGuard
import com.intellij.ui.JBIntSpinner
import javax.swing.JCheckBox
import javax.swing.JSpinner
import javax.swing.event.ChangeListener

@Suppress("unused")
class Number(widget: Pagelet.Widget) : SwingWidget(widget) {

    init {
        isOpaque = false

        var num = 0
        widget.valueString?.let {
            num = it.toInt()
        }
        var min = 0
        widget.min?.let {
            min = it.toInt()
        }
        var max = 1000
        widget.max?.let {
            max = it.toInt()
        }
        val spinner = JBIntSpinner(num, min, max)

        spinner.addChangeListener {
            widget.value = spinner.number.toString()
            applyUpdate()
        }

        add(spinner)
    }
}