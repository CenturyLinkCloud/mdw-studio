package com.centurylink.mdw.draw.edit

import com.centurylink.mdw.constant.WorkAttributeConstant
import com.centurylink.mdw.draw.*
import com.centurylink.mdw.draw.ext.maxActivityId
import com.centurylink.mdw.draw.ext.maxSubprocessId
import com.centurylink.mdw.draw.ext.maxTextNoteId
import com.centurylink.mdw.draw.ext.maxTransitionId
import com.centurylink.mdw.drawio.MxGraphParser
import com.centurylink.mdw.model.workflow.*
import org.json.JSONArray
import org.json.JSONObject

class SelectionBuilder(private val diagram: Diagram) {

    private var selection: Selection? = null
    private fun addToSelection(drawable: Drawable) {
        selection?.let {
            it.add(drawable)
        }
        if (selection == null) {
            selection = Selection(drawable)
        }
    }

    /**
     * Assigns new ids based on destination process.
     * Does not add workflow objects to destination process.
     */
    fun fromJson(json: JSONObject): Selection? {
        val process = diagram.process
        val selectObjJson = json.getJSONObject("mdw.selection")
        val activitiesArr = selectObjJson.optJSONArray("activities")
        val transitions = mutableListOf<Transition>()
        val activityIds = mutableMapOf<Long,Long>()
        activitiesArr?.let {
            var activityId = process.maxActivityId()
            var transitionId = process.maxTransitionId()
            for (i in 0 until activitiesArr.length()) {
                activityId++
                val activityJson = activitiesArr.getJSONObject(i)
                val activity = Activity(activityJson)
                activityIds[activity.id] = activityId
                val implementor = diagram.implementors[activity.implementor] ?: ActivityImplementor(activity.implementor)
                activity.id = activityId
                activity.setAttribute(WorkAttributeConstant.LOGICAL_ID, "A$activityId")
                val step = Step(diagram.g2d, diagram.project, process, activity, implementor)
                addToSelection(step)
                activityJson.optJSONArray("transitions")?.let {
                    for (j in 0 until it.length()) {
                        transitionId++
                        val transition = Transition(it.getJSONObject(j))
                        transition.id = transitionId
                        transition.setAttribute(WorkAttributeConstant.LOGICAL_ID, "T$transitionId")
                        transition.fromId = activityId
                        transitions.add(transition)
                    }
                }
            }
        }
        for (transition in transitions) {
            transition.toId = activityIds[transition.toId]
            val fromStep = selection?.selectObjs?.find {
                it is Step && it.activity.id == transition.fromId
            }
            val toStep = selection?.selectObjs?.find {
                it is Step && it.activity.id == transition.toId
            }
            if (fromStep != null && toStep != null) {
                val link = Link(diagram.g2d, diagram.project, diagram.process, transition, fromStep as Step, toStep as Step)
                addToSelection(link)
            }
        }

        selectObjJson.optJSONArray("subprocesses")?.let {
            var subprocessId = process.maxSubprocessId()
            for (i in 0 until it.length()) {
                subprocessId++
                val subprocess = Process(it.getJSONObject(i))
                subprocess.id = subprocessId
                subprocess.setAttribute(WorkAttributeConstant.LOGICAL_ID, "P$subprocessId")
                val subflow = Subflow(diagram.g2d, diagram.project, diagram.process, subprocess, diagram.implementors)
                addToSelection(subflow)
            }
        }

        selectObjJson.optJSONArray("textNotes")?.let {
            var textNoteId = process.maxTextNoteId()
            for (i in 0 until it.length()) {
                textNoteId++
                val textNote = TextNote(it.getJSONObject(i))
                textNote.logicalId = "N$textNoteId"
                val note = Note(diagram.g2d, diagram.project, diagram.process, textNote)
                addToSelection(note)
            }
        }
        return selection
    }

    fun fromGraph(graph: MxGraphParser): Selection? {
        val json = JSONObject()
        val selectionJson = JSONObject()
        json.put("mdw.selection", selectionJson)
        val process = Process()
        process.activities = graph.activities
        process.transitions = graph.transitions
        if (!graph.textNotes.isEmpty()) {
            process.textNotes = graph.textNotes
        }
        val processJson = process.json
        selectionJson.put("activities", processJson.getJSONArray("activities"))
        if (processJson.has("textNotes")) {
            selectionJson.put("textNotes", processJson.getJSONArray("textNotes"))
        }
        return fromJson(json)
    }

    /**
     * Finds activity outbound transitions from the diagram's process
     */
    fun toJson(selection: Selection): JSONObject {
        val json = JSONObject()
        val selObjJson = JSONObject()
        json.put("mdw.selection", selObjJson)
        for (selObj in selection.selectObjs) {
            when (selObj) {
                is Step -> {
                    var activitiesArr = selObjJson.optJSONArray("activities")
                    if (activitiesArr == null) {
                        activitiesArr = JSONArray()
                        selObjJson.put("activities", activitiesArr)
                    }
                    val activityJson = JSONObject(selObj.workflowObj.obj.toString())
                    val outboundTransitions = diagram.process.getAllTransitions(selObj.activity.id)
                    outboundTransitions?.let {
                        if (!outboundTransitions.isEmpty()) {
                            val transitionsArr = JSONArray()
                            for (transition in outboundTransitions) {
                                val transitionJson = transition.json
                                if (transition.toId < 0) { // newly created
                                    transitionJson.put("to", diagram.process.getActivityVO(transition.toId).logicalId)
                                }
                                transitionsArr.put(transitionJson)
                            }
                            activityJson.put("transitions", transitionsArr)
                        }
                    }
                    activitiesArr.put(activityJson)
                }
                is Subflow -> {
                    // TODO
                }
                is Note -> {
                    var textNotesArr = selObjJson.optJSONArray("textNotes")
                    if (textNotesArr == null) {
                        textNotesArr = JSONArray()
                        selObjJson.put("textNotes", textNotesArr)
                    }
                    textNotesArr.put(JSONObject(selObj.workflowObj.obj.toString()))
                }
            }
        }
        return json
    }
}