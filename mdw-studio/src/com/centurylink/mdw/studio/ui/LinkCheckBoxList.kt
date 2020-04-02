package com.centurylink.mdw.studio.ui

import com.intellij.ui.CheckBoxList
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.EmptyBorder

interface LinkListListener {
    fun itemLinkClicked(index: Int)
}

class LinkCheckBoxList: CheckBoxList<String>() {

    private val linkCellRenderer = LinkCellRenderer(this)
    var checkboxWidth = 0
    var linkListener: LinkListListener? = null

    init {
        cellRenderer = linkCellRenderer
        addMouseMotionListener(object: MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                cursor = if (getHoverItem(e.x, e.y) >= 0) {
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                } else {
                    Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                }
            }
        })
        addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                linkListener?.let { listener ->
                    val itemIndex = getHoverItem(e.x, e.y)
                    if (itemIndex >= 0) {
                        listener.itemLinkClicked(itemIndex)
                    }
                }
            }
        })
    }

    public override fun getForeground(isSelected: Boolean): Color {
        return super.getForeground(isSelected)
    }

    public override fun getBackground(isSelected: Boolean): Color {
        return super.getBackground(isSelected)
    }

    public override fun isEnabled(index: Int): Boolean {
        return super.isEnabled(index)
    }

    fun getHoverItem(x: Int, y: Int): Int {
        val fontMetrics = getFontMetrics(font)
        for (i in 0 until itemsCount) {
            val cellBounds = getCellBounds(i, i)
            val hoverX = x - cellBounds.x
            val hoverY = y - cellBounds.y
            if (hoverX > HGAP + checkboxWidth && hoverY > VGAP) {
                getItemAt(i)?.let{text ->
                    if (hoverX < HGAP + checkboxWidth + fontMetrics.stringWidth(text) &&
                            hoverY < VGAP + fontMetrics.height) {
                        return i
                    }
                }
            }
        }
        return -1
    }

    companion object {
        const val HGAP = 5
        const val VGAP = 1
    }

}

class LinkCellRenderer(private val checkboxList: LinkCheckBoxList) : ListCellRenderer<JCheckBox> {

    private val selectedBorder: Border = UIManager.getBorder("List.focusCellHighlightBorder")
    private val cellBorder: Border = EmptyBorder(selectedBorder.getBorderInsets(JCheckBox()))

    override fun getListCellRendererComponent(list: JList<out JCheckBox>, checkbox: JCheckBox, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component? {
        val textColor: Color = checkboxList.getForeground(isSelected)
        val backgroundColor: Color = checkboxList.getBackground(isSelected)
        val font: Font = checkboxList.font
        checkbox.background = backgroundColor
        checkbox.foreground = textColor
        checkbox.isEnabled = checkboxList.isEnabled && checkboxList.isEnabled(index)
        checkbox.font = font
        checkbox.isFocusPainted = false
        checkbox.isBorderPainted = false
        checkbox.isOpaque = true
        checkbox.text = null

        if (checkbox.width > 0) checkboxList.checkboxWidth = checkbox.width

        val linkText = checkboxList.getItemAt(index)
        var rootComponent: JComponent = if (linkText != null) {
            val panel = JPanel(BorderLayout(LinkCheckBoxList.HGAP, LinkCheckBoxList.VGAP))
            panel.add(checkbox, BorderLayout.LINE_START)
            val linkLabel = JLabel(linkText, SwingConstants.LEFT)
            linkLabel.alignmentX = Component.LEFT_ALIGNMENT
            linkLabel.border = JBUI.Borders.emptyRight(checkbox.insets.left)
            linkLabel.font = font
            panel.add(linkLabel, BorderLayout.CENTER)
            panel.background = backgroundColor
            linkLabel.foreground = textColor
            linkLabel.background = backgroundColor
            panel
        } else {
            checkbox
        }
        rootComponent.border = if (isSelected) selectedBorder else cellBorder
        return rootComponent
    }
}

