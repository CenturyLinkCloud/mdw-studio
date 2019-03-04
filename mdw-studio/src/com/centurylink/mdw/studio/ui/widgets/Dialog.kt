package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.label
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.proj.ProjectSetup
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

open class Dialog(widget: Pagelet.Widget) : SwingWidget(widget)  {

    open fun showAndGet(): JsonValue? {
        val dialogWrapper = DialogWrapper(widget, projectSetup)
        return if (dialogWrapper.showAndGet()) {
            dialogWrapper.jsonValue
        } else {
            null
        }
    }
}

open class DialogWrapper(private val widget: Pagelet.Widget, projectSetup: ProjectSetup) :
        com.intellij.openapi.ui.DialogWrapper(projectSetup.project) {

    val centerPanel = JPanel(BorderLayout())
    val okButton: JButton?
        get() = getButton(okAction)

    var jsonValue: JsonValue? = null

    override fun createCenterPanel(): JComponent {
        return centerPanel
    }

    init {
        init()
        title = widget.label

    }
}
