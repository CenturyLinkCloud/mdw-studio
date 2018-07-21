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

    private val linkLabel: JLabel

    init {
        isOpaque = false

        var assetFile: VirtualFile? = null
        val applier = widget.adapter as WidgetApplier
        val workflowObj = applier.workflowObj
        val projectSetup = workflowObj.project as ProjectSetup

        val link = getAssetLink()
        linkLabel = JLabel(link)
        linkLabel.toolTipText = widget.valueString
        linkLabel.cursor = getAssetCursor()
        linkLabel.border = BorderFactory.createEmptyBorder(0, 0, 2, 0)
        linkLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent?) {
                assetFile = projectSetup.getAssetFile(widget.valueString!!)
                assetFile ?: throw IOException("Asset not found: ${widget.valueString}")
                assetFile?.let {
                    FileEditorManager.getInstance(projectSetup.project).openFile(it, true)
                }
            }
        })
        add(linkLabel)

        if (!widget.isReadonly) {
            // select button
            val selectButton = object : JButton("Select...") {
                override fun getPreferredSize(): Dimension {
                    val size = super.getPreferredSize()
                    return Dimension(size.width, size.height - 2)
                }
            }
            selectButton.isOpaque = false
            selectButton.addActionListener {
                val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
                descriptor.withRoots(projectSetup.assetDir)
                widget.source?.let { source ->
                    descriptor.withFileFilter {
                        fileExtMatch(it, source.split(","))
                    }
                }
                FileChooser.chooseFile(descriptor, projectSetup.project, assetFile) {
                    doUpdate(projectSetup.getAssetPath(it))
                }
            }
            add(selectButton)

            // set button
            val setButton = object : JButton("Set...") {
                override fun getPreferredSize(): Dimension {
                    val size = super.getPreferredSize()
                    return Dimension(size.width, size.height - 2)
                }
            }
            setButton.isOpaque = false
            setButton.addActionListener {
                val value = JOptionPane.showInputDialog(this@Asset, widget.label, "Enter Value",
                        JOptionPane.PLAIN_MESSAGE, null, null,  widget.valueString)
                if (value != null) {
                    // not canceled
                    doUpdate(value.toString())
                }
            }
            add(setButton)
        }
    }

    private fun doUpdate(value: String?) {
        widget.value = value
        linkLabel.text = getAssetLink()
        linkLabel.toolTipText = widget.valueString
        linkLabel.cursor = getAssetCursor()
        applyUpdate()
    }

    private fun getAssetLink(): String {
        var assetName = widget.valueString ?: ""
        val lastSlash = assetName.lastIndexOf('/')
        if (lastSlash > 0) {
            assetName = assetName.substring(lastSlash + 1, assetName.length)
        }
        return if (widget.containsExpression) {
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

    private fun fileExtMatch(file: VirtualFile, exts: List<String>): Boolean {
        for (ext in exts) {
            if (Comparing.equal(file.getExtension(), ext, SystemInfo.isFileSystemCaseSensitive)) {
                return true
            }
        }
        return false
    }
}