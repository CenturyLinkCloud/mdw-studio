package com.centurylink.mdw.studio.draw

import java.awt.Color
import java.awt.Graphics2D

class Anchor(private val g2d: Graphics2D, val owner: Drawable, val pos: Int, x: Int, y: Int)
    : Shape(g2d, Display(x - ANCHOR_W, y - ANCHOR_W, ANCHOR_W * 2, ANCHOR_W * 2)) {

    override val workflowObj = owner.workflowObj

    override fun draw(): Display {
        g2d.color = ANCHOR_COLOR
        g2d.fillRect(display.x, display.y, display.w, display.h)
        g2d.color = Display.DEFAULT_COLOR
        return display
    }

    companion object {
        var ANCHOR_COLOR = Color(0xec407a)
        const val ANCHOR_W = 3
        const val ANCHOR_HIT_W = 8
    }

}