package com.centurylink.mdw.studio.config

import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.studio.draw.Drawable
import com.centurylink.mdw.studio.edit.*
import com.centurylink.mdw.studio.ext.JsonObject
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.google.gson.JsonObject
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.ui.paint.LinePainter2D
import com.intellij.ui.tabs.TabsUtil
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder


class ConfigPanel(val projectSetup: ProjectSetup) :
        JPanel(BorderLayout()), HideShowListener, SelectListener, UpdateListeners by UpdateListenersDelegate() {

    private val titleBar = TitleBar("")
    private var tabPanel: TabPanel? = null
    var hideShowListener: HideShowListener? = null

    init {
        add(titleBar, BorderLayout.NORTH)
        titleBar.hideShowListener = this
    }

    override fun onHideShow(show: Boolean) {
        titleBar.hideShowListener = null
        if (hideShowListener != null) {
            hideShowListener!!.onHideShow(show)
        }
        titleBar.hideShowListener = this
    }

    override fun onSelect(selectObjs: List<Drawable>) {
        tabPanel?.let {
            remove(it)
        }
        if (selectObjs.size == 1) {
            val selectObj = selectObjs[0]
            titleBar.itemLabel.text = selectObj.workflowObj.name.lines().joinToString(" ")
            val configTabsJson = allTabsJson.get(selectObj.workflowObj.type.toString()).asJsonObject
            titleBar.helpLink = findHelpLink(selectObj.workflowObj, configTabsJson)
            tabPanel = TabPanel(projectSetup, configTabsJson, selectObj.workflowObj)
            tabPanel?.addUpdateListener { obj ->
                notifyUpdateListeners(obj)
            }
            add(tabPanel, BorderLayout.CENTER)
        }
        else {
            titleBar.itemLabel.text = ""
            add(JPanel(), BorderLayout.CENTER)
        }
        // this is needed so that tab label MouseListener is active
        revalidate()
        repaint()
    }

    private fun findHelpLink(workflowObj: WorkflowObj, configTabsJson: JsonObject) : HelpLink? {
        for (tab in configTabsJson.keySet()) {
            val configTabJson = configTabsJson.get(tab)
            if (configTabJson.isJsonObject) {
                val template = getTabTemplate(projectSetup, configTabJson.asJsonObject, workflowObj)
                template?.let {
                    val helpLinkWidget = it.pagelet.widgets.find {
                        it.isHelpLink && (it.section == tab || (it.section == null && (tab == "Design" || tab == "General")))
                    }
                    if (helpLinkWidget != null) {
                        return HelpLink(helpLinkWidget.url ?: "", helpLinkWidget.name)
                    }
                }
            }
        }
        return null
    }

    companion object {

        val allTabsJson = JsonObject(Templates.get("configurator/config-tabs.json"))

        const val TITLE = "Configurator"
        const val TITLE_WIDTH = 110
        val ICON = IconLoader.getIcon("/icons/config.gif")
        val ICON_HIDE = AllIcons.General.HideToolWindow
        val ICON_HELP = AllIcons.General.Help_small
        const val PAD = 7
        val HEIGHT_ACTIVE = TabsUtil.getTabsHeight(JBUI.CurrentTheme.ToolWindow.tabVerticalPadding()) - PAD
        val HEIGHT_INACTIVE = HEIGHT_ACTIVE + 3
    }
}

interface HideShowListener {
    fun onHideShow(show: Boolean)
}

data class HelpLink(val url: String, val tooltip: String?)

fun getTabTemplate(projectSetup: ProjectSetup, tabJson: JsonObject, workflowObj: WorkflowObj): Template? {
    val tabTemplate = tabJson.get("_template")
    val templ = tabTemplate.asString
    if (templ == "<implementor>") {
        val implClass = workflowObj.get("implementor")
        val implementor = projectSetup.implementors[implClass]
        implementor?.let {
            return Template(JsonObject(implementor.json.toString()))
        }
    }
    else {
        val content = Templates.get("configurator/" + templ)
        content?.let {
            return Template(JsonObject(content))
        }
    }
    return null
}

class TitleBar(processName: String) : JPanel(BorderLayout()) {

    val title = Title(true)
    val itemLabel: JLabel
    val iconPanel: JPanel
    var helpLabel: JLabel
    var hideLabel: JLabel
    var hideShowListener: HideShowListener? = null
    var helpListener: MouseListener? = null
    var helpLink: HelpLink? = null
        set(value) {
            helpLabel.isEnabled = value != null
            helpLabel.toolTipText = value?.tooltip
            helpListener?.let { helpLabel.removeMouseListener(it) }
            value?.let {
                helpListener = object : MouseAdapter() {
                    override fun mouseReleased(e: MouseEvent) {
                        BrowserUtil.browse(ProjectSetup.HELP_LINK_URL + "/" + it.url)
                    }
                }
                helpLabel.addMouseListener(helpListener)
            }
            iconPanel.invalidate()
            iconPanel.repaint()
        }

