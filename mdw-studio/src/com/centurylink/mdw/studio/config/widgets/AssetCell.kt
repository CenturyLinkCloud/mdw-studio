package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.draw.edit.source
import com.centurylink.mdw.model.asset.Asset
import com.centurylink.mdw.model.asset.AssetVersionSpec
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
        callback: AssetSelectCallback? = null) : JPanel(FlowLayout(FlowLayout.LEFT, HGAP, VGAP)) {

    val assetLink = AssetLink(assetPath, projectSetup)

    /**
     * Coords are relative to cell origin.
     * Returns whether the pointer cursor should be displayed.
     */
    fun onHover(x: Int, y: Int): Boolean {
        if (x > HGAP && y > VGAP) {
            val fontMetrics = assetLink.getFontMetrics(assetLink.font)
            if (x < HGAP + fontMetrics.stringWidth(assetLink.assetName) &&
                    y < VGAP + fontMetrics.height) {
                return true
            }
        }
        return false
    }

    init {
        background = UIManager.getColor("EditorPane.background")

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
                assetLink.linkColor = "white"
            }
            else {
                foreground = table.foreground
                background = table.background
                assetLink.linkColor = null
            }

            if (hasFocus) {
                border = UIManager.getBorder("Table.focusCellHighlightBorder")
            }
            else {
                border = EmptyBorder(1, 1, 1, 1)
            }
        }
    }

    companion object {
        const val HGAP = 3
        const val VGAP = 3
    }
}

class AssetCellRenderer(val isReadonly: Boolean, val projectSetup: ProjectSetup) : DefaultTableCellRenderer() {

    var assetCell : AssetCell? = null

    override fun getTableCellRendererComponent(table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean,
            row: Int, column: Int): Component {
        val assetCell = AssetCell(value.toString(), isReadonly, projectSetup)
        assetCell.init(table, isSelected, hasFocus)
        this.assetCell = assetCell
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
            if (source == "proc" && !assetPath.isBlank() && !assetPath.endsWith(".proc")) {
                assetPath += ".proc"
            }
            if (table.model.columnCount > column + 1) {
                if (assetPath.isBlank()) {
                    table.model.setValueAt("", row, column + 1)
                }
                else {
                    val asset = projectSetup.getAsset(assetPath)
                    if (asset != null) {
                        // auto-set smart version
                        val ver = AssetVersionSpec.getDefaultSmartVersionSpec(Asset.formatVersion(asset.version))
                        table.model.setValueAt(ver, row, column + 1)
                    }
                }
            }
            fireEditingStopped()
        }
    }

    override fun getCellEditorValue(): Any? {
        return assetPath
    }
}