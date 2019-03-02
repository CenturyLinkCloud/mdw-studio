package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.model.asset.Pagelet.Widget
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

class LinkCell(val label: String, url: String?, widget: Widget? = null) :
        JPanel(FlowLayout(FlowLayout.LEFT, HGAP, VGAP)) {

    val linkLabel = LinkLabel(label)

    init {
        linkLabel.alignmentX = Component.LEFT_ALIGNMENT
        linkLabel.clickListener = {
            val dialogWidget = widget
            if (dialogWidget == null) {
                url?.let {
                    BrowserUtil.browse(it)
                }
            }
            else {
                println("TODO: " + dialogWidget.name)
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

    /**
     * Coords are relative to cell origin.
     * Returns whether the pointer cursor should be displayed.
     */
    fun onHover(x: Int, y: Int): Boolean {
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

class LinkCellRenderer(private val url: String?, private val widget: Widget? = null) : TableCellRenderer {

    var linkCell: LinkCell? = null

    override fun getTableCellRendererComponent(table: JTable, value: Any?,
            isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        val linkCell = LinkCell(value.toString(), url, widget)
        linkCell.init(table, isSelected, hasFocus)
        this.linkCell = linkCell
        return linkCell
    }
}

class LinkCellEditor(private val url: String?, private val widget: Widget? = null) :  AbstractCellEditor(), TableCellEditor {

    var label = ""

    override fun getTableCellEditorComponent(table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int):
            Component {
        label = value.toString()
        return LinkCell(label, url, widget)
    }

    override fun getCellEditorValue(): Any? {
        return label
    }
}

