package com.centurylink.mdw.draw.edit.apply

import com.centurylink.mdw.constant.WorkAttributeConstant.WORK_DISPLAY_INFO
import com.centurylink.mdw.constant.WorkTransitionAttributeConstant.TRANSITION_DISPLAY_INFO
import com.centurylink.mdw.draw.Display
import com.centurylink.mdw.draw.Link
import com.centurylink.mdw.draw.LinkDisplay
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.model.workflow.Process

@Suppress("unused")
class TransitionDisplayApplier : AttributeApplier() {
    override fun init(widget: Pagelet.Widget, workflowObj: WorkflowObj) {
        super.init(widget, workflowObj)
        when (widget.name) {
            "_linkType" -> {
                widget.value = LinkDisplay(workflowObj.getAttribute("TRANSITION_DISPLAY_INFO")).type.toString()
            }
            "_controlPoints" -> {
                widget.value = LinkDisplay(workflowObj.getAttribute("TRANSITION_DISPLAY_INFO")).xs.size
            }
        }
    }

    override fun update() {
        when (widget.name) {
            "_linkType" -> {
                val linkDisplay = LinkDisplay(workflowObj.getAttribute("TRANSITION_DISPLAY_INFO"))
                linkDisplay.type = if (widget.value == "") {
                    Link.LinkType.Elbow
                } else {
                    Link.LinkType.valueOf(widget.value.toString())
                }
                calc(linkDisplay, linkDisplay.xs.size)
                workflowObj.setAttribute(TRANSITION_DISPLAY_INFO, linkDisplay.toString())
            }
            "_controlPoints" -> {
                val linkDisplay = LinkDisplay(workflowObj.getAttribute(TRANSITION_DISPLAY_INFO))
                calc(linkDisplay, widget.value.toString().toInt())
                workflowObj.setAttribute(TRANSITION_DISPLAY_INFO, linkDisplay.toString())
            }
            else -> {
                super.update()
            }
        }
    }

    fun calc(linkDisplay: LinkDisplay, points: Int) {
        val transitionId = workflowObj.id.substring(1).toLong()
        val process = workflowObj.asset as Process
        var transition = process.getTransition(transitionId)
        if (transition == null) {
            for (subprocess in process.subprocesses) {
                transition = subprocess.getTransition(transitionId)
            }
        }
        transition?.let {
            val fromActivity = process.getActivityById("A${transition.fromId}")
            val toActivity = process.getActivityById("A${transition.toId}")
            val fromDisplay = Display(fromActivity.getAttribute(WORK_DISPLAY_INFO))
            val toDisplay = Display(toActivity.getAttribute(WORK_DISPLAY_INFO))
            val calcs = Link.Calcs(linkDisplay)
            calcs.calc(points, fromDisplay, toDisplay)
        }
    }
}