package com.centurylink.mdw.studio.draw

import com.intellij.ui.JBColor
import java.awt.*
import java.awt.geom.Area
import java.awt.geom.RoundRectangle2D
import javax.swing.border.AbstractBorder

class RoundedBorder(private val color: Color = JBColor.border(), private val thickness: Int = 1,
        private val radii: Int = 5, private val strokePad: Int = thickness / 2) : AbstractBorder() {

    private val insets: Insets
    private val stroke = BasicStroke(thickness.toFloat())
    private val hints: RenderingHints = RenderingHints(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON)

    init {
        val pad = radii + strokePad
        insets = Insets(pad, pad, pad, pad)
    }

    override fun getBorderInsets(c: Component): Insets? {
        return insets
    }

    override fun getBorderInsets(c: Component, insets: Insets): Insets? {
        return getBorderInsets(c)
    }

    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int,
            width: Int, height: Int) {
        val g2d = g as Graphics2D

        val bottomLineY = height - thickness

        val bubble = RoundRectangle2D.Double(
                0 + strokePad.toDouble(),
                0 + strokePad.toDouble(),
                width - thickness.toDouble(),
                bottomLineY.toDouble(),
                radii.toDouble(),
                radii.toDouble())

        val area = Area(bubble)
        g2d.setRenderingHints(hints)

        // paint parent's background everywhere outside the rounded clip
        c.parent?.let {
            val rect = Rectangle(0, 0, width, height)
            val borderRegion = Area(rect)
            borderRegion.subtract(area)
            g2d.clip = borderRegion
            g2d.color = it.getBackground()
            g2d.fillRect(0, 0, width, height)
            g2d.clip = null
        }

        g2d.color = color
        g2d.stroke = stroke
        g2d.draw(area)
    }
}