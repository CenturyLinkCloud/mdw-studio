package com.centurylink.mdw.studio.proc

import com.centurylink.mdw.constant.WorkAttributeConstant
import com.centurylink.mdw.draw.Diagram
import com.centurylink.mdw.draw.Step
import com.centurylink.mdw.drawio.MxGraphParser
import com.centurylink.mdw.draw.edit.SelectionBuilder
import com.centurylink.mdw.draw.edit.UpdateListeners
import com.centurylink.mdw.draw.edit.UpdateListenersDelegate
import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.CopyProvider
import com.intellij.ide.CutProvider
import com.intellij.ide.DeleteProvider
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import org.json.JSONObject
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLDecoder
import java.util.*

class CanvasActions(private val diagram: Diagram) : DeleteProvider, CutProvider, CopyProvider, PasteProvider,
        UpdateListeners by UpdateListenersDelegate() {

    override fun canDeleteElement(dataContext: DataContext): Boolean {
        return !diagram.isReadonly
    }
    override fun deleteElement(dataContext: DataContext) {
        if (diagram.hasSelection()) {
            diagram.onDelete()
            diagram.selection.selectObj = diagram
            notifyUpdateListeners(diagram.workflowObj)
        }
    }

    override fun isCutVisible(dataContext: DataContext): Boolean {
        return true
    }
    override fun isCutEnabled(dataContext: DataContext): Boolean {
        return !diagram.isReadonly && diagram.hasSelection()
    }
    override fun performCut(dataContext: DataContext) {
        performCopy(dataContext)
        deleteElement(dataContext)
    }

    override fun isCopyVisible(dataContext: DataContext): Boolean {
        return true
    }
    override fun isCopyEnabled(dataContext: DataContext): Boolean {
        return diagram.hasSelection()
    }
    override fun performCopy(dataContext: DataContext) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(JsonTransferable(SelectionBuilder(diagram).toJson(diagram.selection))) { _, _ -> }
    }

    override fun isPastePossible(dataContext: DataContext): Boolean {
        return isPasteEnabled(dataContext)
    }
    override fun isPasteEnabled(dataContext: DataContext): Boolean {
        if (diagram.isReadonly) {
            return false
        }
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        if (clipboard.isDataFlavorAvailable(DATA_FLAVOR_JSON)) {
            try {
                val jsonData = clipboard.getData(DATA_FLAVOR_JSON)
                if (jsonData is String && JSONObject(jsonData).optJSONObject("mdw.selection") != null) {
                    return true
                }
            }
            catch (e: Exception) {
                LOG.error(e)
            }
        }
        if (clipboard.isDataFlavorAvailable(DATA_FLAVOR_TEXT)) {
            try {
                val textData = clipboard.getData(DATA_FLAVOR_TEXT)
                if (textData is InputStream) {
                    Scanner(textData).use {
                        val text = URLDecoder.decode(it.next(), "UTF-8")
                        if (text.startsWith("<mxGraphModel>")) {
                            return true
                        }
                    }
                }
            }
            catch (e: Exception) {
                LOG.error(e)
            }
        }

        return false
    }
    override fun performPaste(dataContext: DataContext) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        if (clipboard.isDataFlavorAvailable(DATA_FLAVOR_JSON)) {
            val jsonData = clipboard.getData(DATA_FLAVOR_JSON)
            if (jsonData is String) {
                @Suppress("USELESS_CAST")
                val selection = SelectionBuilder(diagram).fromJson(JSONObject(jsonData as String))
                if (selection != null) {
                    diagram.onPaste(selection)
                    notifyUpdateListeners(diagram.workflowObj)
                }
            }
        }
        else if (clipboard.isDataFlavorAvailable(DATA_FLAVOR_TEXT)) {
            val textData = clipboard.getData(DATA_FLAVOR_TEXT)
            if (textData is InputStream) {
                Scanner(textData).use {
                    val text = URLDecoder.decode(it.next(), "UTF-8")
                    if (text.startsWith("<mxGraphModel>")) {
                        // paste from draw.io
                        val graph = MxGraphParser().read(ByteArrayInputStream(text.toByteArray()))
                            val paste = SelectionBuilder(diagram).fromGraph(graph)
                            if (paste != null) {
                                diagram.onPaste(paste)
                                notifyUpdateListeners(diagram.workflowObj)
                            }
                    }
                }
            }
        }
    }

    companion object {
        val LOG = Logger.getInstance(CanvasActions::class.java)

        val DATA_FLAVOR_JSON = DataFlavor("application/json")
        val DATA_FLAVOR_TEXT = DataFlavor("text/plain")
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

val Step.associatedAsset: Asset?
    get() {
        if (implementor.category.endsWith("InvokeProcessActivity")) {
            activity.getAttribute(WorkAttributeConstant.PROCESS_NAME)?.let {
                val assetPath = if (it.endsWith(".proc")) it else "$it.proc"
                return (project as ProjectSetup).getAsset(assetPath)
            }
        }
        return null
    }