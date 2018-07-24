package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.*
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.IOException
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JOptionPane

@Suppress("unused")
class Asset(widget: Pagelet.Widget) : SwingWidget(widget) {

    private val assetLink = AssetLink(widget)

    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(2, 0, 0, 0)

        if (!assetLink.text.isNullOrEmpty()) {
            add(assetLink)
        }

        if (!widget.isReadonly) {
            val assetSelectButton = AssetSelectButton(widget, "Select...") { assetPath ->
                assetLink.doUpdate(assetPath)
                remove(assetLink)
                if (!assetLink.text.isNullOrEmpty()) {
                    add(assetLink, 0)
                }
                applyUpdate()
                revalidate()
                repaint()
            }
            add(assetSelectButton)

            // set explicitly (expressions, etc)
            val setButton = object : JButton("Set...") {
                override fun getPreferredSize(): Dimension {
                    val size = super.getPreferredSize()
                    return Dimension(size.width, size.height - 4)
                }
            }
            setButton.isOpaque = false
            setButton.addActionListener {
                val value = JOptionPane.showInputDialog(this@Asset, widget.label, "Enter Value",
                        JOptionPane.PLAIN_MESSAGE, null, null,  widget.valueString)
                if (value != null) {
                    // not canceled
                    assetLink.doUpdate(value.toString())
                    remove(assetLink)
                    if (!assetLink.text.isNullOrEmpty()) {
                        add(assetLink, 0)
                    }
                    applyUpdate()
                    revalidate()
                    repaint()
                }
            }
            add(setButton)
        }
    }
}

class AssetLink(val widget: Pagelet.Widget) : JLabel() {

    init {
        var assetFile: VirtualFile? = null
        val applier = widget.adapter as WidgetApplier
        val workflowObj = applier.workflowObj
        val projectSetup = workflowObj.project as ProjectSetup

        text = getAssetLabel()
        toolTipText = widget.valueString
        cursor = getAssetCursor()
        border = BorderFactory.createEmptyBorder(0, 0, 2, 0)
        addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                assetFile = projectSetup.getAssetFile(widget.valueString!!)
                if (assetFile == null) {
                    // compatibility processes might not have extension
                    val procFile = projectSetup.getAssetFile(widget.valueString + ".proc")
                    procFile?.let { assetFile = procFile }
                }
                assetFile ?: throw IOException("Asset not found: ${widget.valueString}")
                assetFile?.let {
                    FileEditorManager.getInstance(projectSetup.project).openFile(it, true)
                }
            }
        })
    }

    private fun getAssetLabel(): String {
        var assetName = widget.valueString ?: ""
        val lastSlash = assetName.lastIndexOf('/')
        if (lastSlash > 0) {
            assetName = assetName.substring(lastSlash + 1, assetName.length)
        }
        return if (widget.valueString.isNullOrEmpty() || widget.containsExpression) {
            assetName // contains expression
        } else {
            "<html><a href='.'>${assetName}</a></html>"
        }
    }

    private fun getAssetCursor(): Cursor {
        return if (widget.containsExpression) {
            Cursor(Cursor.DEFAULT_CURSOR)
        }
        else {
            Cursor(Cursor.HAND_CURSOR)
        }
    }

    fun doUpdate(assetPath: String?) {
        widget.value = assetPath
        text = getAssetLabel()
        toolTipText = widget.valueString
        cursor = getAssetCursor()
    }
}

typealias AssetSelectCallback = (assetPath: String?) -> Unit

class AssetSelectButton(widget: Pagelet.Widget, label: String, callback: AssetSelectCallback? = null) : JButton(label) {

    init {
        var assetFile: VirtualFile? = null
        val applier = widget.adapter as WidgetApplier
        val workflowObj = applier.workflowObj
        val projectSetup = workflowObj.project as ProjectSetup

        isOpaque = false
        addActionListener {
            val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
            descriptor.withRoots(projectSetup.assetDir)
            widget.source?.let { source ->
                descriptor.withFileFilter {
                    fileExtMatch(it, source.split(","))
                }
            }
            FileChooser.chooseFile(descriptor, projectSetup.project, assetFile) { file ->
                callback?.let { callback(projectSetup.getAssetPath(file)) }
            }
        }
    }

    private fun fileExtMatch(file: VirtualFile, exts: List<String>): Boolean {
        for (ext in exts) {
            if (Comparing.equal(file.getExtension(), ext, SystemInfo.isFileSystemCaseSensitive)) {
                return true
            }
        }
        return false
    }

    override fun getPreferredSize(): Dimension {
        val size = super.getPreferredSize()
        return Dimension(size.width, size.height - 4)
    }
}
