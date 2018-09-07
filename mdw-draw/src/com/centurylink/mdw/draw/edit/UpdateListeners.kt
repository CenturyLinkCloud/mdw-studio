package com.centurylink.mdw.draw.edit

typealias UpdateListener = (obj: WorkflowObj) -> Unit

interface UpdateListeners {
    fun addUpdateListener(listener: UpdateListener)
    fun removeUpdateListener(listener: UpdateListener)
    fun notifyUpdateListeners(obj: WorkflowObj)
}

class UpdateListenersDelegate : UpdateListeners {

    private val updateListeners = mutableListOf<UpdateListener>()

    override fun addUpdateListener(listener: UpdateListener) {
        updateListeners.add(listener)
    }

    override fun removeUpdateListener(listener: UpdateListener) {
        updateListeners.remove(listener)
    }

    override fun notifyUpdateListeners(obj: WorkflowObj) {
        for (listener in updateListeners) {
            listener(obj)
        }
    }
}