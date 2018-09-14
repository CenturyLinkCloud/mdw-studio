package com.centurylink.mdw.draw.ext

import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.constant.WorkAttributeConstant.LOGICAL_ID
import com.centurylink.mdw.constant.WorkAttributeConstant.WORK_DISPLAY_INFO
import com.centurylink.mdw.constant.WorkTransitionAttributeConstant.TRANSITION_DISPLAY_INFO
import com.centurylink.mdw.draw.Display
import com.centurylink.mdw.draw.model.Implementor
import com.centurylink.mdw.draw.LinkDisplay
import com.centurylink.mdw.model.event.EventType
import com.centurylink.mdw.model.workflow.Activity
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.model.workflow.TextNote
import com.centurylink.mdw.model.workflow.Transition
import org.json.JSONObject

fun Process.maxActivityId(): Long {
    var maxAct: Activity? = null
    if (!activities.isEmpty()) {
        maxAct = activities.reduce { acc, act -> if (acc.id > act.id) acc else act }
    }
    subprocesses?.let {
        for (subprocess in subprocesses) {
            if (!subprocess.activities.isEmpty()) {
                val maxSubAct = subprocess.activities.reduce { acc, act -> if (acc.id > act.id) acc else act }
                if (maxAct == null || maxSubAct.id > maxAct!!.id) {
                    maxAct = maxSubAct
                }
            }
        }
    }
    return if (maxAct == null) 0 else maxAct!!.id
}

fun Process.addActivity(x: Int, y: Int, implementor: Implementor, boxed: Boolean = true): Activity {
    val activity = Activity()
    activity.id = maxActivityId() + 1
    activity.name = when (implementor.implementorClassName) {
        Implementor.START_IMPL -> "Start"
        Implementor.STOP_IMPL -> "Stop"
        else -> "New ${implementor.label}"
    }
    activity.implementor = implementor.implementorClassName
    activity.setAttribute(LOGICAL_ID, "A${activity.id}")
    var w = 24
    var h = 24
    if (boxed) {
        if (implementor.iconName?.startsWith("shape:") ?: false) {
            w = 60
            h = 40
        }
        else {
            w = 100
            h = 60
        }
    }
    activity.setAttribute(WORK_DISPLAY_INFO, Display(x, y, w, h).toString())
    activities.add(activity)
    return activity
}

fun Process.maxTransitionId(): Long {
    var maxTrans: Transition? = null
    if (!transitions.isEmpty()) {
        maxTrans = transitions.reduce { acc, trans -> if (acc.id > trans.id) acc else trans }
    }
    subprocesses?.let {
        for (subprocess in subprocesses) {
            if (!subprocess.transitions.isEmpty()) {
                val maxSubTrans = subprocess.transitions.reduce { acc, trans -> if (acc.id > trans.id) acc else trans }
                if (maxTrans == null || maxSubTrans.id > maxTrans!!.id) {
                    maxTrans = maxSubTrans
                }
            }
        }
    }
    return if (maxTrans == null) 0 else maxTrans!!.id
}

fun Process.addTransition(fromActivity: Activity, toActivity: Activity): Transition {
    val transition = Transition()
    transition.id = maxTransitionId() + 1
    transition.eventType = EventType.FINISH
    transition.fromId = fromActivity.id
    transition.toId = toActivity.id
    transition.setAttribute(LOGICAL_ID, "T${transition.id}")
    transitions.add(transition)
    return transition
}

fun Process.maxSubprocessId(): Long {
    var maxSubprocId = 0L
    subprocesses?.let {
        if (!subprocesses.isEmpty()) {
            var maxSubproc = subprocesses.reduce { acc, subproc ->
                if (acc.id > subproc.id) acc else subproc
            }
            maxSubprocId = maxOf(maxSubprocId, maxSubproc.id)
        }
    }
    return maxSubprocId
}

fun Process.addSubprocess(x: Int, y: Int, type: String): Process {

    val maxSubprocId = maxSubprocessId()

    val template = when(type) {
        "Cancellation Handler" -> "cancel.subproc"
        "Delay Handler" -> "delay.subproc"
        "Exception Handler" -> "exception.subproc"
        else -> type + ".subproc" // handle unforeseen types
    }

    val subprocJson = JSONObject(Templates.get("assets/" + template))
    val subprocess = Process(subprocJson)
    subprocess.id = maxSubprocId + 1

    var startActivityId = maxActivityId()
    for (activity in subprocess.activities) {
        activity.id += startActivityId
        activity.setAttribute(LOGICAL_ID, "A${activity.id}")
        val activityDisplay = Display(activity.getAttribute(WORK_DISPLAY_INFO))
        activityDisplay.x = activityDisplay.x + x
        activityDisplay.y = activityDisplay.y + y
        activity.setAttribute(WORK_DISPLAY_INFO, activityDisplay.toString())
    }
    var startTransitionId = maxTransitionId()
    for (transition in subprocess.transitions) {
        transition.id += startTransitionId
        transition.setAttribute(LOGICAL_ID, "T${transition.id}")
        val linkDisplay = LinkDisplay(transition.getAttribute(TRANSITION_DISPLAY_INFO))
        linkDisplay.lx = linkDisplay.lx + x
        linkDisplay.ly = linkDisplay.ly + y
        val xs = mutableListOf<Int>()
        for (ax in linkDisplay.xs) {
            xs.add(ax + x)
        }
        linkDisplay.xs = xs
        val ys = mutableListOf<Int>()
        for (ay in linkDisplay.ys) {
            ys.add(ay + y)
        }
        linkDisplay.ys = ys
        transition.setAttribute(TRANSITION_DISPLAY_INFO, linkDisplay.toString())
        transition.toId += startActivityId
        transition.fromId += startActivityId
    }

    subprocess.setAttribute(LOGICAL_ID, "P${subprocess.id}")
    val subprocDisplay = Display(subprocess.getAttribute(WORK_DISPLAY_INFO))
    subprocess.setAttribute(WORK_DISPLAY_INFO,
            Display(x, y, subprocDisplay.w, subprocDisplay.h).toString())
    subprocesses.add(subprocess)
    return subprocess
}

