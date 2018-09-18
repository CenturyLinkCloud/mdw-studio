package com.centurylink.mdw.draw

import com.centurylink.mdw.draw.edit.Select
import com.centurylink.mdw.draw.edit.Selectable
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.draw.model.WorkflowType
import com.centurylink.mdw.model.Project
import com.centurylink.mdw.model.event.EventType
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.model.workflow.Transition
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.GeneralPath

class LinkDisplay {

    var type = Link.LinkType.Elbow
    var lx = 0
    var ly = 0
    var xs = mutableListOf<Int>()
    var ys = mutableListOf<Int>()

    constructor(attr: String?) {
        if (attr != null) {
            for (v in attr.split(',')) {
                if (v.startsWith("lx=")) {
                    lx = v.substring(3).toInt()
                }
                else if (v.startsWith("ly=")) {
                    ly = v.substring(3).toInt()
                }
                else if (v.startsWith("xs=")) {
                    for (x in v.substring(3).split('&')) {
                        xs.add(x.toInt())
                    }
                }
                else if (v.startsWith("ys=")) {
                    for (y in v.substring(3).split('&')) {
                        ys.add(y.toInt())
                    }
                }
                else if (v.startsWith("type=")) {
                    var t = v.substring(5)
                    if (t == "Curve") t = "Elbow" // Curve not supported
                    type = Link.LinkType.valueOf(t)
                }
            }
        }
    }

    constructor(type: Link.LinkType, lx: Int, ly: Int,
            xs: MutableList<Int> = mutableListOf(), ys: MutableList<Int> = mutableListOf()) {
        this.type = type
        this.lx = lx
        this.ly = ly
        this.xs = xs
        this.ys = ys
    }

    override fun toString(): String {
        var attr = "type=$type,lx=$lx,ly=$ly"
        attr += ",xs="
        for (i in xs.indices) {
            if (i > 0) {
                attr += "&"
            }
            attr += xs[i]
        }
        attr += ",ys="
        for (i in ys.indices) {
            if (i > 0) {
                attr += "&"
            }
            attr += ys[i]
        }
        return attr
    }
}

class Pt(val x: Int, val y: Int) { }

class Seg(val from: Pt, val to: Pt) { }

