package com.centurylink.mdw.studio.draw

import com.centurylink.mdw.studio.file.Icons
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font

class Display {

    var x = 0
    var y = 0
    var w = 0
    var h = 0

    constructor(attr: String?) {
        if (attr != null) {
            for (dim in attr.split(",")) {
                if (dim.startsWith("x=")) {
                    x = dim.substring(2).toInt()
                } else if (dim.startsWith("y=")) {
                    y = dim.substring(2).toInt()
                } else if (dim.startsWith("w=")) {
                    w = dim.substring(2).toInt()
                } else if (dim.startsWith("h=")) {
                    h = dim.substring(2).toInt()
                }
            }
        }
    }

    constructor(x: Int, y: Int, w: Int = 0, h: Int = 0) {
        this.x = x
        this.y = y
        this.w = w
        this.h = h
    }

    constructor(display: Display) {
        this.x = display.x
        this.y = display.y
        this.w = display.w
        this.h = display.h
    }

    fun limit(limits: Display) {
        if (x < limits.x) {
            x = limits.x
        }
        if (x + w > limits.x + limits.w) {
            w = limits.x + limits.w - x
        }
        if (y < limits.y) {
            y = limits.y
        }
        if (y + h > limits.y + limits.h) {
            h = limits.y + limits.h - y
        }
    }

    fun contains(inner: Display): Boolean {
        return x <= inner.x && y <= inner.y && x + w >= inner.x + inner.w && y + h >= inner.y + inner.h
    }

    override fun toString(): String {
        var attr = "x=$x,y=$y"
        if (w > 0) {
            attr += ",w=$w,h=$h"
        }
        return attr
    }

    companion object {
        val DEFAULT_FONT = Font("SansSerif", Font.PLAIN, 12)
        val TITLE_FONT = Font("SansSerif", Font.BOLD, 18)

        const val MAC_FONT_SIZE = 12 // TODO: what's this for?

        var DEFAULT_COLOR = Color.BLACK
        var GRID_COLOR = Color.LIGHT_GRAY
        var OUTLINE_COLOR = Color.BLACK
        var SHADOW_COLOR = Color(0, 0, 0, 50)
        var META_COLOR = Color.GRAY
        var BACKGROUND_COLOR = Color.WHITE

        val START_COLOR = Color(0x98fb98)
        val STOP_COLOR = Color(0xff8c86)

        val START_ICON = Icons.readIcon("/icons/start.png")
        val STOP_ICON = Icons.readIcon("/icons/stop.png")

        var SHAPE_TEXT_COLOR = Color.BLACK

        val DEFAULT_STROKE = BasicStroke()
        val LINE_STROKE = BasicStroke(3.0f)
        val GRID_STROKE = BasicStroke(0.2f)

        const val GRID_SIZE = 10
        const val ROUNDING_RADIUS = 12

        const val MIN_DRAG = 3

        const val ICON_WIDTH = 24
        const val ICON_HEIGHT = 24
        const val ICON_PAD = 8
    }
}