package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.isReadonly
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.AbstractCellEditor
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor

class AssetCell(widget: Pagelet.Widget) : JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)) {

    init {
        val assetLink = AssetLink(widget)
        add(assetLink)

        if (!widget.isReadonly) {
            val selectButton = AssetSelectButton(widget, "...") {
                println("SELECTED: " + it)
            }
            selectButton.preferredSize = Dimension(30, 25)
            add(selectButton)
        }
    }
}

class AssetCellRenderer(val widget: Pagelet.Widget) : DefaultTableCellRenderer() {

    override fun getTableCellRendererComponent(table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean,
            row: Int, column: Int): Component {
        widget.value = table.getValueAt(row, column)
        val assetCell = AssetCell(widget)
        if (isSelected && !hasFocus) {
            val defaultComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            assetCell.background = defaultComponent.background
            assetCell.border = (defaultComponent as JComponent).border
        }
        return assetCell
    }
}

class AssetCellEditor(val widget: Pagelet.Widget) : AbstractCellEditor(), TableCellEditor {

    override fun getTableCellEditorComponent(table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
        return AssetCell(widget)
    }

    override fun getCellEditorValue(): Any? {
        return widget.value
    }
}