package com.centurylink.mdw.studio.ui.widgets

import com.intellij.ide.BrowserUtil
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.AbstractCellEditor
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.UIManager
import javax.swing.border.EmptyBorder
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

open class LinkCell(val label: String, url: String? = null) : JPanel(FlowLayout(FlowLayout.LEFT, HGAP, VGAP)) {

    private val linkLabel = LinkLabel(label)

    init {
        linkLabel.alignmentX = Component.LEFT_ALIGNMENT
        linkLabel.clickListener = {
            url?.let {
                BrowserUtil.browse(it)
            }
        }
        add(linkLabel)
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

    fun isHover(x: Int, y: Int): Boolean {
        if (x > HGAP && y > VGAP) {
            val fontMetrics = getFontMetrics(font)
            if (x < HGAP + fontMetrics.stringWidth(label) &&
                    y < VGAP + fontMetrics.height) {
                return true
            }
        }
        return false
    }

    companion object {
        const val HGAP = 3
        const val VGAP = 3
    }
}

class LinkCellRenderer(private val url: String?) : TableCellRenderer, Hoverable {

    var linkCell: LinkCell? = null

    override fun getTableCellRendererComponent(table: JTable, value: Any?,
            isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        val linkCell = LinkCell(value.toString(), url)
        linkCell.init(table, isSelected, hasFocus)
        this.linkCell = linkCell
        return linkCell
    }

    override fun isHover(x: Int, y: Int): Boolean {
        return linkCell?.isHover(x, y) == true
    }
}

class LinkCellEditor(private val url: String?) :  AbstractCellEditor(), TableCellEditor {

    var label = ""

    override fun getTableCellEditorComponent(table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int):
            Component {
        label = value.toString()
        return LinkCell(label, url)
    }

    override fun getCellEditorValue(): Any? {
        return label
    }
}

