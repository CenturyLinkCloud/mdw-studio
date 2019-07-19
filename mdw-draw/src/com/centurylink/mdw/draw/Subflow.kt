package com.centurylink.mdw.draw

import com.centurylink.mdw.constant.WorkAttributeConstant
import com.centurylink.mdw.draw.ext.addActivity
import com.centurylink.mdw.draw.ext.addTransition
import com.centurylink.mdw.draw.ext.maxActivityId
import com.centurylink.mdw.draw.ext.maxTransitionId
import com.centurylink.mdw.draw.model.DrawProps
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.draw.model.WorkflowType
import com.centurylink.mdw.model.project.Project
import com.centurylink.mdw.model.workflow.ActivityImplementor
import com.centurylink.mdw.model.workflow.Process
import java.awt.Color
import java.awt.Graphics2D

class Subflow(private val g2d: Graphics2D, private val project: Project, private val process: Process,
        val subprocess: Process, val implementors: Map<String,ActivityImplementor>, val props: DrawProps) :
        Shape(g2d, Display(subprocess.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO))), Drawable, Resizable  {

    override val workflowObj = object: WorkflowObj(project, process, WorkflowType.subprocess, subprocess.json, props) {
        init {
            id = if (subprocess.id == null) "-1" else "P" + subprocess.id
            name = subprocess.name
        }
    }

    var label: Label
    val steps = mutableListOf<Step>()
    val links = mutableListOf<Link>()

    init {
        // label
        g2d.font = Display.DEFAULT_FONT
        val labelX = display.x + 10
        val labelY = display.y - g2d.fontMetrics.ascent + Label.PAD
        label = Label(g2d, Display(labelX, labelY), subprocess.name, this)

        // activities
        for (activity in subprocess.activities) {
            var impl = implementors[activity.implementor]
            if (impl == null) {
                impl = ActivityImplementor(activity.implementor)
            }
            val step = Step(g2d, project, process, activity, impl, props)
            steps.add(step)
        }

        // transitions
        for (step in steps) {
            for (transition in subprocess.getAllTransitions(step.activity.id)) {
                val link = Link(g2d, project, process, transition, step, findStep("A${transition.toId}")!!, props)
                links.add(link)
            }
        }
    }

    fun findStep(logicalId: String): Step? {
        for (step in steps) {
            if (step.activity.logicalId == logicalId) {
                return step
            }
        }
        return null
    }

    fun addStep(implementor: ActivityImplementor, x: Int, y: Int): Step {
        val activity = subprocess.addActivity(x, y, implementor, process.maxActivityId() + 1)
        val step = Step(g2d, project, process, activity, implementor, props)
        steps.add(step) // unnecessary if redrawn
        return step
    }

    fun getLinks(step: Step): List<Link> {
        val links = mutableListOf<Link>()
        for (link in this.links) {
            if (step.activity.id == link.to.activity.id || step.activity.id == link.from.activity.id) {
                links.add(link)
            }
        }
        return links
    }

    fun addLink(from: Step, to: Step): Link {
        val transition = subprocess.addTransition(from.activity, to.activity, process.maxTransitionId() + 1)
        val link = Link(g2d, project, process, transition, from, to, props)
        link.calc()
        links.add(link) // unnecessary if redrawn
        return link
    }

    override fun draw(): Display {

        val extents = Display(0, 0, display.x + display.w, display.y + display.h)
        drawRect(border = Subflow.BOX_OUTLINE_COLOR)

        // label
        extents.w = maxOf(extents.w, label.display.x + label.display.w)
        extents.h = maxOf(extents.h, label.display.y)
        val labelW = g2d.fontMetrics.stringWidth(label.text) + Label.PAD * 2
        val labelH = g2d.fontMetrics.height + Label.PAD * 2
        clearRect(label.display.x - 1, label.display.y, labelW + 1, labelH)
        label.draw()

        for (step in steps) {
            val d = step.draw()
            extents.w = maxOf(extents.w, d.x + d.w)
        }

        for (link in links) {
            val d = link.draw()
            extents.h = maxOf(extents.h, d.y + d.h)
        }

        // logical id
        if (subprocess.id > 0) {
            val metaX = display.x + 10
            val metaY = display.y + display.h + g2d.fontMetrics.descent
            val metaW = g2d.fontMetrics.stringWidth("P${subprocess.id}")
            val metaH = g2d.fontMetrics.height

            extents.w = maxOf(extents.w, metaX + metaW)
            extents.h = maxOf(extents.h, metaY)

            clearRect(metaX - 1, metaY - metaH + g2d.fontMetrics.descent, metaW + 2, metaH)
            drawText("[${subprocess.id}]", metaX, metaY, color = Display.META_COLOR)
        }

        return extents
    }

    fun findObj(id: String): Drawable? {
        return when {
            id.startsWith("A") -> return steps.find { it.workflowObj.id == id }
            id.startsWith("T") -> return links.find { it.workflowObj.id == id }
            else -> null
        }
    }

    override fun move(deltaX: Int, deltaY: Int, limits: Display?) {
        display = Display(display.x + deltaX, display.y + deltaY, display.w, display.h)
        limits?.let {
            display.limit(limits)
        }
        subprocess.setAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO, display.toString())
        for (step in steps) {
            step.move(deltaX, deltaY, display)
        }
        for (link in links) {
            link.move(deltaX, deltaY, display)
        }
    }

    override fun resize(anchor: Int, x: Int, y: Int, deltaX: Int, deltaY: Int, limits: Display?) {
        display = resizeDisplay(anchor, x, y, deltaX, deltaY, Step.MIN_SIZE)
        subprocess.setAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO, display.toString())
    }

    companion object {
        var BOX_OUTLINE_COLOR = Color(0x337ab)
    }
}