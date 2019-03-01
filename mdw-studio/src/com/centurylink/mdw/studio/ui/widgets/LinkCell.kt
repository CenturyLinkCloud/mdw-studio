package com.centurylink.mdw.studio.ui.widgets

import com.intellij.ide.BrowserUtil
import java.awt.Component
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

class LinkCell(val label: String, url: String) : LinkLabel(label) {

    init {
        alignmentX = Component.LEFT_ALIGNMENT
        clickListener = { BrowserUtil.browse(url) }
    }

    fun onHover(x: Int, y: Int): Boolean {
        if (x > AssetCell.HGAP && y > AssetCell.VGAP) {
            val fontMetrics = getFontMetrics(font)
            if (x < AssetCell.HGAP + fontMetrics.stringWidth(label) &&
                    y < AssetCell.VGAP + fontMetrics.height) {
                return true
            }
        }
        return false
    }
}

class LinkCellRenderer : TableCellRenderer {

    var linkCell: LinkCell? = null

    override fun getTableCellRendererComponent(table: JTable, value: Any?,
            isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        val linkCell = LinkCell(value?.toString() ?: "", "http://www.google.com")
        this.linkCell = linkCell
        return linkCell
    }
}

