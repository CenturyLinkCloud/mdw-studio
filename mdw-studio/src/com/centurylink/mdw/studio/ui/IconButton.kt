package com.centurylink.mdw.studio.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.border.Border

open class IconButton(icon: Icon, tooltip: String, clickListener: (() -> Unit)? = null) : JLabel(icon) {

    private val iconCursor: Cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)

    private val iconSize: Dimension
        get() = Dimension(24, 22)

    private val iconHoverBorder: Border
        get() {
            return if (UIUtil.isUnderDarcula()) {
                BorderFactory.createLineBorder(JBColor.GRAY)
            }
            else {
                BorderFactory.createLineBorder(JBColor.border())
            }
        }

    private val iconEmptyBorder: Border
        get() = BorderFactory.createEmptyBorder(1, 1, 1, 1)

    init {
        border = iconEmptyBorder
        cursor = iconCursor
        toolTipText = tooltip
        preferredSize = iconSize
        addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                clickListener?.invoke()
            }
            override fun mouseEntered(e: MouseEvent?) {
                isOpaque = true
                border = iconHoverBorder
            }
            override fun mouseExited(e: MouseEvent?) {
                isOpaque = false
                border = iconEmptyBorder
            }
        })
    }
}

interface IconClickListener {
    fun onClick()
}
