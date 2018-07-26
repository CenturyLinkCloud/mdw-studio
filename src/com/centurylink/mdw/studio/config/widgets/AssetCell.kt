package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.studio.proj.ProjectSetup
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.AbstractCellEditor
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.UIManager
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor

class AssetCell(assetPath: String, isReadonly: Boolean, projectSetup: ProjectSetup, source: String? = null,
        callback: AssetSelectCallback? = null) : JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)) {

    val assetLink = AssetLink(assetPath, projectSetup)

    init {
        add(assetLink)

        if (!isReadonly) {
            val selectButton = AssetSelectButton("...", assetPath, projectSetup, source) { selectedAssetPath ->
                callback?.let {
                    callback(selectedAssetPath)
                }
            }
            selectButton.preferredSize = Dimension(30, 25)
            add(selectButton)
        }
    }

    fun init(table: JTable, isSelected: Boolean, hasFocus: Boolean) {
        if (isSelected && !hasFocus) {
            if (isSelected) {
                foreground = table.selectionForeground
                background = table.selectionBackground
            }
            else {
                foreground = table.foreground
                background = table.background
            }

            if (hasFocus) {
                border = UIManager.getBorder("Table.focusCellHighlightBorder")
            }
            else {
                border = EmptyBorder(1, 1, 1, 1)
            }
        }
    }
}

class AssetCellRenderer(val isReadonly: Boolean, val projectSetup: ProjectSetup) : DefaultTableCellRenderer() {

    override fun getTableCellRendererComponent(table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean,
            row: Int, column: Int): Component {
        val assetCell = AssetCell(value.toString(), isReadonly, projectSetup)
        assetCell.init(table, isSelected, hasFocus)
        return assetCell
    }
}

class AssetCellEditor(val isReadonly: Boolean, val projectSetup: ProjectSetup, val source: String?) :
        AbstractCellEditor(), TableCellEditor {

    var assetPath = ""

    override fun getTableCellEditorComponent(table: JTable, value: Any, isSelected: Boolean, row: Int, column: Int):
            Component {
        assetPath = value.toString()
        return AssetCell(assetPath, isReadonly, projectSetup, source) {
            assetPath = it ?: ""
            fireEditingStopped()
        }
    }

    override fun getCellEditorValue(): Any? {
        return assetPath
    }
}