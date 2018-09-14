package com.centurylink.mdw.studio.config

import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.draw.model.Data
import com.centurylink.mdw.draw.Drawable
import com.centurylink.mdw.draw.edit.*
import com.centurylink.mdw.draw.ext.JsonObject
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.google.gson.JsonObject
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel

class ConfigPanel(val projectSetup: ProjectSetup) :
        JPanel(BorderLayout()), SelectListener, UpdateListeners by UpdateListenersDelegate() {

    val titleBar = TitleBar("")
    private var tabPanel: TabPanel? = null
    private var blankPanel: JPanel? = null
    var hideShowListener: HideShowListener? = null

    init {
    }

    override fun onSelect(selectObjs: List<Drawable>, activate: Boolean) {
        tabPanel?.let { remove(it) }
        blankPanel?.let { remove(it) }

        if (selectObjs.size == 1) {
            val selectObj = selectObjs[0]
            titleBar.itemLabel.text = selectObj.workflowObj.name.lines().joinToString(" ")
            val configTabsJson = ALL_TABS_JSON.get(selectObj.workflowObj.type.toString()).asJsonObject
            titleBar.helpLink = findHelpLink(selectObj.workflowObj, configTabsJson)
            tabPanel = TabPanel(projectSetup, configTabsJson, selectObj.workflowObj)
            tabPanel?.addUpdateListener { obj ->
                notifyUpdateListeners(obj)
            }
            add(tabPanel, BorderLayout.CENTER)
        }
        else {
            titleBar.itemLabel.text = ""
            blankPanel = JPanel()
            add(blankPanel, BorderLayout.CENTER)
        }

        if (activate) {
            hideShowListener?.let {
                it.onHideShow(true)
            }
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
        const val TITLE = "Configurator"
        val ICON = IconLoader.getIcon("/icons/config.gif")
        val ICON_HIDE: Icon = AllIcons.General.HideToolWindow
        val ICON_HELP: Icon = AllIcons.General.Help_small
        val ALL_TABS_JSON = JsonObject(Templates.get("configurator/config-tabs.json"))
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

    val title = Title()
    val itemLabel: JLabel
    private val iconPanel: JPanel
    private var helpLabel: JLabel
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
                        BrowserUtil.browse(Data.HELP_LINK_URL + "/" + it.url)
                    }
                }
                helpLabel.addMouseListener(helpListener)
            }
            iconPanel.invalidate()
            iconPanel.repaint()
        }

    init {
        background = JBUI.CurrentTheme.ToolWindow.headerBackground(true)
        border = BorderFactory.createMatteBorder(1, 0, 1, 0, JBColor.border())

        add(title, BorderLayout.WEST)
        title.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                title.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
            }
        })

        itemLabel = JLabel(processName)
        itemLabel.horizontalAlignment = JLabel.CENTER
        add(itemLabel, BorderLayout.CENTER)

        iconPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 1))
        iconPanel.isOpaque = false
        iconPanel.border = BorderFactory.createEmptyBorder(0, 0, 0, 3)

        val iconEmptyBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1)
        val iconHoverBorder = BorderFactory.createLineBorder(JBColor.border())
        val iconCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        val iconSize = Dimension(24, 22)

        helpLabel = JLabel(ConfigPanel.ICON_HELP)
        helpLabel.isEnabled = false
        helpLabel.border = iconEmptyBorder
        helpLabel.cursor = iconCursor
        helpLabel.preferredSize = iconSize
        helpLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                helpLabel.isOpaque = true
                helpLabel.border = iconHoverBorder
            }
            override fun mouseExited(e: MouseEvent?) {
                helpLabel.isOpaque = false
                helpLabel.border = iconEmptyBorder
            }
        })
        iconPanel.add(helpLabel)

        hideLabel = JLabel(ConfigPanel.ICON_HIDE)
        hideLabel.border = iconEmptyBorder
        hideLabel.cursor = iconCursor
        hideLabel.toolTipText = "Hide"
        hideLabel.preferredSize = iconSize
        hideLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                hideShowListener?.let {
                    hideLabel.isOpaque = false
                    hideLabel.border = iconEmptyBorder
                    it.onHideShow(false)
                }
            }
            override fun mouseEntered(e: MouseEvent?) {
                hideLabel.isOpaque = true
                hideLabel.border = iconHoverBorder
            }
            override fun mouseExited(e: MouseEvent?) {
                hideLabel.isOpaque = false
                hideLabel.border = iconEmptyBorder
            }
        })
        iconPanel.add(hideLabel)

        add(iconPanel, BorderLayout.EAST)
    }
}

class PanelBar: JPanel(BorderLayout()) {

    var hideShowListener: HideShowListener? = null
    val titlePanel: JPanel

    init {
        background = JBUI.CurrentTheme.ToolWindow.headerBackground(false)
        border = BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border())

        titlePanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        titlePanel.border = BorderFactory.createEmptyBorder(0, 2, 1, 5)
        titlePanel.add(Title())
        titlePanel.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                hideShowListener?.let {
                    it.onHideShow(true)
                }
            }
            override fun mouseEntered(e: MouseEvent) {
                titlePanel.background = JBUI.CurrentTheme.ToolWindow.tabHoveredBackground()
            }
            override fun mouseExited(e: MouseEvent) {
                titlePanel.background = JBUI.CurrentTheme.ToolWindow.headerBackground(false)
            }
        })
        add(titlePanel, BorderLayout.WEST)
    }
}

/**
 * Icon and label text.
 */
class Title() : JPanel(FlowLayout(FlowLayout.LEFT, 5, 2)) {
    init {
        isOpaque = false
        val iconLabel = JLabel(ConfigPanel.ICON)
        iconLabel.border = BorderFactory.createEmptyBorder(3, 0, 0, 0)
        add(iconLabel)
        add(JLabel(ConfigPanel.TITLE))
    }
}



