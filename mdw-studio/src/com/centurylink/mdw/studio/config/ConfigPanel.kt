package com.centurylink.mdw.studio.config

import com.centurylink.mdw.activity.types.AdapterActivity
import com.centurylink.mdw.activity.types.TaskActivity
import com.centurylink.mdw.annotations.Monitor
import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.draw.Drawable
import com.centurylink.mdw.draw.edit.*
import com.centurylink.mdw.draw.ext.JsonObject
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.model.asset.PrePostWidgetProvider
import com.centurylink.mdw.model.project.Data
import com.centurylink.mdw.monitor.ActivityMonitor
import com.centurylink.mdw.monitor.AdapterMonitor
import com.centurylink.mdw.monitor.ProcessMonitor
import com.centurylink.mdw.monitor.TaskMonitor
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.centurylink.mdw.studio.ui.IconButton
import com.google.gson.JsonObject
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.json.JSONArray
import org.json.JSONObject
import java.awt.BorderLayout
import java.awt.Cursor
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
            hideShowListener?.onHideShow(true)
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
        val ICON_HELP: Icon = AllIcons.General.ContextHelp
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
            val template = Template(JsonObject(implementor.json.toString()), implementor.category)
            template.pagelet.addWidgetProvider(PrePostWidgetProvider())
            if (!DumbService.getInstance(projectSetup.project).isDumb) {
                template.pagelet.addWidgetProvider { implCategory ->
                    val monitoringWidget = Widget(JSONObject(Templates.get("configurator/monitors.json")))
                    val widgets = listOf(monitoringWidget)
                    val rows = JSONArray()
                    for ((asset, psiAnnotations) in projectSetup.findAnnotatedAssets(Monitor::class)) {
                        for (psiAnnotation in psiAnnotations) {
                            val monitorAnnotation = MonitorAnnotation(psiAnnotation, asset)
                            val applicable = when (monitorAnnotation.category) {
                                ActivityMonitor::class.qualifiedName -> true
                                AdapterMonitor::class.qualifiedName -> implCategory == AdapterActivity::class.qualifiedName
                                TaskMonitor::class.qualifiedName -> implCategory == TaskActivity::class.qualifiedName
                                else -> false
                            }
                            if (applicable) {
                                rows.put(monitorAnnotation.defaultAttributeValue)
                            }
                        }
                    }
                    if (rows.length() > 0) {
                        monitoringWidget.setAttribute("default", rows.toString())
                    }
                    widgets
                }
            }
            return template
        }
    }
    else {
        val content = Templates.get("configurator/" + templ)
        content?.let {
            val template = Template(JsonObject(content))
            if (templ == "processMonitoring.json") {
                if (!DumbService.getInstance(projectSetup.project).isDumb) {
                    template.pagelet.addWidgetProvider { _ ->
                        val monitoringWidget = Widget(JSONObject(Templates.get("configurator/monitors.json")))
                        val widgets = listOf(monitoringWidget)
                        val rows = JSONArray()
                        for ((asset, psiAnnotations) in projectSetup.findAnnotatedAssets(Monitor::class)) {
                            for (psiAnnotation in psiAnnotations) {
                                val monitorAnnotation = MonitorAnnotation(psiAnnotation, asset)
                                if (monitorAnnotation.category == ProcessMonitor::class.qualifiedName) {
                                    rows.put(monitorAnnotation.defaultAttributeValue)
                                }
                            }
                        }
                        if (rows.length() > 0) {
                            monitoringWidget.setAttribute("default", rows.toString())
                        }
                        widgets
                    }
                }
            }
            return template
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
    var helpLink: HelpLink?
        get() = null
        set(value) {
            helpLabel.isEnabled = value != null
            helpLabel.toolTipText = value?.tooltip
            helpListener?.let { helpLabel.removeMouseListener(it) }
            value?.let {
                helpListener = object : MouseAdapter() {
                    override fun mouseReleased(e: MouseEvent) {
                        BrowserUtil.browse(if (it.url.startsWith("http://") || it.url.startsWith("https://")) {
                            it.url
                        }
                        else {
                            Data.DOCS_URL + "/" + it.url
                        })
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

        helpLabel = IconButton(ConfigPanel.ICON_HELP, "Help")
        iconPanel.add(helpLabel)

        hideLabel = IconButton(ConfigPanel.ICON_HIDE, "Hide") {
            hideShowListener?.onHideShow(false)
        }
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
                hideShowListener?.onHideShow(true)
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



