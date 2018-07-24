package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.file.Icons
import com.centurylink.mdw.studio.ext.Json
import com.centurylink.mdw.studio.ext.toStrings
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import org.json.JSONArray
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel

@Suppress("unused")
class Picklist(widget: Pagelet.Widget) : SwingWidget(widget) {

    var unselected = mutableListOf<String>()
    var selected = mutableListOf<String>()

    val listDimension
        get() = Dimension(175, maxOf(100, 20 * (unselected.size + selected.size)))

    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(3, 3, 0, 0)

        widget.value?.let {
            selected.addAll((it as JSONArray).toStrings())
        }
        for (option in widget.options) {
            if (!selected.contains(option)) {
                unselected.add(option)
            }
        }

        val unselList = JBList(unselected)
        unselList.preferredSize = listDimension
        unselList.border = BorderFactory.createLineBorder(JBColor.border())
        unselList.disableEmptyText()

        val selList = JBList(selected)
        selList.preferredSize = listDimension
        selList.border = BorderFactory.createLineBorder(JBColor.border())
        selList.disableEmptyText()

        unselList.addListSelectionListener { _ ->
            selList.clearSelection()
        }

        selList.addListSelectionListener {
            unselList.clearSelection()
        }

        val btnPanel = JPanel(BorderLayout())
        btnPanel.isOpaque = false

        val addButton = JButton()
        addButton.icon = ADD_BTN_ICON
        addButton.preferredSize = BUTTON_DIMENSION
        addButton.isOpaque = false
        btnPanel.add(addButton, BorderLayout.NORTH)
        addButton.addActionListener {
            val items = unselList.selectedValuesList
            unselected.removeAll(items)
            selected.addAll(items)
            selected.sort()
            selList.setListData(selected.toTypedArray())
            unselList.setListData(unselected.toTypedArray())
            widget.value = Json.toJSONArray(selected)
            applyUpdate()
        }

        val remButton = JButton()
        remButton.icon = REM_BTN_ICON
        remButton.preferredSize = BUTTON_DIMENSION
        remButton.isOpaque = false
        btnPanel.add(remButton, BorderLayout.SOUTH)
        remButton.addActionListener {
            val items = selList.selectedValuesList
            selected.removeAll(items)
            unselected.addAll(items)
            unselected.sort()
            unselList.setListData(unselected.toTypedArray())
            selList.setListData(selected.toTypedArray())
            widget.value = Json.toJSONArray(selected)
            applyUpdate()
        }

        add(unselList)
        add(btnPanel)
        add(selList)
    }

    companion object {
        val BUTTON_DIMENSION = Dimension(30, 30)
        val ADD_BTN_ICON = Icons.readIcon("/icons/right.gif")
        val REM_BTN_ICON = Icons.readIcon("/icons/left.gif")

    }
}