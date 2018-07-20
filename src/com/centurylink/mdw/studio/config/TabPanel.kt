package com.centurylink.mdw.studio.config

import com.centurylink.mdw.studio.edit.UpdateListeners
import com.centurylink.mdw.studio.edit.UpdateListenersDelegate
import com.centurylink.mdw.studio.edit.WorkflowObj
import com.centurylink.mdw.studio.ext.contains
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.UIManager
import javax.swing.border.EmptyBorder


class TabPanel(val projectSetup: ProjectSetup, configTabsJson: JsonObject, val workflowObj: WorkflowObj) :
        JPanel(BorderLayout()), TabSelectListener, UpdateListeners by UpdateListenersDelegate() {

    private val tabPane: TabPane
    private val contentPane: ContentPane
    private var tabComponent: Component? = null
    var tabSelectListener: TabSelectListener? = null

    init {
        background = JBColor.background()
        tabPane = TabPane(projectSetup, configTabsJson, workflowObj)
        tabPane.tabSelectListener = this
        add(tabPane, BorderLayout.WEST)

        contentPane = ContentPane()
        add(contentPane)

        if (tabPane.configTabs.isEmpty()) {
            currentTab = null
        }
        else {
            if (tabPane.configTabs.keys.contains(currentTab)) {
                tabPane.selectTab(currentTab)
            }
            else {
                currentTab = tabPane.configTabs.keys.iterator().next()
                tabPane.selectTab(currentTab)
            }
        }
    }

    override fun onTabSelect(tabName: String, tabJson: JsonElement) {
        tabComponent?.let {
            contentPane.remove(it)
            tabComponent = null
        }
        currentTab = tabName
        if (tabJson.isJsonObject) {
            val tabTemplate = getTabTemplate(projectSetup, tabJson.asJsonObject, workflowObj)
            tabTemplate?.pagelet?.let {
                val configTab = ConfigTab(tabName, tabTemplate, workflowObj)
                tabComponent = configTab
                configTab.addUpdateListener { obj ->
                    notifyUpdateListeners(obj)
                }
                contentPane.add(tabComponent)
            }
        }
        else if (tabJson.isJsonPrimitive) {
            val prop = tabJson.asJsonPrimitive.asString
            if (prop == "source") {
                tabComponent = TextTab(workflowObj.obj.toString(2))
            }
            else {
                tabComponent = TextTab(workflowObj.toString(prop))
            }
            contentPane.add(tabComponent)
        }
        contentPane.invalidate()
        contentPane.repaint()
        tabSelectListener?.let { listener ->
            listener.onTabSelect(tabName, tabJson)
        }
    }

    companion object {
        const val TAB_WIDTH = 150
        const val TAB_HEIGHT = 24
        const val CONTENT_PAD_X = 8
        const val CONTENT_PAD_Y = 6
        val TAB_BORDER = JBColor.border()
        var currentTab: String? = null
    }
}

class TabPane(val projectSetup: ProjectSetup, configTabsJson: JsonObject, val workflowObj: WorkflowObj) :
        JPanel(BorderLayout()) {

    var tabsPanel: JPanel? = null
    val configTabs = linkedMapOf<String,JsonElement>()
    var selectedTab: String? = null
    var tabSelectListener: TabSelectListener? = null

    init {
        // configTabs filtering
        for (tabName in configTabsJson.keySet()) {
            val tab = configTabsJson.get(tabName)
            var include = true
            if (tab.isJsonObject) {
                val tabJson = tab.asJsonObject
                val categories = tabJson.get("_categories")
                if (categories != null) {
                    val tabTemplate = getTabTemplate(projectSetup, tabJson, workflowObj)
                    include = tabTemplate != null && categories.asJsonArray.contains(tabTemplate.category)
                }
            }
            if (include) {
                configTabs.put(tabName, tab)
            }
        }
    }

    fun selectTab(tab: String?) {
        selectedTab = tab

        if (tabsPanel != null) {
            remove(tabsPanel)
        }

        tabsPanel = JPanel(GridBagLayout())
        if (UIUtil.isUnderDarcula()) {
            background = Color.DARK_GRAY
            tabsPanel?.background = Color.DARK_GRAY
        }
        add(tabsPanel, BorderLayout.NORTH)
        val constraints = GridBagConstraints()
        constraints.weightx = 1.0
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.gridwidth = GridBagConstraints.REMAINDER
        constraints.anchor = GridBagConstraints.NORTH

        val selColor = if (UIUtil.isUnderDarcula()) Color.WHITE else Color(0x337ab7)
        val unselColor = JLabel().foreground

        for (tabName in configTabs.keys) {
            val tabLabel = JLabel("  " + tabName)
            tabLabel.preferredSize = Dimension(TabPanel.TAB_WIDTH, TabPanel.TAB_HEIGHT)
            tabLabel.minimumSize = Dimension(TabPanel.TAB_WIDTH, TabPanel.TAB_HEIGHT)
            val selected = tabName == selectedTab
            if (selected) {
                tabLabel.isOpaque = true
                tabLabel.background = UIManager.getColor("EditorPane.background")
            }
            tabLabel.border = BorderFactory.createMatteBorder(0, 1, 1,
                    if (selected) 0 else 1, TabPanel.TAB_BORDER)

            if (!selected) {
                tabLabel.addMouseListener(object : MouseAdapter() {
                    override fun mousePressed(e: MouseEvent) {
                        selectTab(tabName)
                        revalidate()
                            repaint()
                    }
                    override fun mouseEntered(e: MouseEvent) {
                        tabLabel.foreground = selColor
                    }
                    override fun mouseExited(e: MouseEvent) {
                        tabLabel.foreground = unselColor
                    }
                })
            }
            tabsPanel?.let {
                it.add(tabLabel, constraints)
            }
        }

        tabSelectListener?.let { listener ->
            configTabs[selectedTab]?.let {
                listener.onTabSelect(selectedTab!!, it)
            }
        }
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(TabPanel.TAB_WIDTH, super.getPreferredSize().height)
    }
}


interface TabSelectListener {
    fun onTabSelect(tabName: String, tabJson: JsonElement)
}

class ContentPane() : JPanel(BorderLayout()) {
    init {
        background = UIManager.getColor("EditorPane.background")
        border = EmptyBorder(TabPanel.CONTENT_PAD_Y, TabPanel.CONTENT_PAD_X, TabPanel.CONTENT_PAD_Y,
                TabPanel.CONTENT_PAD_X)
    }
}