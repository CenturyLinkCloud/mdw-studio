package com.centurylink.mdw.draw

import com.centurylink.mdw.draw.edit.Select
import com.centurylink.mdw.draw.edit.Selectable
import java.awt.*
import javax.swing.ImageIcon

abstract class Shape(private val g2d: Graphics2D, open var display: Display): Drawable, Selectable by Select() {

    val anchors: Map<Int,Anchor>
        get() {
            return mapOf(
                0 to Anchor(g2d, this, 0, display.x, display.y),
                1 to Anchor(g2d, this, 1, display.x + display.w, display.y),
                2 to Anchor(g2d, this, 2, display.x + display.w, display.y + display.h),
                3 to Anchor(g2d, this, 3, display.x, display.y + display.h)
            )
        }

    fun drawRect(x: Int = display.x, y: Int = display.y, w: Int = display.w, h: Int = display.h,
            border: Color = Display.OUTLINE_COLOR, fill: Color? = null, radius: Int? = Display.ROUNDING_RADIUS) {

        g2d.color = border

        if (radius != null) {
            g2d.drawRoundRect(x, y, w, h, radius, radius)
            if (fill != null) {
                g2d.paint = fill
                g2d.fillRoundRect(x, y, w, h, radius, radius)
            }
        }
        else {
            g2d.drawRect(x, y, w, h)
            if (fill != null) {
                g2d.paint = fill
                g2d.fillRect(x, y, w, h)
            }
        }

        g2d.color = Display.DEFAULT_COLOR
        g2d.paint = Display.DEFAULT_COLOR
    }

    fun drawIcon(icon: ImageIcon, x: Int, y: Int, opacity: Float = 1f) {
        drawImage(icon.image, x, y, opacity)
    }

    fun drawImage(image: Image, x: Int, y: Int, opacity: Float = 1f) {
        val origComposite = g2d.composite
        if (opacity != 1f) {
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity)
        }
        g2d.drawImage(image, x, y, null)
        g2d.composite = origComposite
    }

    fun drawOval(x: Int = display.x, y: Int = display.y, w: Int = display.w, h: Int = display.h,
            fill: Color? = null, g2d: Graphics2D = this.g2d) {

        if (fill != null) {
            g2d.color = Display.OUTLINE_COLOR
            g2d.fillOval(x, y, w, h)
            g2d.paint = fill
            g2d.fillOval(x + 1, y + 1, w - 2, h - 2)
        } else {
            g2d.color = Display.OUTLINE_COLOR
            g2d.drawOval(x, y, w, h)
        }

        g2d.paint = Display.DEFAULT_COLOR
    }

    fun drawDiamond(x: Int = display.x, y: Int = display.y, w: Int = display.w, h: Int = display.h,
            border: Color = Display.OUTLINE_COLOR) {

        val x1 = x + w / 2
        val y1 = y + h / 2
        g2d.color = border
        g2d.drawLine(x, y1, x1, y)
        g2d.drawLine(x1, y, x + w, y1)
        g2d.drawLine(x + w, y1, x1, y + h)
        g2d.drawLine(x1, y + h, x, y1)
        g2d.color = Display.DEFAULT_COLOR
    }

    fun drawText(text: String, x: Int = display.x, y: Int = display.y,
            font: Font = Display.DEFAULT_FONT, color: Color = Display.DEFAULT_COLOR) {

        g2d.font = font
        g2d.color = color
        g2d.drawString(text, x, y)
        g2d.font = Display.DEFAULT_FONT
        g2d.color = Display.DEFAULT_COLOR
    }

    fun clearRect(x: Int = display.x, y: Int = display.y, w: Int = display.w, h: Int = display.h) {
        drawRect(x, y, w, h, Display.BACKGROUND_COLOR, Display.BACKGROUND_COLOR)
    }

    override fun isHover(x: Int, y: Int): Boolean {
        if (x >= display.x && x <= display.x + display.w && y >= display.y && y <= display.y + display.h) {
            return true
        }
        else if (isSelected) {
            // account for anchors as well
            return getAnchor(x, y) != null
        }
        return false
    }

    override fun getAnchor(x: Int, y: Int): Int? {
        for ((i, anchor) in anchors) {
            if (anchor.isHover(x, y))
                return i
        }
        return null
    }

    override fun select() {
        for (anchor in anchors.values) {
            anchor.draw()
        }
        isSelected = true
    }

    fun resizeDisplay(anchor: Int, x: Int, y: Int, deltaX: Int, deltaY: Int, min: Int, limits: Display? = null): Display {
        val display = Display(this.display.x, this.display.y, this.display.w, this.display.h)
        val t1: Int
        val t2: Int
        if (anchor == 0) {
            t1 = display.x + display.w
            t2 = display.y + display.h
            display.x = x + deltaX
            display.y = y + deltaY
            if (t1 - display.x < min) {
                display.x = t1 - min
            }
            if (t2 - display.y < min) {
                display.y = t2 - min
            }
            display.w = t1 - display.x
            display.h = t2 - display.y
        }
        else if (anchor == 1) {
            t2 = display.y + display.h
            display.y = y + deltaY
            if (t2 - display.y < min) {
                display.y = t2 - min
            }
            display.w = x - (display.x - deltaX)
            if (display.w < min) {
                display.w = min
            }
            display.h = t2 - display.y
        }
        else if (anchor == 2) {
            display.w = x - (display.x - deltaX)
            display.h = y - (display.y - deltaY)
            if (display.w < min) {
                display.w = min
            }
            if (display.h < min) {
                display.h = min
            }
        }
        else if (anchor == 3) {
            t1 = display.x + display.w
            display.x = x + deltaX
            if (t1 - display.x < min) {
                display.x = t1 - min
            }
            display.w = t1 - display.x
            display.h = y - (display.y - deltaY)
            if (display.h < min) {
                display.h = min
            }
        }

        limits?.let {
            display.limit(it)
        }

        return display
    }

    companion object {
        const val MIN_SIZE = 4
    }
}