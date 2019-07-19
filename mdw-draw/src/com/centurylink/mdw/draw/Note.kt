package com.centurylink.mdw.draw

import com.centurylink.mdw.constant.WorkAttributeConstant
import com.centurylink.mdw.draw.model.DrawProps
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.draw.model.WorkflowType
import com.centurylink.mdw.model.project.Project
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.model.workflow.TextNote
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D

class Note (private val g2d: Graphics2D, project: Project, process: Process, val textNote: TextNote, props: DrawProps) :
        Shape(g2d, Display(textNote.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO))), Drawable, Resizable {

    override val workflowObj = object : WorkflowObj(project, process, WorkflowType.textNote, textNote.json, props) {
        override var name: String = "Note"
            get() = "Note ${textNote.logicalId}"
    }

    override fun draw(): Display {

        var extents = Display(0, 0, display.x + display.w, display.y + display.h)

        drawRect(border = BOX_OUTLINE_COLOR, fill = BOX_FILL_COLOR, radius = BOX_ROUNDING_RADIUS)
        if (textNote.content != null) {
            g2d.font = FONT
            val h = g2d.fontMetrics.height
            var y = display.y
            for (line in textNote.content.lines()) {
                y += h
                extents.w = maxOf(extents.w, display.x + 4 + g2d.fontMetrics.stringWidth(line))
                extents.h = maxOf(extents.h, y)
                drawText(line, display.x + 4, y, FONT, Display.NOTE_COLOR)
            }
        }

        return extents
    }

    override fun move(deltaX: Int, deltaY: Int, limits: Display?) {
        display = Display(display.x + deltaX, display.y + deltaY, display.w, display.h)
        textNote.setAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO, display.toString())
    }

    override fun resize(anchor: Int, x: Int, y: Int, deltaX: Int, deltaY: Int, limits: Display?) {
        display = resizeDisplay(anchor, x, y, deltaX, deltaY, Shape.MIN_SIZE, limits)
        textNote.setAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO, display.toString())
    }

    companion object {
        val BOX_OUTLINE_COLOR = Color(0x808080)
        val BOX_FILL_COLOR = Color(0xffffcc)
        val BOX_ROUNDING_RADIUS = 2
        val FONT = Font("Monospace", Font.PLAIN, 13)
    }
}