    init {
        background = JBUI.CurrentTheme.ToolWindow.headerBackground(true)
        add(title, BorderLayout.WEST)
        itemLabel = JLabel(processName)
        itemLabel.horizontalAlignment = JLabel.CENTER
        itemLabel.border = EmptyBorder(-1, 0, ConfigPanel.PAD, ConfigPanel.PAD * 4)
        add(itemLabel, BorderLayout.CENTER)


        iconPanel = object: JPanel(BorderLayout()) {
            override fun getPreferredSize(): Dimension {
                return Dimension(40, super.getPreferredSize().height)
            }
        }
        iconPanel.border = EmptyBorder(-15, 0, -8, 5)
        helpLabel = JLabel(ConfigPanel.ICON_HELP)
        helpLabel.isEnabled = false
        iconPanel.add(helpLabel)

        hideLabel = JLabel(ConfigPanel.ICON_HIDE)
        hideLabel.toolTipText = "Hide"
        hideLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                hideShowListener.let {
                    hideShowListener!!.onHideShow(false)
                }
            }
            override fun mouseEntered(e: MouseEvent?) {
                hideLabel.isOpaque = true
            }
            override fun mouseExited(e: MouseEvent?) {
                hideLabel.isOpaque = false
            }
        })
        iconPanel.add(hideLabel, BorderLayout.EAST)

        add(iconPanel, BorderLayout.EAST)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D

        val borderY = (ConfigPanel.HEIGHT_ACTIVE - 1).toDouble()
        g2d.color = JBColor.border()
        LinePainter2D.paint(g2d, 0.toDouble(), borderY, width.toDouble(), borderY)
    }

    override fun getPreferredSize(): Dimension {
        val size = super.getPreferredSize()
        return Dimension(size.width, ConfigPanel.HEIGHT_ACTIVE)
    }
}

class PanelBar: JPanel() {

    val title = Title(false)
    val showButton = JPanel()
    var hideShowListener: HideShowListener? = null

    init {
        background = JBUI.CurrentTheme.ToolWindow.headerBackground(false)
        showButton.background = JBUI.CurrentTheme.ToolWindow.headerBackground(false)
        val flowLayout = FlowLayout(FlowLayout.LEFT, 0, 1)
        layout = flowLayout
        showButton.layout = FlowLayout(FlowLayout.LEFT, 0, 4)
        showButton.add(title)
        add(showButton)
        showButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                hideShowListener.let {
                    showButton.background = JBUI.CurrentTheme.ToolWindow.headerBackground(false)
                    hideShowListener!!.onHideShow(true)
                }
            }
            override fun mouseReleased(e: MouseEvent) {
                hideShowListener.let {
                    showButton.background = JBUI.CurrentTheme.ToolWindow.headerBackground(false)
                    hideShowListener!!.onHideShow(true)
                }
            }
            override fun mouseEntered(e: MouseEvent) {
                showButton.background = JBUI.CurrentTheme.ToolWindow.tabHoveredBackground()
            }
            override fun mouseExited(e: MouseEvent) {
                showButton.background = JBUI.CurrentTheme.ToolWindow.headerBackground(false)
            }
        })
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.color = JBColor.border()
        LinePainter2D.paint(g2d, 0.toDouble(), 0.toDouble(), width.toDouble(), 0.toDouble())
    }

    override fun getPreferredSize(): Dimension {
        val size = super.getPreferredSize()
        return Dimension(size.width, ConfigPanel.HEIGHT_INACTIVE)
    }
}

/**
 * Icon and label text.
 */
class Title(val active: Boolean) : JPanel() {

    init {
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        UISettings.setupAntialiasing(g)
        super.paintComponent(g)
        val g2d = g as Graphics2D

        ConfigPanel.ICON.paintIcon(this, g2d, ConfigPanel.PAD, 0)
        g2d.font = JBUI.CurrentTheme.ToolWindow.headerFont()
        g2d.drawString(ConfigPanel.TITLE, ConfigPanel.PAD + 20, JBUI.CurrentTheme.ToolWindow.tabVerticalPadding() + ConfigPanel.PAD)

        g2d.color = JBColor.border()
        if (active) {
            val borderY = (ConfigPanel.HEIGHT_ACTIVE - 1).toDouble()
            LinePainter2D.paint(g2d, 0.toDouble(), borderY, width.toDouble(), borderY)
        }
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(ConfigPanel.TITLE_WIDTH, if (active) ConfigPanel.HEIGHT_ACTIVE else (ConfigPanel.HEIGHT_INACTIVE))
    }
}



