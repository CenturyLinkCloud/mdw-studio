package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.RoundedBorder
import com.centurylink.mdw.draw.edit.*
import com.centurylink.mdw.draw.edit.apply.WidgetApplier
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.swing.*

open class Dialog(widget: Pagelet.Widget) : SwingWidget(widget)  {

    open fun showAndGet(initialValue: JsonValue): JsonValue? {
        val dialogWrapper = DialogWrapper(widget, projectSetup, initialValue.copy())
        return if (dialogWrapper.showAndGet()) {
            dialogWrapper.jsonValue
        } else {
            initialValue
        }
    }
}

open class DialogWrapper(widget: Pagelet.Widget, projectSetup: ProjectSetup, var jsonValue: JsonValue? = null) :
        com.intellij.openapi.ui.DialogWrapper(projectSetup.project) {

    protected val workflowObj = (widget.adapter as WidgetApplier).workflowObj

    val centerPanel = JPanel(BorderLayout())
    val okButton: JButton?
        get() = getButton(okAction)

    override fun createCenterPanel(): JComponent {
        return centerPanel
    }

    private val swingWidgets = mutableMapOf<String,SwingWidget>()

    init {
        init()
        title = widget.label

        val containerPane = JPanel()
        containerPane.layout = GridBagLayout()
        containerPane.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)

        val scrollPane = JBScrollPane(containerPane)
        scrollPane.border = RoundedBorder(JBColor.border())
        scrollPane.isOpaque = false
        scrollPane.viewport.isOpaque = false
        centerPanel.add(scrollPane)

        try {
            val containerConstraints = GridBagConstraints()
            containerConstraints.anchor = GridBagConstraints.NORTH
            containerConstraints.gridx = 0
            containerConstraints.gridy = 0
            containerConstraints.fill = GridBagConstraints.HORIZONTAL
            containerConstraints.weightx = 1.0
            containerConstraints.weighty = 1.0

            val layout = GridBagLayout()
            val gridPanel = JPanel(layout)

            val labelConstraints = GridBagConstraints()
            labelConstraints.gridx = 0
            labelConstraints.anchor = GridBagConstraints.NORTHWEST
            labelConstraints.fill = GridBagConstraints.NONE
            labelConstraints.weightx = 0.1

            val widgetConstraints = GridBagConstraints()
            labelConstraints.gridx = 1
            widgetConstraints.gridwidth = GridBagConstraints.REMAINDER
            widgetConstraints.anchor = GridBagConstraints.EAST
            widgetConstraints.fill = GridBagConstraints.HORIZONTAL
            widgetConstraints.weightx = 1.0

            for (widg in widget.widgets) {
                widg.init("dialog", workflowObj)
                widg.attributes["path"]?.let {
                    widg.value = jsonValue?.evalPath(it)
                    widg.valueString?.let {
                        if (widg.type == "link") {
                            widg.url = it
                        }
                    }
                }
                val label = Label(widg)
                gridPanel.add(label, labelConstraints)
                val swingWidget = createSwingWidget(widg)
                gridPanel.add(swingWidget, widgetConstraints)
            }

            // add gridPanel at the end to avoid adding when exception
            containerPane.add(gridPanel, containerConstraints)

            containerConstraints.gridy = 1
            containerConstraints.fill = GridBagConstraints.VERTICAL
            containerConstraints.gridheight = GridBagConstraints.REMAINDER
            containerConstraints.weighty = 100.0
            val glue = Box.createVerticalGlue()
            containerPane.add(glue, containerConstraints)
        }
        catch (ex: Exception) {
            ex.printStackTrace()
            // show stack trace in containerPane
            val outputStream = ByteArrayOutputStream()
            ex.printStackTrace(PrintStream(outputStream))
            val textArea = JTextArea(String(outputStream.toByteArray()))
            textArea.isEditable = false
            textArea.isOpaque = false
            textArea.lineWrap = false
            textArea.font = Font("monospaced", Font.PLAIN, 12)
            containerPane.removeAll()
            containerPane.add(textArea)
        }
    }

    private fun createSwingWidget(widget: Pagelet.Widget): SwingWidget {
        val swingWidget = SwingWidget.create(widget)
        swingWidget.addUpdateListener { obj ->
            println("TODO: perform update")
        }
        swingWidgets[widget.name] = swingWidget
        return swingWidget
    }

}
