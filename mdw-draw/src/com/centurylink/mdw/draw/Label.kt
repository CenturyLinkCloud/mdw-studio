package com.centurylink.mdw.draw

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D

class Label(private val g2d: Graphics2D, override var display: Display, val text: String, val owner: Drawable, val font: Font = Display.DEFAULT_FONT)
    : Shape(g2d, display), Drawable {

    override val workflowObj = owner.workflowObj

    override fun draw(): Display {
        return draw(Display.DEFAULT_COLOR)
    }

    fun draw(color: Color): Display {
        g2d.font = font
        display.w = g2d.fontMetrics.stringWidth(text) + PAD * 2
        display.h = g2d.fontMetrics.height + PAD * 2
        drawText(text, display.x + PAD, display.y + g2d.fontMetrics.ascent + PAD, font, color)
        g2d.font = Display.DEFAULT_FONT
        return display
    }

    override fun select() {
        var x = display.x - SEL_PAD
        var y = display.y + SEL_ADJ
        var w = display.w + SEL_PAD + SEL_ADJ
        var h = display.h - SEL_ADJ
        drawRect(x, y, w, h, SEL_COLOR, radius = SEL_ROUNDING_RADIUS)
        g2d.color = Display.DEFAULT_COLOR
    }

    override fun move(deltaX: Int, deltaY: Int, limits: Display?) {
        display = Display(display.x + deltaX, display.y + deltaY, display.w, display.h)
    }

    companion object {
        const val PAD = 2
        const val SEL_PAD = 2
        const val SEL_ADJ = 2
        const val SEL_ROUNDING_RADIUS = 4
        val SEL_COLOR = Color(0xe91e63)
    }
}