package com.centurylink.mdw.draw.edit.apply

import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.model.workflow.Activity
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.monitor.MonitorAttributes

@Suppress("unused")
class ProcessAttributeApplier : AttributeApplier() {
    override fun init(widget: Pagelet.Widget, workflowObj: WorkflowObj) {
        super.init(widget, workflowObj)
        when (widget.name) {
            "_isService" -> {
                widget.value = workflowObj.getAttribute("PROCESS_VISIBILITY") == "SERVICE"
            }
            "_captureTimings" -> {
                var allActivitiesTimed = true
                val process = workflowObj.asset as Process
                var allActivities = mutableListOf<Activity>()
                allActivities.addAll(process.activities)
                process.subprocesses?.let { subprocs ->
                    for (subproc in subprocs) {
                        allActivities.addAll(subproc.activities)
                    }
                }
                for (activity in allActivities) {
                    val monitorsStr = activity.getAttribute("Monitors")
                    val timed = if (monitorsStr == null) {
                        false
                    }
                    else {
                        MonitorAttributes(monitorsStr).isEnabled("com.centurylink.mdw.base.ActivityTimingMonitor")
                    }
                    if (!timed) {
                        allActivitiesTimed = false
                        break
                    }
                }
                widget.value = allActivitiesTimed
            }
        }
    }

    override fun update() {
        when (widget.name) {
            "_isService" -> {
                workflowObj.setAttribute("PROCESS_VISIBILITY", if (widget.value == "true") "SERVICE" else "PUBLIC")
            }
            "_captureTimings" -> {
                val enabled = widget.value == "true"
                val attr = "[\"$enabled\",\"Activity Timing\",\"com.centurylink.mdw.base/ActivityTimingMonitor.java\",\"\"]"
                workflowObj.setAttribute("[activities]_Monitors", attr)
            }
        }
    }
}