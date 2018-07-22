package com.centurylink.mdw.studio.config

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.config.widgets.Editor
import com.centurylink.mdw.studio.config.widgets.Label
import com.centurylink.mdw.studio.config.widgets.SwingWidget
import com.centurylink.mdw.studio.config.widgets.Text
import com.centurylink.mdw.studio.draw.RoundedBorder
import com.centurylink.mdw.studio.edit.*
import com.google.gson.GsonBuilder
import com.intellij.ui.components.JBScrollPane
import java.awt.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.swing.*


class ConfigTab(tabName: String, template: Template, workflowObj: WorkflowObj) : JPanel(BorderLayout()),
        UpdateListeners by UpdateListenersDelegate() {

    var configurator: Configurator? = null

    val containerPane = JPanel()
    var scrollPane: JBScrollPane? = null

    init {
        background = getBackgroundColor()
        containerPane.background = getBackgroundColor()

        println("PAGELET: " + GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(template.pagelet))

        try {
            configurator = Configurator(tabName, template, workflowObj)

            val editorWidget = template.pagelet.widgets.find { it.isEditor }
            if (editorWidget != null) {
                // plain layout
                containerPane.layout = BorderLayout()
                containerPane.border = RoundedBorder()

                val editor = Editor(editorWidget)
                editor.addUpdateListener { obj ->
                    notifyUpdateListeners(obj)
                }
                containerPane.add(editor)
                add(containerPane)
            }
            else {
                addScrollPane()

                val containerConstraints = GridBagConstraints()
                containerConstraints.anchor = GridBagConstraints.NORTH
                containerConstraints.gridx = 0
                containerConstraints.gridy = 0
                containerConstraints.fill = GridBagConstraints.HORIZONTAL
                containerConstraints.weightx = 1.0
                containerConstraints.weighty = 1.0

                // regular widgets come first
                val regularWidgets = template.pagelet.widgets.filter { !it.isTableType }
                if (!regularWidgets.isEmpty()) {
                    val layout = GridBagLayout()
                    val gridPanel = JPanel(layout)
                    gridPanel.background = getBackgroundColor()

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

                    for (widget in regularWidgets) {
                        val label = Label(widget)
                        gridPanel.add(label, labelConstraints)

                        val swingWidgetClass = javaClass.`package`.name + ".widgets." +
                                widget.type.substring(0, 1).toUpperCase() + widget.type.substring(1)

                        // TEMP try/catch
                        var swingWidget: SwingWidget = Text(widget)
                        try {
                            val widgetConstructor = Class.forName(swingWidgetClass).getConstructor(Pagelet.Widget::class.java)
                            swingWidget = widgetConstructor.newInstance(widget) as SwingWidget
                            swingWidget.addUpdateListener { obj ->
                                notifyUpdateListeners(obj)
                                // println("OBJ:" + workflowObj.toString(true))
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                        gridPanel.add(swingWidget, widgetConstraints)
                    }

                    // add gridPanel at the end to avoid adding when exception
                    containerPane.add(gridPanel, containerConstraints)
                }


                // table widgets
                //            val tab1 = JBTable(DefaultTableModel(5, 3))
                //            containerConstraints.gridy = 1
                //            containerConstraints.weightx = 1.0
                //            containerPane.add(tab1, containerConstraints)

                containerConstraints.gridy = 2
                containerConstraints.fill = GridBagConstraints.VERTICAL
                containerConstraints.gridheight = GridBagConstraints.REMAINDER
                // containerConstraints.anchor = GridBagConstraints.LINE_END
                containerConstraints.weighty = 100.0
                val glue = Box.createVerticalGlue()
                containerPane.add(glue, containerConstraints)
            }
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
            containerPane.add(textArea)
            if (scrollPane == null) {
                addScrollPane()
            }
        }
    }

    private fun addScrollPane() {
        containerPane.layout = GridBagLayout()
        containerPane.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)

        scrollPane = JBScrollPane(containerPane)
        scrollPane?.let {
            it.border = RoundedBorder()
            it.isOpaque = false
            it.viewport.isOpaque = false
            add(it)
        }
    }

    private fun getBackgroundColor(): Color {
        return UIManager.getColor("EditorPane.background")
    }
}