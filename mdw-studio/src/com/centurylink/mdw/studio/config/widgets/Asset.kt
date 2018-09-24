package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.draw.edit.*
import com.centurylink.mdw.draw.edit.apply.WidgetApplier
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.OpenSourceUtil
import com.intellij.util.ui.UIUtil
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

    private val projectSetup = (widget.adapter as WidgetApplier).workflowObj.project as ProjectSetup
    private val assetLink = AssetLink(widget.url ?: widget.valueString, projectSetup)

    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(2, 0, 0, 0)

        if (!assetLink.text.isNullOrEmpty()) {
            add(assetLink)
        }

        if (!widget.isReadonly) {
            val assetSelectButton = AssetSelectButton("Select...", widget.valueString, projectSetup, widget.source) { assetPath ->
                widget.value = assetPath
                assetLink.update(assetPath)
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
                    widget.value = value.toString()
                    assetLink.update(widget.valueString)
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

class AssetLink(var assetPath: String?, projectSetup: ProjectSetup) : JLabel() {

    private var _linkColor: String? = null
    var linkColor: String? = null
        set(value) {
            _linkColor = value
            text = getAssetLabel()
        }

    val assetName: String
      get() {
          var name = assetPath ?: ""
          val lastSlash = name.lastIndexOf('/')
          if (lastSlash > 0) {
              name = name.substring(lastSlash + 1, name.length)
          }
          return name
      }

    init {
        var assetFile: VirtualFile?

        text = getAssetLabel()
        toolTipText = assetPath
        cursor = getAssetCursor()
        border = BorderFactory.createEmptyBorder(0, 0, 2, 0)
        addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                assetPath?.let { assetPath ->
                    if (assetPath.startsWith("http://") || assetPath.startsWith("https://")) {
                        BrowserUtil.browse(assetPath)
                    }
                    else if (assetPath.startsWith("class://")) {
                        val scope = GlobalSearchScope.allScope(projectSetup.project)
                        val psiFacade = JavaPsiFacade.getInstance(projectSetup.project)
                        val psiClass = psiFacade.findClass(assetPath.substring(8), scope)
                        psiClass?.let {
                            OpenSourceUtil.navigate(psiClass)
                        }
                    }
                    else {
                        assetFile = projectSetup.getAssetFile(assetPath)
                        if (assetFile == null) {
                            // compatibility processes might not have extension
                            val procFile = projectSetup.getAssetFile(assetPath + ".proc")
                            procFile?.let { assetFile = procFile }
                        }
                        assetFile ?: throw IOException("Asset not found: ${assetPath}")
                        assetFile?.let {
                            FileEditorManager.getInstance(projectSetup.project).openFile(it, true)
                        }
                    }
                }
            }
        })
    }

    private fun getAssetLabel(): String {
        return if (assetPath.isNullOrEmpty() || containsExpression(assetPath)) {
            assetName // contains expression
        } else {
            val color = if (_linkColor == null && UIUtil.isUnderDarcula()) {
                "white"
            }
            else {
                _linkColor
            }
            if (color == null) {
                "<html><a href='.'>${assetName}</a></html>"
            } else {
                "<html><a href='.' style='color:$color;'>${assetName}</a></html>"
            }
        }
    }

    private fun getAssetCursor(): Cursor {
        return if (containsExpression(assetPath)) {
            Cursor(Cursor.DEFAULT_CURSOR)
        }
        else {
            Cursor(Cursor.HAND_CURSOR)
        }
    }

    fun update(assetPath: String?) {
        this.assetPath = assetPath
        text = getAssetLabel()
        toolTipText = assetPath
        cursor = getAssetCursor()
    }
}

typealias AssetSelectCallback = (assetPath: String?) -> Unit

fun containsExpression(string: String?): Boolean {
    return string != null && string.indexOf("\${") >= 0
}

class AssetSelectButton(label: String, var assetPath: String?, projectSetup: ProjectSetup, source: String? = null,
        callback: AssetSelectCallback? = null) : JButton(label) {

    init {
        isOpaque = false

        var assetFile: VirtualFile? = null

        addActionListener {
            if (!assetPath.isNullOrEmpty()) {
                if (assetPath!!.contains("/")) {
                    assetFile = projectSetup.getAssetFile(assetPath!!)
                }
            }
            val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
            descriptor.withRoots(projectSetup.assetDir)
            source?.let {
                var src = it
                if (src.startsWith("[")) {
                    src = src.substring(1, src.length - 1)
                }
                descriptor.withFileFilter {
                    fileExtMatch(it, src.split(","))
                }
            }
            FileChooser.chooseFile(descriptor, projectSetup.project, assetFile) { file ->
                assetPath = projectSetup.getAssetPath(file)
                callback?.let { callback(assetPath) }
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
