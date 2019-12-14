package com.centurylink.mdw.draw

import java.awt.Graphics2D
import kotlin.math.roundToInt

/**
 * Canvas grid.
 */
class Grid(private val g2d: Graphics2D, val display: Display, val snap: Int = 0) {

    fun draw() {
        g2d.color = Display.GRID_COLOR
        g2d.stroke = Display.GRID_STROKE
        var x = Display.GRID_SIZE
        while (x < display.w + Diagram.BOUNDARY_DIM) {
            g2d.drawLine(x, 0, x, display.h + Diagram.BOUNDARY_DIM)
            x += Display.GRID_SIZE
        }
        var y = Display.GRID_SIZE
        while (y < display.h + Diagram.BOUNDARY_DIM) {
            g2d.drawLine(0, y, display.w + Diagram.BOUNDARY_DIM, y)
            y += Display.GRID_SIZE
        }
        g2d.color = Display.DEFAULT_COLOR
        g2d.stroke = Display.DEFAULT_STROKE
    }

    /**
     * Snaps display to nearest grid lines.  If even, make sure display.w and display.h are divisible by two,
     * for centering link lines.
     */
    fun snap(display: Display, even: Boolean = false) {
        if (snap > 0) {
            display.x = (display.x.toDouble() / snap).roundToInt() * snap
            display.y = (display.y.toDouble() / snap).roundToInt() * snap
            if (even) {
                display.w = (display.w.toDouble() / (snap * 2)).roundToInt() * (snap * 2)
                display.h = (display.h.toDouble() / (snap * 2)).roundToInt() * (snap * 2)
            } else {
                display.w = (display.w.toDouble() / snap).roundToInt() * snap
                display.h = (display.h.toDouble() / snap).roundToInt() * snap
            }
        }
    }
}