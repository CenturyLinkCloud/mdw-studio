package com.centurylink.mdw.studio.edit

import com.centurylink.mdw.studio.draw.Display
import com.centurylink.mdw.studio.draw.Drawable

class Selection(selectObj: Drawable) {

    var selectObjs = mutableListOf<Drawable>()
    var anchor: Int? = null
    /**
     * a line to intended destination
     */
    var destination: Display? = null

    /**
     * a box around a group of selectObjs
     */
    var marquee: Display? = null

    init {
        selectObjs.add(selectObj)
    }

    var selectObj: Drawable
        get() = selectObjs.first()
        set(obj) {
            selectObjs.clear()
            selectObjs.add(obj)
            destination = null
        }

    fun includes(obj: Drawable): Boolean {
        return selectObjs.find { it.workflowObj.id == obj.workflowObj.id } != null
    }

    fun add(obj: Drawable) {
        selectObjs.add(obj)
    }

    fun remove(obj: Drawable) {
        selectObjs.remove(obj)
    }

    fun isMulti(): Boolean {
        return selectObjs.size > 1
    }
}

interface SelectListener {
    fun onSelect(selectObjs: List<Drawable>)
}

interface Selectable {
    var isSelected: Boolean
    fun select() {
        isSelected = true
    }
}

class Select : Selectable {
    override var isSelected = false
}