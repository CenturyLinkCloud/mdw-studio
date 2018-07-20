package com.centurylink.mdw.studio.proc

import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.studio.draw.Diagram
import com.centurylink.mdw.studio.draw.DiagramEvent
import com.centurylink.mdw.studio.draw.Display
import com.centurylink.mdw.studio.draw.DragEvent
import com.centurylink.mdw.studio.draw.Label
import com.centurylink.mdw.studio.edit.SelectListener
import com.centurylink.mdw.studio.edit.UpdateListeners
import com.centurylink.mdw.studio.edit.UpdateListenersDelegate
import com.centurylink.mdw.studio.ext.JsonObject
import com.centurylink.mdw.studio.proj.Implementor
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.ui.UISettings
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.*
import javax.swing.*

class ProcessCanvas(val setup: ProjectSetup, val process: Process, val readonly: Boolean = false) :
        JPanel(BorderLayout()), UpdateListeners by UpdateListenersDelegate() {

    private var zoom = 100

    private val initDisplay: Display by lazy {
        Display(0, 0, size.width - 1, size.height)
    }

    // TODO: initialize diagram so can't be null? (new process?)
    var diagram: Diagram? = null
    var selectListeners = mutableListOf<SelectListener>()

    var mouseDown = false
    var downX = -1
    var downY = -1
    var dragX = -1
    var dragY = -1
    var dragging = false

    init {
        Display.DEFAULT_COLOR = UIManager.getColor("EditorPane.foreground")
        Display.GRID_COLOR = Color.LIGHT_GRAY
        Display.OUTLINE_COLOR = UIManager.getColor("EditorPane.foreground")
        Display.SHADOW_COLOR = Color(0, 0, 0, 50)
        Display.META_COLOR = Color.GRAY
        Display.BACKGROUND_COLOR = UIManager.getColor("EditorPane.background")

        background = Display.BACKGROUND_COLOR
        isFocusable = true
        autoscrolls = true

        addComponentListener(object: ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                diagram?.let {
                    it.display.w = size.width - Diagram.BOUNDARY_DIM
                    it.display.h = size.height - Diagram.BOUNDARY_DIM
                }
            }
        })

        addMouseListener(object: MouseAdapter() {

            override fun mousePressed(e: MouseEvent) {
                grabFocus()
                mouseDown = true
                downX = e.x
                downY = e.y
                val shift = (e.modifiers and ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK
                val ctrl = (e.modifiers and ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK
                diagram?.onMouseDown(DiagramEvent(e.x, e.y, shift, ctrl))
                revalidate()
                repaint()

                diagram?.selection?.selectObjs?.let {
                    for (listener in selectListeners) {
                        listener.onSelect(it)
                    }
                }
            }
            override fun mouseReleased(e: MouseEvent) {
                mouseDown = false
                if (!readonly) {
                    val shift = (e.modifiers and ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK
                    val ctrl = (e.modifiers and ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK
                    diagram?.let {
                        it.onMouseUp(DiagramEvent(e.x, e.y, shift, ctrl, drag = dragging))
                        if (dragging) {
                            notifyUpdateListeners(it.workflowObj)
                        }
                    }
                    invalidate()
                    repaint()
                }
                dragging = false
            }
        })

        addMouseMotionListener(object: MouseMotionAdapter() {

            override fun mouseMoved(e: MouseEvent) {
                diagram?.let {
                    val cursor = it.onMouseMove(DiagramEvent(e.x, e.y))
                    UIUtil.setCursor(this@ProcessCanvas, cursor)
                }
            }

            override fun mouseDragged(e: MouseEvent) {
                if (!readonly) {
                    val shift = (e.modifiers and ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK
                    val ctrl = (e.modifiers and ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK
                    if (!dragging || dragX == -1) {
                        dragX = downX
                        dragY = downY
                        dragging = true
                    }
                    diagram?.let {
                        var deltaX = e.x - dragX
                        var deltaY = e.y - dragY
                        it.onMouseDrag(DiagramEvent(e.x, e.y, shift = shift, ctrl = ctrl, drag = true),
                                        DragEvent(downX, downY, deltaX, deltaY))

                        invalidate()
                        repaint()

                        dragX = e.x
                        dragY = e.y

                        val visMinX = maxOf(e.x - Diagram.BOUNDARY_DIM, 0)
                        val visMaxX = e.x + Diagram.BOUNDARY_DIM
                        val visMinY = maxOf(e.y - Diagram.BOUNDARY_DIM, 0)
                        val visMaxY = e.y + Diagram.BOUNDARY_DIM
                        scrollRectToVisible(Rectangle(visMinX, visMinY, visMaxX - visMinX, visMaxY - visMinY))
                    }
                }
            }
        })

        transferHandler = object : TransferHandler() {
            override fun canImport(support: TransferSupport): Boolean {
                if (!readonly && support.transferable.isDataFlavorSupported(jsonFlavor)) {
                    return getImplementor(support.transferable) != null
                }
                return false
            }
            override fun importData(support: TransferSupport): Boolean {
                if (support.transferable.isDataFlavorSupported(jsonFlavor)) {
                    val impl = getImplementor(support.transferable)
                    if (impl != null) {
                        val dropPoint = support.dropLocation.dropPoint
                        diagram?.let {
                            val x = maxOf(dropPoint.x - 60, 0)
                            val y = maxOf(dropPoint.y - 30, 0)
                            it.onDrop(DiagramEvent(x, y), impl)
                            notifyUpdateListeners(it.workflowObj)
                            invalidate()
                            repaint()
                        }
                    }
                }
                return false
            }
            fun getImplementor(transferable: Transferable): Implementor? {
                val json = JsonObject(transferable.getTransferData(jsonFlavor).toString())
                return setup.implementors.get(json.get("mdw.implementor")?.asString)
            }
        }

        if (!readonly) {
            getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "mdw.delete")
            actionMap.put("mdw.delete", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    diagram?.let {
                        val selObj = it.selection.selectObj
                        if (selObj != it && !(selObj is Label)) {
                            var msg = if (it.selection.isMulti()) "Delete selected items?" else "Delete ${selObj.workflowObj.type}?"
                            if (JOptionPane.showConfirmDialog(this@ProcessCanvas, msg, "Confirm Delete",
                                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == 0) {
                                it.onDelete()
                                notifyUpdateListeners(it.workflowObj)
                                invalidate()
                                repaint()
                            }
                        }
                    }
                }
            })
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D

        if (zoom != 100) {
            val scale = zoom / 100.0
            g2d.scale(scale, scale)
        }

        // draw the process diagram
        var prevSelect = diagram?.selection
        diagram = Diagram(g2d, initDisplay, setup, process, setup.implementors, readonly)
        if (prevSelect == null) {
            // first time
            diagram?.let {
                prevSelect = it.selection
                for (listener in selectListeners) {
                    listener.onSelect(it.selection.selectObjs)
                }
            }
        }
        else {
            prevSelect?.let {
                diagram?.selection = it
            }
        }
        diagram?.draw()
    }

    override fun getPreferredSize(): Dimension {
        diagram?.let {
            return Dimension(it.display.w + Diagram.BOUNDARY_DIM, it.display.h + Diagram.BOUNDARY_DIM)
        }
        return super.getPreferredSize()
    }

    override fun paint(g: Graphics) {
        UISettings.setupAntialiasing(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        // g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        super.paint(g)
    }

    companion object {
        val jsonFlavor = DataFlavor("application/json")
    }
}