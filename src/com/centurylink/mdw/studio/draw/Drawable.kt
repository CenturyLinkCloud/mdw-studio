package com.centurylink.mdw.studio.draw

import com.centurylink.mdw.studio.edit.Selectable
import com.centurylink.mdw.studio.edit.WorkflowObj

/**
 * Drawable, selectable item on canvas
 */
interface Drawable {

    val workflowObj: WorkflowObj

    /**
     * returns display extents
     */
    fun draw(): Display

    fun move(deltaX: Int, deltaY: Int, limits: Display? = null) { }

    fun isHover(x: Int, y: Int): Boolean {
        return false
    }

    fun getAnchor(x: Int, y: Int): Int? {
        return null
    }
}

interface Resizable {
    fun resize(anchor: Int, x: Int, y: Int, deltaX: Int, deltaY: Int, limits: Display? = null)
}