class Link(val g2d: Graphics2D, project: Project, process: Process, val transition: Transition, var from: Step, var to: Step) :
        Drawable, Selectable by Select() {

    override val workflowObj = object : WorkflowObj(project, process, WorkflowType.transition, transition.json) {
        override var name: String = ""
            get() = if (labelText.length > 0) labelText else ""
    }

    val anchors: Map<Int,Anchor>
        get() {
            val map = mutableMapOf<Int,Anchor>()
            for (i in display.xs.indices) {
                map.put(i, Anchor(g2d, this, i, display.xs[i], display.ys[i]))
            }
            return map
        }

    val event = EventType.getEventTypeName(transition.eventType)
    val color = EventColor.valueOf(event).color
    var display = LinkDisplay(transition.getAttribute("TRANSITION_DISPLAY_INFO"))

    var label: Label?
    val labelText: String
        get() {
            var text = if (event == EventType.EVENTNAME_FINISH) "" else event + ":"
            text += if (transition.completionCode == null) "" else transition.completionCode
            return text
        }

    private val calcs = Calcs(display)

    init {
        if (labelText.length > 0) {
            label = Label(g2d, Display(display.lx, display.ly + LABEL_CORR), labelText, this)
        }
        else {
            label = null
        }
    }

    fun setFromStep(step: Step) {
        this.transition.fromId = step.activity.id
        this.from = step
    }

    fun setToStep(step: Step) {
        this.transition.toId = step.activity.id
        this.to = step
    }

    override fun draw(): Display {
        g2d.color = color
        g2d.paint = color

        drawConnector(null, null)
        label?.draw()

        // TODO: determine extents
        return Display(0, 0, 0, 0)
    }

    fun calc(points: Int? = null) {
        calcs.calc(points, from, to)
        transition.setAttribute("TRANSITION_DISPLAY_INFO", display.toString())
    }

    fun recalc(step: Step) {
        calcs.recalc(step, from, to)
        transition.setAttribute("TRANSITION_DISPLAY_INFO", display.toString())
    }

    override fun isHover(x: Int, y: Int): Boolean {
        return (label?.isHover(x, y) ?: false) || drawConnector(x, y)
    }

    override fun getAnchor(x: Int, y: Int): Int? {
        for ((i, anchor) in anchors) {
            if (anchor.isHover(x, y))
                return i
        }
        return null
    }

    fun moveAnchor(anchor: Int, newX: Int, newY: Int) {
        val display = LinkDisplay(this.display.type, this.display.lx, this.display.ly)

        for (i in this.display.xs.indices) {
            if (i == anchor) {
                display.xs.add(newX)
                display.ys.add(newY)
            }
            else {
                display.xs.add(this.display.xs[i])
                display.ys.add(this.display.ys[i])
            }
        }
        if (display.type.toString().startsWith("Elbow") && display.xs.size != 2) {
            if (calcs.isHorizontal(anchor)) {
                if (anchor > 0) {
                    display.ys[anchor - 1] = newY
                }
                if (anchor < display.xs.size - 1) {
                    display.xs[anchor + 1] = newX
                }
            }
            else {
                if (anchor > 0) {
                    display.xs[anchor - 1] = newX
                }
                if (anchor < display.xs.size - 1) {
                    display.ys[anchor + 1] = newY
                }
            }
        }
        // TODO: update arrows
         this.display = display
        transition.setAttribute("TRANSITION_DISPLAY_INFO", display.toString())
    }

    override fun select() {
        for (anchor in anchors.values) {
            anchor.draw()
        }
        label?.select()
        isSelected = true
    }

    override fun move(deltaX: Int, deltaY: Int, limits: Display?) {
        val d = LinkDisplay(display.type, display.lx + deltaX, display.ly + deltaY)
        for (x in display.xs) {
            d.xs.add(x + deltaX)
        }
        for (y in display.ys) {
            d.ys.add(y + deltaY)
        }
        transition.setAttribute("TRANSITION_DISPLAY_INFO", d.toString())
    }

    fun moveLabel(deltaX: Int, deltaY: Int, limits: Display? = null) {
        val d = LinkDisplay(display.type, display.lx + deltaX, display.ly + deltaY, display.xs, display.ys)
        transition.setAttribute("TRANSITION_DISPLAY_INFO", d.toString())
    }

    private fun drawConnector(hitX: Int?, hitY: Int?): Boolean {
        val type = display.type
        val xs = display.xs
        val ys = display.ys

        g2d.stroke = LINK_STROKE

        var hit = false

        if (type.toString().startsWith("Elbow")) {
            if (xs.size == 2) {
                hit = drawAutoElbowConnector(hitX, hitY)
            }
            else {
                // TODO: make use of Link.CORR
                val path = GeneralPath()
                var horizontal = ys[0] == ys[1] && (xs[0] != xs[1] || xs[1] == xs[2])
                path.moveTo(xs[0].toFloat(), ys[0].toFloat())
                for (i in xs.indices) {
                    if (i == 0) {
                        continue
                    }
                    else if (hitX != null && hitY != null) {
                        val hitPath = GeneralPath()
                        hitPath.moveTo(xs[i - 1].toFloat(), ys[i - 1].toFloat())
                        hitPath.lineTo(xs[i].toFloat(), ys[i].toFloat())
                        if (hitPath.intersects(hitX - HIT_PAD, hitY - HIT_PAD, HIT_PAD * 2, HIT_PAD * 2)) {
                            return true
                        }
                    }
                    else if (horizontal) {
                        path.lineTo(if (xs[i] > xs[i - 1]) (xs[i] - CR).toFloat() else (xs[i] + CR).toFloat(), ys[i].toFloat())
                        if (i < xs.size - 1) {
                            path.quadTo(xs[i].toFloat(), ys[i].toFloat(), xs[i].toFloat(), if (ys[i + 1] > ys[i]) (ys[i] + CR).toFloat() else (ys[i] - CR).toFloat())
                        }
                        g2d.draw(path)
                    }
                    else {
                        path.lineTo(xs[i].toFloat(), if (ys[i] > ys[i - 1]) (ys[i] - CR).toFloat() else (ys[i] + CR).toFloat())
                        if (i < xs.size - 1) {
                            path.quadTo(xs[i].toFloat(), ys[i].toFloat(), if (xs[i + 1] > xs[i]) (xs[i] + CR).toFloat() else (xs[i] - CR).toFloat(), ys[i].toFloat())
                        }
                        g2d.draw(path)
                    }
                    horizontal = !horizontal
                }
            }
        }
        else if (type == LinkType.Straight) {
            val segs = mutableListOf<Seg>()
            for (i in xs.indices) {
                if (i < xs.size - 1) {
                   segs.add(Seg(Pt(xs[i], ys[i]), Pt(xs[i + 1], ys[i + 1])))
                }
            }
            if (hitX != null && hitY != null) {
                for (seg in segs) {
                    val hitPath = GeneralPath()
                    hitPath.moveTo(seg.from.x.toFloat(), seg.from.y.toFloat())
                    hitPath.lineTo(seg.to.x.toFloat(), seg.to.y.toFloat())
                    hit = hitPath.intersects(hitX - HIT_PAD, hitY - HIT_PAD, HIT_PAD * 2, HIT_PAD * 2)
                }
            }
            else {
                drawLine(segs)
            }
        }

        if (!hit) {
            hit = drawConnectorArrow(hitX, hitY)
        }

        g2d.stroke = Display.DEFAULT_STROKE
        g2d.color = Display.DEFAULT_COLOR
        g2d.paint = Display.DEFAULT_COLOR

        return hit
    }

    private fun drawAutoElbowConnector(hitX: Int?, hitY: Int?): Boolean {
        val xs = display.xs
        val ys = display.ys
        var t = 0
        val xcorr = if (xs[0] < xs[1]) CORR else -CORR
        val ycorr = if (ys[0] < ys[1]) CORR else -CORR
        val path = GeneralPath()
        when (getAutoElbowLinkType()) {
            AutoElbowLinkType.AutoLinkH -> {
                path.moveTo((xs[0] - xcorr).toFloat(), ys[0].toFloat())
                path.lineTo(xs[1].toFloat(), ys[1].toFloat())
            }
            AutoElbowLinkType.AutoLinkV -> {
                path.moveTo(xs[0].toFloat(), (ys[0] - ycorr).toFloat())
                path.lineTo(xs[1].toFloat(), ys[1].toFloat())
            }
            AutoElbowLinkType.AutoLinkHVH -> {
                t = (xs[0] + xs[1]) / 2
                path.moveTo((xs[0] - xcorr).toFloat(), ys[0].toFloat())
                path.lineTo(if (t > xs[0]) (t - CR).toFloat() else (t + CR).toFloat(), ys[0].toFloat())
                path.quadTo(t.toFloat(), ys[0].toFloat(), t.toFloat(), if (ys[1] > ys[0]) (ys[0] + CR).toFloat() else (ys[0] - CR).toFloat())
                path.lineTo(t.toFloat(), if (ys[1] > ys[0]) (ys[1] - CR).toFloat() else (ys[1] + CR).toFloat())
                path.quadTo(t.toFloat(), ys[1].toFloat(), if (xs[1] > t) (t + CR).toFloat() else (t - CR).toFloat(), ys[1].toFloat())
                path.lineTo(xs[1].toFloat(), ys[1].toFloat())
            }
            AutoElbowLinkType.AutoLinkVHV -> {
                t = (ys[0] + ys[1]) / 2
                path.moveTo(xs[0].toFloat(), (ys[0] - ycorr).toFloat())
                path.lineTo(xs[0].toFloat(), if (t > ys[0]) (t - CR).toFloat() else (t + CR).toFloat())
                path.quadTo(xs[0].toFloat(), t.toFloat(), if (xs[1] > xs[0]) (xs[0] + CR).toFloat() else (xs[0] - CR).toFloat(), t.toFloat())
                path.lineTo(if (xs[1] > xs[0]) (xs[1] - CR).toFloat() else (xs[1] + CR).toFloat(), t.toFloat())
                path.quadTo(xs[1].toFloat(), t.toFloat(), xs[1].toFloat(), if (ys[1] > t) (t + CR).toFloat() else (t - CR).toFloat())
                path.lineTo(xs[1].toFloat(), ys[1].toFloat())
            }
            AutoElbowLinkType.AutoLinkHV -> {
                path.moveTo((xs[0] - xcorr).toFloat(), ys[0].toFloat())
                path.lineTo(if (xs[1] > xs[0]) (xs[1] - CR).toFloat() else (xs[1] + CR).toFloat(), ys[0].toFloat())
                path.quadTo(xs[1].toFloat(), ys[0].toFloat(), xs[1].toFloat(), if (ys[1] > ys[0]) (ys[0] + CR).toFloat() else (ys[0] - CR).toFloat())
                path.lineTo(xs[1].toFloat(), ys[1].toFloat())
            }
            AutoElbowLinkType.AutoLinkVH -> {
                path.moveTo(xs[0].toFloat(), (ys[0] - ycorr).toFloat())
                path.lineTo(xs[0].toFloat(), if (ys[1] > ys[0]) (ys[1] - CR).toFloat() else (ys[1] + CR).toFloat())
                path.quadTo(xs[0].toFloat(), ys[1].toFloat(), if (xs[1] > xs[0]) (xs[0] + CR).toFloat() else (xs[0] - CR).toFloat(), ys[1].toFloat())
                path.lineTo(xs[1].toFloat(), ys[1].toFloat())
            }
        }

        if (hitX != null && hitY != null) {
            return path.intersects(hitX - HIT_PAD, hitY - HIT_PAD, HIT_PAD * 2, HIT_PAD * 2)
        }
        else {
            g2d.draw(path)
        }

        return false
    }

    private fun getAutoElbowLinkType(): AutoElbowLinkType {
        val xs = display.xs
        val ys = display.ys

        if (display.type == LinkType.ElbowH) {
            if (xs[0] == xs[1]) {
                return AutoElbowLinkType.AutoLinkV
            }
            else if (ys[0] == ys[1]) {
                return AutoElbowLinkType.AutoLinkH
            }
            else if (Math.abs(this.to.display.x - this.from.display.x) > ELBOW_VH_THRESHOLD) {
                return AutoElbowLinkType.AutoLinkHVH
            }
            else {
                return AutoElbowLinkType.AutoLinkHV
            }
        }
        else if (display.type === LinkType.ElbowV) {
            if (xs[0] == xs[1]) {
                return AutoElbowLinkType.AutoLinkV
            }
            else if (ys[0] == ys[1]) {
                return AutoElbowLinkType.AutoLinkH
            }
            else if (Math.abs(this.to.display.y - this.from.display.y) > ELBOW_VH_THRESHOLD) {
                return AutoElbowLinkType.AutoLinkVHV
            }
            else {
                return AutoElbowLinkType.AutoLinkVH
            }
        }
        else {
            if (xs[0] == xs[1]) {
                return AutoElbowLinkType.AutoLinkV
            }
            else if (ys[0] == ys[1]) {
                return AutoElbowLinkType.AutoLinkH
            }
            else if (Math.abs(this.to.display.x - this.from.display.x) < Math.abs(this.to.display.y - this.from.display.y) * ELBOW_THRESHOLD) {
                return AutoElbowLinkType.AutoLinkVHV
            }
            else if (Math.abs(this.to.display.y - this.from.display.y) < Math.abs(this.to.display.x - this.from.display.x) * ELBOW_THRESHOLD) {
                return AutoElbowLinkType.AutoLinkHVH
            }
            else {
                return AutoElbowLinkType.AutoLinkHV
            }
        }
    }

    private fun getPath(segs: List<Seg>): GeneralPath {
        val path = GeneralPath()
        for (seg in segs) {
            path.moveTo(seg.from.x.toFloat(), seg.from.y.toFloat())
            path.lineTo(seg.to.x.toFloat(), seg.to.y.toFloat())
        }
        return path
    }

    private fun drawLine(segs: List<Seg>) {
        g2d.draw(getPath(segs))
    }

    private fun drawConnectorArrow(hitX: Int?, hitY: Int?): Boolean {
        val xs = display.xs
        val ys = display.ys
        val p = 12
        var slope = 0f
        var x = 0
        var y = 0
        if (display.type == LinkType.Straight) {
            val p2 = xs.size - 1
            val p1 = p2 - 1
            x = xs[p2]
            y = ys[p2]
            slope = calcs.calcSlope(xs[p1], ys[p1], xs[p2], ys[p2])
        }
        else if (xs.size == 2) {
            // auto ELBOW/ELBOWH/ELBOWV type
            when (getAutoElbowLinkType()) {
                AutoElbowLinkType.AutoLinkV,
                AutoElbowLinkType.AutoLinkVHV,
                AutoElbowLinkType.AutoLinkHV -> {
                    x = xs[1]
                    y = if (ys[1] > ys[0]) (ys[1] + GAP) else (ys[1] - GAP)
                    slope = if (ys[1] > ys[0]) (Math.PI / 2).toFloat() else (Math.PI * 1.5).toFloat()
                }
                AutoElbowLinkType.AutoLinkH,
                AutoElbowLinkType.AutoLinkHVH,
                AutoElbowLinkType.AutoLinkVH -> {
                    x = if (xs[1] > xs[0]) (xs[1] + GAP) else (xs[1] - GAP)
                    y = ys[1]
                    slope = if (xs[1] > xs[0]) 0f else Math.PI.toFloat()
                }
            }
        }
        else {
            // ELBOW/ELBOWH/ELBOWV, control points > 2
            val k = xs.size - 1
            if (xs[k] == xs[k-1] && (ys[k] != ys[k-1] || ys[k-1] == ys[k-2])) {
                // verticle arrow
                x = xs[k]
                y = if (ys[k] > ys[k-1]) (ys[k] + GAP) else (ys[k] - GAP)
                slope = if (ys[k] > ys[k-1]) (Math.PI / 2).toFloat() else (Math.PI * 1.5).toFloat()
            }
            else {
                x = if (xs[k] > xs[k-1]) (xs[k] + GAP) else (xs[k] - GAP)
                y = ys[k]
                slope = if (xs[k] > xs[k-1]) 0f else Math.PI.toFloat()
            }
        }
        // apply point and slope to polygon
        val dl = slope - 2.7052  // 25 degrees
        val dr = slope + 2.7052  // 25 degrees

        val path = GeneralPath()
        path.moveTo(x.toFloat(), y.toFloat())
        path.lineTo(Math.cos(dl) * p + x, Math.sin(dl) * p + y)
        path.lineTo(Math.cos(dr) * p + x, Math.sin(dr) * p + y)
        path.lineTo(x.toFloat(), y.toFloat())
        if (hitX != null && hitY != null) {
            return path.contains(hitX.toDouble(), hitY.toDouble())
        }
        else {
            g2d.fill(path)
        }

        return false
    }

    class Calcs(val display: LinkDisplay) {

        fun calc(points: Int? = null, from: Step, to: Step) {
            calc(points, from.display, to.display)
        }

        fun calc(points: Int? = null, fromDisplay: Display, toDisplay: Display) {
            val type = display.type
            var xs = display.xs
            var ys = display.ys
            val x1 = fromDisplay.x
            val y1 = fromDisplay.y
            val w1 = fromDisplay.w
            val h1 = fromDisplay.h
            val x2 = toDisplay.x
            val y2 = toDisplay.y
            val w2 = toDisplay.w
            val h2 = toDisplay.h
            val n = if (points != null) points else (if (xs.size < 2) 2 else xs.size)

            if (type == Link.LinkType.Straight) {

                xs = mutableListOf<Int>()
                display.xs = xs
                ys = mutableListOf<Int>()
                display.ys = ys

                for (i in 0 until n) {
                    xs.add(0)
                    ys.add(0)
                }

                if (Math.abs(x1 - x2) >= Math.abs(y1 - y2)) {
                    // more of a horizontal link
                    xs[0] = if (x1 <= x2) (x1 + w1) else x1
                    ys[0] = y1 + h1 / 2
                    xs[n-1] = if (x1 <= x2) x2 else (x2 + w2)
                    ys[n-1] = y2 + h2 / 2
                    for (i in 1 until n-1) {
                        if (i % 2 != 0) {
                            ys[i] = ys[i-1]
                            xs[i] = (xs[n-1] -xs[0]) * ((i + 1) / 2) / (n / 2) + xs[0]
                        }
                        else {
                            xs[i] = xs[i-1]
                            ys[i] = (ys[n-1] - ys[0]) * ((i + 1) / 2) / ((n - 1) / 2) + ys[0]
                        }
                    }
                }
                else {
                    // more of a vertical link
                    xs[0] = x1 + w1 / 2
                    ys[0] = if (y1 <= y2) (y1 + h1) else y1
                    xs[n-1] = x2 + w2 / 2
                    ys[n-1] = if (y1 <= y2) y2 else (y2 + h2)
                    for (i in 1 until n-1) {
                        if (i % 2 != 0) {
                            xs[i] = xs[i-1]
                            ys[i] = (ys[n-1] - ys[0]) * ((i + 1) / 2) / (n / 2) + ys[0]
                        }
                        else {
                            ys[i] = ys[i-1]
                            xs[i] = (xs[n-1] - xs[0]) * (i / 2) / ((n - 1) / 2) + xs[0]
                        }
                    }
                }
            }
            else if (n == 2) {
                // auto ELBOW, ELBOWH, ELBOWV
                xs = mutableListOf(0, 0)
                display.xs = xs
                ys = mutableListOf(0, 0)
                display.ys = ys
                calcAutoElbow(fromDisplay, toDisplay)
            }
            else {
                // ELBOW, ELBOWH, ELBOWV with middle control points
                val horizontalFirst = type == Link.LinkType.ElbowH || (type == Link.LinkType.Elbow && Math.abs(x1 - x2) >= Math.abs(y1 - y2))
                val evenN = n % 2 == 0
                val horizontalLast = (horizontalFirst && evenN) || (!horizontalFirst && !evenN)
                xs = mutableListOf<Int>()
                display.xs = xs
                ys = mutableListOf<Int>()
                display.ys = ys
                for (i in 0 until n) {
                    xs.add(0)
                    ys.add(0)
                }
                if (horizontalFirst) {
                    xs[0] = if (x1 <= x2) (x1 + w1) else x1
                    ys[0] = y1 + h1 / 2
                }
                else {
                    xs[0] = x1 + w1 / 2
                    ys[0] = if (y1 <= y2) (y1 + h1) else y1
                }
                if (horizontalLast) {
                    xs[n-1] = if (x2 <= x1) (x2 + w2) else x2
                    ys[n-1] = y2 + h2 / 2
                }
                else {
                    xs[n-1] = x2 + w2 / 2
                    ys[n-1] = if (y2 <= y1) (y2 + h2) else y2
                }
                if (horizontalFirst) {
                    for (i in 1 until n-1) {
                        if (i % 2 != 0) {
                            ys[i] = ys[i-1]
                            xs[i] = ((xs[n-1] - xs[0]) * Math.round(((i + 1) / 2).toFloat() / (n / 2).toFloat()) + xs[0])
                        }
                        else {
                            xs[i] = xs[i-1]
                            ys[i] = ((ys[n-1] - ys[0]) * Math.round(((i + 1) / 2).toFloat() / ((n - 1) / 2).toFloat()) + ys[0])
                        }
                    }
                }
                else {
                    for (i in 1 until n-1) {
                        if (i % 2 != 0) {
                            xs[i] = xs[i-1]
                            ys[i] = (ys[n-1] - ys[0]) * Math.round(((i + 1) / 2).toFloat() / (n / 2).toFloat()) + ys[0]
                        }
                        else {
                            ys[i] = ys[i-1]
                            xs[i] = (xs[n-1] - xs[0]) * Math.round((i / 2).toFloat() / ((n - 1).toFloat() / 2)) + xs[0]
                        }
                    }
                }
            }
            calcLabel(fromDisplay, toDisplay)
        }

        fun recalc(step: Step, from: Step, to: Step) {
            val type = display.type
            val xs = display.xs
            val ys = display.ys

            val n = xs.size
            val k: Int

            if (type == Link.LinkType.Straight) {
                if (n == 2) {
                    calc(null, from.display, to.display)
                }
                else {
                    if (step == from) {
                        if (xs[1] > step.display.x + step.display.w + Link.GAP) {
                            xs[0] = step.display.x + step.display.w + Link.GAP
                        }
                        else if (xs[1] < step.display.x - Link.GAP) {
                            xs[0] = step.display.x - Link.GAP
                        }
                        if (ys[1] > step.display.y + step.display.h + Link.GAP) {
                            ys[0] = step.display.y + step.display.h + Link.GAP
                        }
                        else if (ys[1] < step.display.y - Link.GAP) {
                            ys[0] = step.display.y - Link.GAP
                        }
                    }
                    else {
                        k = n - 1
                        if (xs[k-1] > step.display.x + step.display.w + Link.GAP) {
                            xs[k] = step.display.x + step.display.w + Link.GAP
                        }
                        else if (xs[k-1] < step.display.x - Link.GAP) {
                            xs[k] = step.display.x - Link.GAP
                        }
                        if (ys[k-1] > step.display.y + step.display.h + Link.GAP) {
                            ys[k] = step.display.y + step.display.h + Link.GAP
                        }
                        else if (ys[k-1] < step.display.y - Link.GAP) {
                            ys[k] = step.display.y - Link.GAP
                        }
                    }
                }
            }
            else if (n == 2) {
                // automatic ELBOW, ELBOWH, ELBOWV
                calcAutoElbow(from.display, to.display)
            }
            else {
                // controlled ELBOW, ELBOWH, ELBOWV
                val wasHorizontal = !isHorizontal(0)
                val horizontalFirst = (Math.abs(from.display.x - to.display.x) >= Math.abs(from.display.y - to.display.y))
                if (type == Link.LinkType.Elbow && wasHorizontal != horizontalFirst) {
                    calc(null, from, to)
                }
                else if (step == from) {
                    if (xs[1] > step.display.x + step.display.w) {
                        xs[0] = step.display.x + step.display.w + Link.GAP
                    }
                    else if (xs[1] < step.display.x) {
                        xs[0] = step.display.x - Link.GAP
                    }
                    else {
                        xs[0] = xs[1]
                    }

                    if (ys[1] > step.display.y + step.display.h) {
                        ys[0] = step.display.y + step.display.h + Link.GAP
                    }
                    else if (ys[1] < step.display.y) {
                        ys[0] = step.display.y - Link.GAP
                    }
                    else {
                        ys[0] = ys[1]
                    }

                    if (wasHorizontal) {
                        ys[1] = ys[0]
                    }
                    else {
                        xs[1] = xs[0]
                    }
                }
                else {
                    k = n - 1
                    if (xs[k-1] > step.display.x + step.display.w) {
                        xs[k] = step.display.x + step.display.w + Link.GAP
                    }
                    else if (xs[k-1] < step.display.x) {
                        xs[k] = step.display.x - Link.GAP
                    }
                    else {
                        xs[k] = xs[k - 1]
                    }

                    if (ys[k-1] > step.display.y + step.display.h) {
                        ys[k] = step.display.y + step.display.h + Link.GAP
                    }
                    else if (ys[k-1] < step.display.y) {
                        ys[k] = step.display.y - Link.GAP
                    }
                    else {
                        ys[k] = ys[k - 1]
                    }

                    if ((wasHorizontal && n % 2 == 0) || (!wasHorizontal && n % 2 != 0)) {
                        ys[k - 1] = ys[k]
                    }
                    else {
                        xs[k - 1] = xs[k]
                    }
                }
            }

            calcLabel(from.display, to.display)
        }

        fun calcAutoElbow(fromDisplay: Display, toDisplay: Display) {
            val type = display.type
            val xs = display.xs
            val ys = display.ys

            if (toDisplay.x + toDisplay.w >= fromDisplay.x && toDisplay.x <= fromDisplay.x + fromDisplay.w) {
                // V
                xs[0] = (Math.max(fromDisplay.x, toDisplay.x) + Math.min(fromDisplay.x + fromDisplay.w, toDisplay.x + toDisplay.w)) / 2
                xs[1] = xs[0]
                if (toDisplay.y > fromDisplay.y) {
                    ys[0] = fromDisplay.y + fromDisplay.h + Link.GAP
                    ys[1] = toDisplay.y - Link.GAP
                }
                else {
                    ys[0] = fromDisplay.y - Link.GAP
                    ys[1] = toDisplay.y + toDisplay.h + Link.GAP
                }
            }
            else if (toDisplay.y + toDisplay.h>= fromDisplay.y && toDisplay.y <= fromDisplay.y + fromDisplay.h) {
                // H
                ys[0] = (Math.max(fromDisplay.y, toDisplay.y) + Math.min(fromDisplay.y + fromDisplay.h, toDisplay.y + toDisplay.h)) / 2
                ys[1] = ys[0]
                if (toDisplay.x > fromDisplay.x) {
                    xs[0] = fromDisplay.x + fromDisplay.w + Link.GAP
                    xs[1] = toDisplay.x - Link.GAP
                }
                else {
                    xs[0] = fromDisplay.x - Link.GAP
                    xs[1] = toDisplay.x + toDisplay.w + Link.GAP
                }
            }
            else if ((type == Link.LinkType.Elbow && Math.abs(toDisplay.x - fromDisplay.x) < Math.abs(toDisplay.y - fromDisplay.y) * Link.ELBOW_THRESHOLD) ||
                    (type == Link.LinkType.ElbowV && Math.abs(toDisplay.y - fromDisplay.y) > Link.ELBOW_VH_THRESHOLD)) {
                // VHV
                xs[0] = fromDisplay.x + fromDisplay.w / 2
                xs[1] = toDisplay.x + toDisplay.w / 2
                if (toDisplay.y > fromDisplay.y) {
                    ys[0] = fromDisplay.y + fromDisplay.h + Link.GAP
                    ys[1] = toDisplay.y - Link.GAP
                }
                else {
                    ys[0] = fromDisplay.y - Link.GAP
                    ys[1] = toDisplay.y + toDisplay.h + Link.GAP
                }
            }
            else if ((type == Link.LinkType.Elbow && Math.abs(toDisplay.y - fromDisplay.y) < Math.abs(toDisplay.x - fromDisplay.x) * Link.ELBOW_THRESHOLD) ||
                    (type == Link.LinkType.ElbowH && Math.abs(toDisplay.x - fromDisplay.x) > Link.ELBOW_VH_THRESHOLD)) {
                // HVH
                ys[0] = fromDisplay.y + fromDisplay.h / 2
                ys[1] = toDisplay.y + toDisplay.h / 2
                if (toDisplay.x > fromDisplay.x) {
                    xs[0] = fromDisplay.x + fromDisplay.w + Link.GAP
                    xs[1] = toDisplay.x - Link.GAP
                }
                else {
                    xs[0] = fromDisplay.x - Link.GAP
                    xs[1] = toDisplay.x + toDisplay.w + Link.GAP
                }
            }
            else if (type == Link.LinkType.ElbowV) {
                // VH
                if (toDisplay.y > fromDisplay.y) {
                    ys[0] = fromDisplay.y + fromDisplay.h + Link.GAP
                }
                else {
                    ys[0] = fromDisplay.y - Link.GAP
                }
                xs[0] = fromDisplay.x + fromDisplay.w / 2
                ys[1] = toDisplay.y + toDisplay.h / 2
                if (toDisplay.x > fromDisplay.x) {
                    xs[1] = toDisplay.x - Link.GAP
                }
                else {
                    xs[1] = toDisplay.x + toDisplay.w + Link.GAP
                }
            }
            else {
                // HV
                if (toDisplay.x > fromDisplay.x) {
                    xs[0] = fromDisplay.x + fromDisplay.w + Link.GAP
                }
                else {
                    xs[0] = fromDisplay.x - Link.GAP
                }
                ys[0] = fromDisplay.y + fromDisplay.h / 2
                xs[1] = toDisplay.x + toDisplay.w / 2
                if (toDisplay.y > fromDisplay.y) {
                    ys[1] = toDisplay.y - Link.GAP
                }
                else {
                    ys[1] = toDisplay.y + toDisplay.h + Link.GAP
                }
            }
        }

        fun calcLabel(fromDisplay: Display, toDisplay: Display) {
            val type = display.type
            val xs = display.xs
            val ys = display.ys
            val x1 = fromDisplay.x
            val x2 = toDisplay.x
            val n = xs.size

            if (type == Link.LinkType.Straight) {
                display.lx = (xs[0] + xs[n-1]) / 2
                display.ly = (ys[0] + ys[n-1]) / 2
            }
            else if (n == 2) {
                // auto ELBOW, ELBOWH, ELBOWV
                display.lx = (xs[0] + xs[n-1]) / 2
                display.ly = (ys[0] + ys[n-1]) / 2
            }
            else {
                // ELBOW, ELBOWH, ELBOWV with middle control points
                val horizontalFirst = ys[0] == ys[1]
                if (n <= 3) {
                    if (horizontalFirst) {
                        display.lx = (x1 + x2) / 2 - 40
                        display.ly = ys[0] - 4
                    }
                    else {
                        display.lx = xs[0] + 2
                        display.ly = (ys[0] + ys[1]) / 2
                    }
                }
                else {
                    if (horizontalFirst) {
                        display.lx = if (x1 <= x2) (xs[(n-1) / 2] + 2) else (xs[(n-1)/2 + 1] + 2)
                        display.ly = ys[n/2] - 4
                    }
                    else {
                        display.lx = if (x1 <= x2) xs[n/2 - 1] else xs[n/2]
                        display.ly = ys[n/2 - 1] - 4
                    }
                }
            }
        }

        fun isHorizontal(anchor: Int): Boolean {
            val p = anchor - 1
            val n = anchor + 1
            if (p >= 0 && display.xs[p] != display.xs[anchor] && display.ys[p] == display.ys[anchor]) {
                return true
            }
            else {
                return (n < display.xs.size && display.xs[n] == display.xs[anchor] && display.ys[n] != display.ys[anchor])
            }
        }

        fun calcSlope(x1: Int, y1: Int, x2: Int, y2: Int): Float {
            var slope: Float
            if (x1 == x2) {
                slope = if (y1 < y2) (Math.PI / 2).toFloat() else (-Math.PI / 2).toFloat()
            }
            else {
                slope = Math.atan((y2 - y1).toDouble()/(x2 - x1)).toFloat()
                if (x1 > x2) {
                    if (slope > 0)
                        slope -= Math.PI.toFloat()
                    else
                        slope += Math.PI.toFloat()
                }
            }
            return slope
        }

        private fun distance(x1: Int, x2: Int, y1: Int, y2: Int): Float {
            return Math.sqrt(((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1)).toDouble()).toFloat()
        }

    }

    companion object {
        const val CORR = 3 // offset for link start points
        const val CR = 8
        const val GAP = 4
        const val LABEL_CORR = -2

        const val ELBOW_THRESHOLD = 0.8
        const val ELBOW_VH_THRESHOLD = 60

        val LINK_STROKE = Display.LINE_STROKE
        const val HIT_PAD = 4.toDouble()
    }

    enum class LinkType {
        Straight,
        Elbow,
        ElbowH,
        ElbowV
    }

    enum class AutoElbowLinkType(val i: Int) {
        AutoLinkH(1),
        AutoLinkV(2),
        AutoLinkHV(3),
        AutoLinkVH(4),
        AutoLinkHVH(5),
        AutoLinkVHV(6)
    }

    enum class EventColor(val color: Color) {
        START(Color(0x006400)),
        RESUME(Color(0x006400)),
        DELAY(Color(0xFFA500)),
        HOLD(Color(0xFFA500)),
        ERROR(Color(0xf44336)),
        ABORT(Color(0xf44336)),
        CORRECT(Color(0x800080)),
        FINISH(Color(0x808080))
    }
}