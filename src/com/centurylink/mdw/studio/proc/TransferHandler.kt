package com.centurylink.mdw.studio.proc

import com.centurylink.mdw.constant.ProcessVisibilityConstant.*
import com.centurylink.mdw.constant.WorkAttributeConstant.EMBEDDED_PROCESS_TYPE
import com.centurylink.mdw.studio.draw.Diagram
import com.centurylink.mdw.studio.draw.DiagramEvent
import com.centurylink.mdw.studio.edit.UpdateListeners
import com.centurylink.mdw.studio.edit.UpdateListenersDelegate
import com.centurylink.mdw.studio.proc.CanvasActions.Companion.DATA_FLAVOR_JSON
import com.centurylink.mdw.studio.proj.Implementor
import org.json.JSONObject

/**
 * Drag/drop handler for activity implementors from toolbox.
 */
class TransferHandler(private val diagram: Diagram) : javax.swing.TransferHandler(),
        UpdateListeners by UpdateListenersDelegate() {

    override fun canImport(support: TransferSupport): Boolean {
        if (support.transferable.isDataFlavorSupported(DATA_FLAVOR_JSON)) {
            val data = JSONObject(support.transferable.getTransferData(DATA_FLAVOR_JSON).toString())
            val impl = getImplementor(data)
            if (impl != null) {
                if (impl.category == "subflow") {
                    diagram.process.subprocesses?.let {
                        for (subproc in it) {
                            val type = subproc.getAttribute(EMBEDDED_PROCESS_TYPE)
                            if (type == EMBEDDED_DELAY_PROCESS && impl.label.startsWith("Delay")) {
                                return false
                            }
                            else if (type == EMBEDDED_ERROR_PROCESS && impl.label.startsWith("Exception")) {
                                return false
                            }
                            else if (type == EMBEDDED_ABORT_PROCESS && impl.label.startsWith("Cancel")) {
                                return false
                            }
                        }
                    }
                }
                return true
            }
        }
        return false
    }

    override fun importData(support: TransferSupport): Boolean {
        if (support.transferable.isDataFlavorSupported(DATA_FLAVOR_JSON)) {
            val data = JSONObject(support.transferable.getTransferData(DATA_FLAVOR_JSON).toString())
            val impl = getImplementor(data)
            if (impl != null) {
                val dropPoint = support.dropLocation.dropPoint
                val x = maxOf(dropPoint.x - 60, 0)
                val y = maxOf(dropPoint.y - 30, 0)
                diagram.onDrop(DiagramEvent(x, y), impl)
                notifyUpdateListeners(diagram.workflowObj)
                return true;
            }
        }
        return false
    }

    private fun getImplementor(json: JSONObject): Implementor? {
        json.optString("mdw.implementor")?.let { impl ->
            return diagram.implementors[impl]
        }
        return null
    }
}