fun Process.maxTextNoteId(): Long {
    var maxId = 0L
    textNotes?.let {
        if (!textNotes.isEmpty()) {
            for (textNote in textNotes) {
                val textNoteId = textNote.logicalId.substring(1).toLong()
                if (textNoteId > maxId) {
                    maxId = textNoteId
                }
            }
        }
    }
    return maxId
}

fun Process.addTextNote(x: Int, y: Int): TextNote {
    val textNote = TextNote()
    textNote.logicalId = "N${maxTextNoteId() + 1}"
    textNote.content = ""
    textNote.setAttribute(LOGICAL_ID, textNote.logicalId)
    if (textNotes == null) {
        textNotes = mutableListOf()
    }
    textNote.setAttribute(WORK_DISPLAY_INFO, Display(x, y, 200, 60).toString())
    textNotes.add(textNote)
    return textNote
}

/**
 * Updates description, attributes and variables
 */
fun Process.set(process: Process) {
    description = process.description
    val displayInfo = getAttribute(WORK_DISPLAY_INFO)
    attributes = process.attributes
    setAttribute(WORK_DISPLAY_INFO, displayInfo)
    variables = process.variables
}

/**
 * Updates activity but preserves geometry.
 */
fun Process.setActivity(logicalId: String, activity: Activity) {
    val i = activities.findIndex { it.logicalId == logicalId }
    if (i >= 0) {
        activity.setAttribute(WORK_DISPLAY_INFO, activities[i].getAttribute(WORK_DISPLAY_INFO))
        activities.replaceAt(i, activity)
    }
    else {
        subprocesses?.let {
            for (subprocess in subprocesses) {
                val j = subprocess.activities.findIndex { it.logicalId == logicalId }
                if (j >= 0) {
                    activity.setAttribute(WORK_DISPLAY_INFO, subprocess.activities[j].getAttribute(WORK_DISPLAY_INFO))
                    subprocess.activities.replaceAt(j, activity)
                }
            }
        }
    }
}

fun Process.deleteActivity(logicalId: String) {
    val i = activities.findIndex { it.logicalId == logicalId }
    if (i >= 0) activities.removeAt(i)
}

fun Process.setTransition(logicalId: String, transition: Transition) {
    val i = transitions.findIndex { it.logicalId == logicalId }
    if (i >= 0) {
        transition.fromId = transitions[i].fromId
        transitions.replaceAt(i, transition)
    }
    else {
        subprocesses?.let {
            for (subprocess in subprocesses) {
                val j = subprocess.transitions.findIndex { it.logicalId == logicalId }
                if (j >= 0) {
                    transition.fromId = subprocess.transitions[j].fromId
                    subprocess.transitions.replaceAt(j, transition)
                }
            }
        }
    }
}

fun Process.deleteTransition(logicalId: String) {
    val i = transitions.findIndex { it.logicalId == logicalId }
    if (i >= 0) transitions.removeAt(i)
}

fun Process.setSubprocess(logicalId: String, subprocess: Process) {
    val i = subprocesses.findIndex { it.id == logicalId.substring(1).toLong() }
    if (i >= 0) {
        subprocess.setAttribute(WORK_DISPLAY_INFO, subprocesses[i].getAttribute(WORK_DISPLAY_INFO))
        subprocesses.replaceAt(i, subprocess)
    }
}

fun Process.deleteSubprocess(logicalId: String) {
    subprocesses?.let {
        val i = subprocesses.findIndex { it.id == logicalId.substring(1).toLong() }
        if (i >= 0) subprocesses.removeAt(i)
    }
}

fun Process.setTextNote(logicalId: String, textNote: TextNote) {
    val i = textNotes.findIndex { it.logicalId == logicalId }
    if (i >= 0) {
        textNote.setAttribute(WORK_DISPLAY_INFO, textNotes[i].getAttribute(WORK_DISPLAY_INFO))
        textNotes.replaceAt(i, textNote)
    }
}

fun Process.deleteTextNote(logicalId: String) {
    textNotes?.let {
        val i = textNotes.findIndex { it.logicalId == logicalId }
        if (i >= 0) textNotes.removeAt(i)
    }
}
