package com.centurylink.mdw.studio.draw

import com.centurylink.mdw.constant.WorkAttributeConstant
import com.centurylink.mdw.model.workflow.Activity
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.studio.edit.WorkflowObj
import com.centurylink.mdw.studio.edit.WorkflowType
import com.centurylink.mdw.studio.proj.Implementor
import java.awt.Graphics2D

class Step(private val g2d: Graphics2D, project: Any?, process: Process, val activity: Activity,
        val implementor: Implementor, private val boxStyle: Boolean = true) :
        Shape(g2d, Display(activity.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO))), Drawable, Resizable {

    override val workflowObj = WorkflowObj(project, process, WorkflowType.activity, activity.json)

    override fun draw(): Display {

        var extents = Display(0, 0, display.x + display.w, display.y + display.h)

        var yAdjust = -3
        var textColor = Display.DEFAULT_COLOR

        if (implementor.icon != null) {
            if (boxStyle) {
                drawRect()
            }
            var iconX = display.x + display.w / 2 - 12
            var iconY = display.y + 5
            extents.w = maxOf(extents.w, iconX + implementor.icon!!.iconWidth)
            extents.h = maxOf(extents.h, iconY + implementor.icon!!.iconHeight)
            drawIcon(implementor.icon!!, iconX, iconY)
        }
        else if (implementor.iconName != null && implementor.iconName.startsWith("shape:")) {
            val shape = implementor.iconName.substring(6)
            when(shape) {
                "start" -> {
                    if (display.w == 60 && display.h == 40) {
                        // use image for better quality via feathering
                        drawIcon(Display.START_ICON, display.x, display.y)
                    }
                    else {
                        // use image for better quality via feathering
                        drawOval(fill = Display.START_COLOR)
                    }
                    textColor = Display.SHAPE_TEXT_COLOR
                }
                "stop" -> {
                    if (display.w == 60 && display.h == 40) {
                        drawIcon(Display.STOP_ICON, display.x, display.y)
                    }
                    else {
                        drawOval(fill = Display.STOP_COLOR)
                    }
                    textColor = Display.SHAPE_TEXT_COLOR
                }
                "decision" -> {
                    drawDiamond()
                }
                else -> {
                    drawRect()
                }
            }
        }
        else {
            drawRect()
        }

        // label
        if (activity.name != null) {
            val lines = activity.name.lines()
            var w = 0
            var y = display.y + display.h / 2
            if (lines.size == 1) {
                // center the one and only line
                y += g2d.fontMetrics.height / 2
            }
            if (implementor.icon != null) {
                y += implementor.icon!!.iconHeight / 2
            }
            if (y < 0) {
                y = 0
            }

            for (line in lines) {
                val lw = g2d.fontMetrics.stringWidth(line)
                if (lw > w) {
                    w = lw
                }
                val x = display.x + display.w / 2 - lw / 2
                extents.w = maxOf(extents.w, x + w)
                extents.h = maxOf(extents.h, y + yAdjust + g2d.fontMetrics.height)
                drawText(line, x, y + yAdjust, color = textColor)
                y += g2d.fontMetrics.height - 1
            }
        }

        // logical id
        if (activity.id > 0) {
            extents.w = maxOf(extents.w, this.display.x + 2 + g2d.fontMetrics.stringWidth(activity.id.toString()))
            extents.h = maxOf(extents.h, this.display.y - 2 + g2d.fontMetrics.height)
            drawText(activity.logicalId, this.display.x + 2, this.display.y - 2, color = Display.META_COLOR)
        }

        return extents
    }

    override fun move(deltaX: Int, deltaY: Int, limits: Display?) {
        val d = Display(display.x + deltaX, display.y + deltaY, display.w, display.h)
        limits?.let {
            if (d.x < it.x) {
                d.x = it.x
            }
            else if (d.x > it.x + it.w - d.w) {
                d.x = it.x + it.w - d.w
            }
            if (d.y < it.y) {
                d.y = it.y
            }
            else if (d.y > it.y + it.h - d.h) {
                d.y = it.y + it.h - d.h
            }
        }
        activity.setAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO, d.toString())
    }

    override fun resize(anchor: Int, x: Int, y: Int, deltaX: Int, deltaY: Int, limits: Display?) {
        display = resizeDisplay(anchor, x, y, deltaX, deltaY, Step.MIN_SIZE, limits)
        activity.setAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO, display.toString())
    }

    companion object {
        const val MIN_SIZE = 4
    }
}