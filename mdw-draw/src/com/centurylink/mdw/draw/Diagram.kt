package com.centurylink.mdw.draw

import com.centurylink.mdw.constant.WorkAttributeConstant.LOGICAL_ID
import com.centurylink.mdw.constant.WorkAttributeConstant.WORK_DISPLAY_INFO
import com.centurylink.mdw.draw.edit.Select
import com.centurylink.mdw.draw.edit.Selectable
import com.centurylink.mdw.draw.edit.Selection
import com.centurylink.mdw.draw.ext.*
import com.centurylink.mdw.draw.model.DrawProps
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.draw.model.WorkflowType
import com.centurylink.mdw.model.project.Project
import com.centurylink.mdw.model.workflow.ActivityImplementor
import com.centurylink.mdw.model.workflow.Process
import java.awt.Color
import java.awt.Cursor
import java.awt.Graphics2D

class Diagram(val g2d: Graphics2D, val display: Display, val project: Project, val process: Process,
        val implementors: Map<String, ActivityImplementor>, val props: DrawProps = DrawProps()) : Drawable, Selectable by Select() {

    override val workflowObj = object : WorkflowObj(project, process, WorkflowType.process, process.json, props) {
        init {
            id = process.id?.toString() ?: "-1"
            name = process.name
            version = "v${process.versionString}"
        }
    }

    var grid: Grid? = null
    private var background = Color.WHITE

    var hoverObj: Drawable? = null
    var selection: Selection

    var label = Label(g2d, Display(process.getAttribute(WORK_DISPLAY_INFO)), process.name, this, Display.TITLE_FONT)
    val steps = mutableListOf<Step>()
    val links = mutableListOf<Link>()
    val subflows = mutableListOf<Subflow>()
    val notes = mutableListOf<Note>()

    init {
        g2d.background = background

        selection = Selection(this)

        // activities
        for (activity in process.activities) {
            val impl = implementors[activity.implementor] ?: ActivityImplementor(activity.implementor)
            val step = Step(g2d, project, process, activity, impl, props)
            steps.add(step)
        }

        // transitions
        for (step in steps) {
            for (transition in process.getAllTransitions(step.activity.id)) {
                val link = Link(g2d, project, process, transition, step, steps.find {
                    it.workflowObj.id == "A${transition.toId}"
                }!!, props)
                links.add(link)
            }
        }

        // subflows
        for (subprocess in process.subprocesses) {
            val subflow = Subflow(g2d, project, process, subprocess, implementors, props)
            subflows.add(subflow)
        }

        // notes
        for (textNote in process.textNotes) {
            val note = Note(g2d, project, process, textNote, props)
            notes.add(note)
        }
    }

    override fun draw(): Display {
        grid?.draw()

        makeRoom(label.draw())

        for (step in steps) {
            makeRoom(step.draw())
        }

        for (link in links) {
            makeRoom(link.draw())
        }

        for (subflow in subflows) {
            makeRoom(subflow.draw())
        }

        for (note in notes) {
            makeRoom(note.draw())
        }

        doSelect()

        selection.destination?.let {
            g2d.color = LINE_COLOR
            g2d.drawLine(it.x, it.y, it.x + it.w, it.y + it.h)
            g2d.color = Display.DEFAULT_COLOR
        }

        selection.marquee?.let {
            g2d.color = MARQUEE_COLOR
            g2d.drawRoundRect(it.x, it.y, it.w, it.h, MARQUEE_ROUNDING, MARQUEE_ROUNDING)
            g2d.color = Display.DEFAULT_COLOR
        }

        return display
    }

    private fun makeRoom(extents: Display) {
        val reqWidth = extents.x + extents.w
        if (reqWidth > display.w)
            display.w = reqWidth
        val reqHeight = extents.y + extents.h
        if (reqHeight > display.h)
            display.h = reqHeight
    }

    /**
     * reselect objects based on id (since new g2d instance)
     */
    private fun doSelect() {
        val selectObjs = mutableListOf<Drawable>()
        for (selectObj in selection.selectObjs) {
            if (selectObj is Label && selectObj.owner.workflowObj.id == this.workflowObj.id) {
                // label was selected specifically
                label.select()
                selectObjs.add(label)
            }
            else {
                findObj(selectObj.workflowObj.id)?.let {
                    (it as Selectable).select()
                    selectObjs.add(it)
                }
            }
        }
        if (selectObjs.isEmpty()) {
            selectObjs.add(this)
        }
        selection.selectObjs = selectObjs
    }

    override fun select() {
        isSelected = true
    }

    fun hasSelection(): Boolean {
        return selection.selectObj != this && selection.selectObj !is Label
    }

    override fun move(deltaX: Int, deltaY: Int, limits: Display?) {
        moveLabel(deltaX, deltaY, limits)
    }

    private fun moveLabel(deltaX: Int, deltaY: Int, limits: Display?) {
        val d = Display(label.display.x + deltaX, label.display.y + deltaY, label.display.w, label.display.h)
        process.setAttribute(WORK_DISPLAY_INFO, d.toString())
        limits?.let {
            d.limit(it)
        }
    }

    fun findObj(id: String): Drawable? {
        for (subflow in subflows) {
            val subflowObj = subflow.findObj(id)
            if (subflowObj != null) {
                return subflowObj
            }
        }
        return when {
            id == this.workflowObj.id -> this
            id.startsWith("A") -> steps.find { it.workflowObj.id == id }
            id.startsWith("T") -> links.find { it.workflowObj.id == id }
            id.startsWith("P") -> subflows.find { it.workflowObj.id == id }
            id.startsWith("N") -> notes.find { it.workflowObj.id == id }
            else -> null
        }
    }

    private fun addStep(x: Int, y: Int, implementor: ActivityImplementor): Step {
        for (subflow in subflows) {
            if (subflow.isHover(x, y)) {
                return subflow.addStep(implementor, x, y)
            }
        }
        val activity = process.addActivity(x, y, implementor, process.maxActivityId() + 1)
        val step = Step(g2d, project, process, activity, implementor, props)
        steps.add(step) // unnecessary if redrawn
        snap(step)
        return step
    }

    private fun getLinks(step: Step): List<Link> {
        val links = mutableListOf<Link>()
        for (link in this.links) {
            if (step.activity.id == link.to.activity.id || step.activity.id == link.from.activity.id) {
                links.add(link)
            }
        }
        return links
    }

    private fun addLink(from: Step, to: Step): Link {
        for (subflow in subflows) {
            if (subflow.findStep(from.activity.logicalId) != null && subflow.findStep(to.activity.logicalId) != null) {
                return subflow.addLink(from, to)
            }
        }
        val transition = process.addTransition(from.activity, to.activity, process.maxTransitionId() + 1)
        val link = Link(g2d, project, process, transition, from, to, props)
        link.calc()
        links.add(link) // unnecessary if redrawn
        return link
    }

    private fun addSubflow(x: Int, y: Int, type: String): Subflow {
        val subprocess = process.addSubprocess(x, y, type)
        val subflow = Subflow(g2d, project, process, subprocess, implementors, props)
        subflows.add(subflow)
        snap(subflow)
        return subflow
    }

    private fun addNote(x: Int, y: Int): Note {
        val textNote = process.addTextNote(x, y)
        val note = Note(g2d, project, process, textNote, props)
        notes.add(note) // unnecessary if redrawn
        return note
    }

    fun onMouseMove(de: DiagramEvent): Cursor {
        hoverObj = getHoverObj(de.x, de.y)
        hoverObj?.let {
            if (!props.isReadonly && (hoverObj == selection.selectObj)) {
                selection.anchor = hoverObj?.getAnchor(de.x, de.y)
                if (selection.anchor != null) {
                    if (hoverObj is Link) {
                        return Cursor(Cursor.CROSSHAIR_CURSOR)
                    }
                    else {
                        if (selection.anchor == 0 || selection.anchor == 2) {
                            return Cursor(Cursor.NW_RESIZE_CURSOR)
                        }
                        else if (selection.anchor == 1 || selection.anchor == 3) {
                            return Cursor(Cursor.NE_RESIZE_CURSOR)
                        }
                    }
                }
            }
            return Cursor(Cursor.HAND_CURSOR)
        }
        return Cursor.getDefaultCursor()
    }

    fun onMouseDown(de: DiagramEvent) {
        var selObj = getHoverObj(de.x, de.y)
        if (selObj == null) {
            selObj = this
        }
        if (selObj is Label && selObj.owner != this) {
            selObj = selObj.owner
        }
        if (!props.isReadonly && de.ctrl) {
            if (selection.includes(selObj)) {
                selection.remove(selObj)
            }
            else {
                addToSelection(selObj)
            }
        }
        else {
            if (selObj == label || !selection.includes(selObj)) {
                // normal single select
                selection.selectObj = selObj
                selection.anchor = null
            }
        }
    }

    fun onMouseUp(de: DiagramEvent) {
        val resize = selection.anchor != null
        selection.anchor = null
        selection.destination = null
        if (de.drag) {
            if (de.shift && selection.selectObj is Step) {
                val destObj = getHoverObj(de.x, de.y)
                if (destObj is Step) {
                    selection.selectObj = addLink(selection.selectObj as Step, destObj)
                }
            }
            else if ((grid?.snap ?: 0) > 0) {
                for (selObj in selection.selectObjs) {
                    if (selObj is Shape) {
                        snap(selObj, resize)
                    }
                }
            }
        }

        selection.marquee?.let {
            val selectObjs = getSelectObjs(it)
            if (selectObjs.isEmpty()) {
                selection.selectObj = this
            }
            else {
                selection.selectObjs = selectObjs
            }
            selection.marquee = null
        }
    }

    fun onMouseDrag(de: DiagramEvent, drag: DragEvent) {
        if (!props.isReadonly && !de.ctrl && de.drag) {
            val deltaX = de.x - drag.origX
            val deltaY = de.y - drag.origY
            if (Math.abs(deltaX) > Display.MIN_DRAG || Math.abs(deltaY) > Display.MIN_DRAG) {
                if (de.shift && de.drag) {
                    if (selection.selectObj is Step) {
                        selection.destination = Display(drag.origX, drag.origY, de.x - drag.origX, de.y - drag.origY)
                    }
                }
                else if (selection.anchor != null) {
                    val anchor = selection.anchor!!
                    if (selection.selectObj is Link) {
                        val link = selection.selectObj as Link
                        link.moveAnchor(selection.anchor!!, de.x, de.y)
                        if (selection.anchor == 0) {
                            val hovStep = getHoverStep(de.x, de.y)
                            if (hovStep != null && link.from.activity.id != hovStep.activity.id) {
                                link.setFromStep(hovStep)
                            }
                        }
                        else if (selection.anchor == link.display.xs.size - 1) {
                            val hovStep = getHoverStep(de.x, de.y)
                            if (hovStep != null && link.to.activity.id != hovStep.activity.id) {
                                link.setToStep(hovStep)
                            }
                        }
                    }
                    if (selection.selectObj is Resizable) {
                        val resizable = selection.selectObj as Resizable
                        if (resizable is Step) {
                            val activityId = (selection.selectObj as Step).activity.id
                            var step = steps.find { it.activity.id == activityId }
                            if (step != null) {
                                step.resize(anchor, drag.origX, drag.origY, de.x - drag.origX, de.y - drag.origY)
                                for (link in getLinks(step)) {
                                    link.recalc(step)
                                }
                            } else {
                                // try subflows
                                for (subflow in subflows) {
                                    step = subflow.steps.find { it.activity.id == activityId }
                                    if (step != null) {
                                        // only within bounds of subflow
                                        step.resize(anchor, drag.origX, drag.origY, de.x - drag.origX, de.y - drag.origY, subflow.display)
                                        for (link in subflow.getLinks(step)) {
                                            link.recalc(step)
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            resizable.resize(anchor, drag.origX, drag.origY, de.x - drag.origX, de.y - drag.origY)
                        }
                        getHoverObj(de.x, de.y)?.let {
                            if (it is Selectable) {
                                it.select()
                            }
                        }
                    }
                }
                else {
                    if (selection.selectObj != this) {
                        moveSelection(drag.deltaX, drag.deltaY, de.x, de.y)
                    }
                    else {
                        selection.marquee = Display(drag.origX, drag.origY, de.x - drag.origX, de.y - drag.origY)
                    }
                }
            }
        }
    }

    fun onDrop(de: DiagramEvent, implementor: ActivityImplementor): Drawable {
        when (implementor.category) {
            "subflow" -> {
                selection.selectObj = addSubflow(de.x, de.y, implementor.implementorClass)
            }
            "note" -> {
                selection.selectObj = addNote(de.x, de.y)
            }
            else -> {
                selection.selectObj = addStep(de.x, de.y, implementor)
            }
        }
        return selection.selectObj
    }

    fun onPaste(pasted: Selection) {
        for (selObj in pasted.selectObjs) {
            selObj.move(PASTE_OFFSET, PASTE_OFFSET)
            when (selObj) {
                is Step ->  {
                    process.activities.add(selObj.activity)
                    steps.add(selObj)
                }
                is Link -> {
                    process.transitions.add(selObj.transition)
                    links.add(selObj)
                }
                is Subflow -> {
                    process.subprocesses.add(selObj.subprocess)
                    subflows.add(selObj)
                }
                is Note -> {
                    process.textNotes.add(selObj.textNote)
                    notes.add(selObj)
                }
            }
        }
        selection = pasted
    }

    fun onDelete() {
        deleteSelection()
    }

    fun getHoverObj(x: Int, y: Int): Drawable? {
        if (label.isHover(x, y)) {
            return label
        }
        // links checked before steps for better anchor selectability
        for (subflow in subflows)  {
            if (subflow.label.isHover(x, y)) {
                return subflow
            }
            if (subflow.isHover(x, y)) {
                for (link in subflow.links) {
                    if (link.isHover(x, y)) {
                        return link
                    }
                }
                for (step in subflow.steps) {
                    if (step.isHover(x, y)) {
                        return step
                    }
                }
                return subflow
            }
        }
        for (link in links) {
            if (link.isHover(x, y)) {
                return link
            }
        }
        for (step in steps) {
            if (step.isHover(x, y)) {
                return step
            }
        }
        for (note in notes) {
            if (note.isHover(x, y)) {
                return note
            }
        }
        return null
    }

    private fun getHoverStep(x: Int, y: Int): Step? {
        for (subflow in subflows)  {
            if (subflow.isHover(x, y)) {
                for (step in subflow.steps) {
                    if (step.isHover(x, y)) {
                        return step
                    }
                }
            }
        }
        for (step in steps) {
            if (step.isHover(x, y)) {
                return step
            }
        }
        return null
    }

    private fun getSelectObjs(display: Display): MutableList<Drawable> {
        val selectObjs = mutableListOf<Drawable>()
        if (display.contains(label.display)) {
            selectObjs.add(label)
        }
        for (step in steps) {
            if (display.contains(step.display)) {
                selectObjs.add(step)
            }
        }
        for (subflow in subflows) {
            if (display.contains(subflow.display)) {
                selectObjs.add(subflow)
            }
        }
        for (note in notes) {
            if (display.contains(note.display)) {
                selectObjs.add(note)
            }
        }

        return selectObjs
    }

    private fun moveSelection(deltaX: Int, deltaY: Int, currentX: Int, currentY: Int)  {
        val limits = Display(0, 0, Int.MAX_VALUE, Int.MAX_VALUE)

        if (!selection.isMulti() && selection.selectObj is Link) {
            val link = selection.selectObj as Link
            link.label?.let {
                if (it.isHover(currentX, currentY)) {
                    link.moveLabel(deltaX, deltaY, limits)
                }
            }
        }
        else {
            for (selObj in selection.selectObjs) {
                if (selObj is Step) {
                    val activityId = selObj.activity.id
                    var step = steps.find { it.activity.id == activityId }
                    if (step != null) {
                        step.move(deltaX, deltaY, limits)
                        for (link in getLinks(step)) {
                            link.recalc(step)
                        }
                    }
                    else {
                        // try subflows
                        for (subflow in subflows) {
                            step = subflow.steps.find { it.activity.id == activityId }
                            if (step != null) {
                                // only within bounds of subflow
                                step.move(deltaX, deltaY, subflow.display)
                                for (link in subflow.getLinks(step)) {
                                    link.recalc(step)
                                }
                            }
                        }
                    }
                }
                else {
                    if (selObj == label) {
                        this.move(deltaX, deltaY, limits)
                    }
                    else {
                        // TODO: prevent subproc links in multisel from moving beyond border
                        selObj.move(deltaX, deltaY, limits)
                    }
                }
            }
        }
    }

    private fun addToSelection(selObj: Drawable) {
        selection.add(selObj)
        if (selObj is Selectable) {
            selObj.select()
        }
        if (selObj is Step) {
            // add any contained links
            var step = steps.find { it.workflowObj.id == selObj.workflowObj.id}
            var stepLinks: List<Link>? = null
            if (step != null) {
                stepLinks = getLinks(step)
            }
            else {
                for (subflow in subflows) {
                    step = subflow.steps.find { it.workflowObj.id == selObj.workflowObj.id }
                    stepLinks = step?.let { getLinks(it) }
                    if (stepLinks != null) {
                        break
                    }
                }
            }

            stepLinks?.let {
                for (stepLink in stepLinks) {
                    if (stepLink.from == selObj) {
                        if (selection.includes(stepLink.to)) {
                            selection.add(stepLink)
                            stepLink.select()
                        }
                    }
                    else {
                        if (selection.includes(stepLink.from)) {
                            selection.add(stepLink)
                            stepLink.select()
                        }
                    }
                }
            }
        }
    }

    private fun snap(shape: Shape, resize: Boolean = false) {
        grid?.let { g ->
            g.snap(shape.display, resize)
            when (shape) {
                is Step -> {
                    shape.activity.setAttribute(WORK_DISPLAY_INFO, shape.display.toString())
                    if (steps.find{ it.activity.id == shape.activity.id } != null) {
                        for (link in getLinks(shape)) {
                            link.recalc(shape)
                        }
                    }
                    else {
                        for (subflow in subflows) {
                            if (subflow.steps.find{ it.activity.id == shape.activity.id } != null) {
                                for (link in subflow.getLinks(shape)) {
                                    link.recalc(shape)
                                }
                            }
                        }
                    }
                }
                is Note -> {
                    shape.textNote.setAttribute(WORK_DISPLAY_INFO, shape.display.toString())
                }
                is Subflow -> {
                    shape.subprocess.setAttribute(WORK_DISPLAY_INFO, shape.display.toString())
                    for (step in shape.steps) {
                        g.snap(step.display)
                        step.activity.setAttribute(WORK_DISPLAY_INFO, step.display.toString())
                        for (link in shape.getLinks(step)) {
                            link.recalc(step)
                        }
                    }
                }
            }
        }
    }

    private fun deleteStep(step: Step) {
        val logicalId = step.activity.logicalId
        val i = steps.findIndex { it.activity.logicalId == logicalId }
        if (i >= 0) {
            process.deleteActivity(logicalId)
            steps.removeAt(i)
            for (link in links.toList()) {
                if (link.from.activity.logicalId == logicalId || link.to.activity.logicalId == logicalId) {
                    deleteLink(link)
                }
            }
        }
        else {
            subflows.let {
                for (subflow in subflows) {
                    val si = subflow.steps.findIndex { it.activity.logicalId == logicalId }
                    if (si >= 0) {
                        subflow.subprocess.deleteActivity(logicalId)
                        subflow.steps.removeAt(si)
                        for (link in subflow.links.toList()) {
                            if (link.from.activity.logicalId == logicalId || link.to.activity.logicalId == logicalId) {
                                deleteLink(link)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun deleteLink(link: Link) {
        val logicalId = link.transition.logicalId
        val i = links.findIndex { it.transition.logicalId == logicalId }
        if (i >= 0) {
            process.deleteTransition(logicalId)
            links.removeAt(i)
        }
        else {
            subflows.let {
                for (subflow in subflows) {
                    val si = subflow.links.findIndex { it.transition.logicalId == logicalId }
                    if (si >= 0) {
                        subflow.subprocess.deleteTransition(logicalId)
                        subflow.links.removeAt(si)
                    }
                }
            }
        }
    }

    private fun deleteSubflow(subflow: Subflow) {
        val logicalId = "P${subflow.subprocess.id}"
        process.deleteSubprocess(logicalId)
        val i = subflows.findIndex { it.subprocess.id == subflow.subprocess.id }
        if (i > 0) subflows.removeAt(i)
    }

    private fun deleteNote(note: Note) {
        val logicalId = note.textNote.logicalId
        process.deleteTextNote(logicalId)
        val i = notes.findIndex { it.textNote.logicalId == logicalId }
        if (i > 0) notes.removeAt(i)
    }

    private fun deleteSelection() {
        if (selection.selectObj != this && !(selection.selectObj is Label)) {
            for (selObj in selection.selectObjs) {
                when (selObj) {
                    is Step -> deleteStep(selObj)
                    is Link -> deleteLink(selObj)
                    is Subflow -> deleteSubflow(selObj)
                    is Note -> deleteNote(selObj)
                }
            }
        }
    }

    fun rename(newName: String) {
        label = Label(g2d, Display(process.getAttribute(WORK_DISPLAY_INFO)), newName, this, Display.TITLE_FONT)
    }

    fun incrementStepId(step: Step) {
        val oldLogicalId = step.activity.logicalId
        val newId = process.maxActivityId() + 1
        val newLogicalId = "A$newId"
        if (steps.findIndex { it.activity.logicalId == oldLogicalId } >= 0) {
            step.activity.id = newId
            step.activity.setAttribute(LOGICAL_ID, newLogicalId)
            for (link in links.toList()) {
                if (link.from.activity.logicalId == newLogicalId) {
                    link.setFromStep(step)
                }
                else if (link.to.activity.logicalId == newLogicalId) {
                    link.setToStep(step)
                }
            }
        }
        else {
            subflows.let {
                for (subflow in subflows) {
                    if (subflow.steps.findIndex { it.activity.logicalId == oldLogicalId } >= 0) {
                        step.activity.id = newId
                        step.activity.setAttribute(LOGICAL_ID, newLogicalId)
                        for (link in subflow.links.toList()) {
                            if (link.from.activity.logicalId == newLogicalId) {
                                link.setFromStep(step)
                            }
                            else if (link.to.activity.logicalId == newLogicalId) {
                                link.setToStep(step)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val BOUNDARY_DIM = 25
        var LINE_COLOR = Color.GREEN
        val MARQUEE_COLOR = Color.CYAN
        const val MARQUEE_ROUNDING = 3
        const val PASTE_OFFSET = 20
    }
}

data class DiagramEvent(
    val x: Int,
    val y: Int,
    val shift: Boolean = false,
    val ctrl: Boolean = false,
    val drag: Boolean = false
)

/**
 * delta is since last drag event
 */
data class DragEvent(
    val origX: Int,
    val origY: Int,
    val deltaX: Int,
    val deltaY: Int
)