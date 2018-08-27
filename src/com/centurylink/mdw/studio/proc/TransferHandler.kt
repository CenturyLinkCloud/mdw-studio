package com.centurylink.mdw.studio.proc

import com.centurylink.mdw.constant.ProcessVisibilityConstant.*
import com.centurylink.mdw.constant.WorkAttributeConstant.EMBEDDED_PROCESS_TYPE
import com.centurylink.mdw.drawio.MxGraphParser
import com.centurylink.mdw.studio.draw.Diagram
import com.centurylink.mdw.studio.draw.DiagramEvent
import com.centurylink.mdw.studio.edit.SelectionBuilder
import com.centurylink.mdw.studio.edit.UpdateListeners
import com.centurylink.mdw.studio.edit.UpdateListenersDelegate
import com.centurylink.mdw.studio.proj.Implementor
import com.centurylink.mdw.model.workflow.Process
import org.json.JSONArray
import org.json.JSONObject
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLDecoder
import java.util.*
import javax.swing.JComponent

class TransferHandler(private val diagram: Diagram) : javax.swing.TransferHandler(),
        UpdateListeners by UpdateListenersDelegate()  {

    override fun getSourceActions(c: JComponent): Int {
        if (diagram.hasSelection()) {
            return COPY_OR_MOVE
        }
        return NONE
    }

    override fun createTransferable(c: JComponent): Transferable {
        return if (diagram.hasSelection()) {
            JsonTransferable(SelectionBuilder(diagram).toJson(diagram.selection))
        }
        else {
            JsonTransferable(null)
        }
    }

    override fun canImport(support: TransferSupport): Boolean {
        if (support.transferable.isDataFlavorSupported(JSON_DATA_FLAVOR)) {
            val data = JSONObject(support.transferable.getTransferData(JSON_DATA_FLAVOR).toString())
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
            else {
                // selection copy/paste
                return data.optJSONObject("mdw.selection") != null
            }
        }
        return false
    }

    override fun importData(support: TransferSupport): Boolean {
        if (support.transferable.isDataFlavorSupported(JSON_DATA_FLAVOR)) {
            val data = JSONObject(support.transferable.getTransferData(JSON_DATA_FLAVOR).toString())
            val impl = getImplementor(data)
            if (impl != null) {
                val dropPoint = support.dropLocation.dropPoint
                val x = maxOf(dropPoint.x - 60, 0)
                val y = maxOf(dropPoint.y - 30, 0)
                diagram.onDrop(DiagramEvent(x, y), impl)
                notifyUpdateListeners(diagram.workflowObj)
            }
            else {
                // selection paste
                val paste = SelectionBuilder(diagram).fromJson(data)
                if (paste != null) {
                    diagram.onPaste(paste)
                    notifyUpdateListeners(diagram.workflowObj)
                }
            }
            return true;
        }
        else if (support.transferable.isDataFlavorSupported(TEXT_DATA_FLAVOR)) {
            val data = support.transferable.getTransferData(TEXT_DATA_FLAVOR);
            if (data is InputStream) {
                Scanner(data).use {
                    val text = URLDecoder.decode(it.next(), "UTF-8")
                    if (text.startsWith("<mxGraphModel>")) {
                        // paste from draw.io
                        val graph = MxGraphParser().read(ByteArrayInputStream(text.toByteArray()))
                        val paste = SelectionBuilder(diagram).fromGraph(graph)
                        if (paste != null) {
                            diagram.onPaste(paste)
                            notifyUpdateListeners(diagram.workflowObj)
                        }
                        return true;
                    }
                }
            }
        }
        return false
    }

    private fun getImplementor(json: JSONObject): Implementor? {
        json.optString("mdw.implementor")?.let {
            return diagram.implementors[it]
        }
        return null
    }

    companion object {
        val JSON_DATA_FLAVOR = DataFlavor("application/json")
        val TEXT_DATA_FLAVOR = DataFlavor("text/plain")
    }
}

class JsonTransferable(private val json: JSONObject?) : Transferable {

    override fun getTransferData(flavor: DataFlavor): Any {
        return json?.toString() ?: "{}"
    }
    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return flavor.isMimeTypeEqual(DataFlavor("application/json"))
    }
    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(DataFlavor("application/json"))
    }

}