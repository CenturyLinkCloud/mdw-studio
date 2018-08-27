package com.centurylink.mdw.studio.proc

import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.studio.draw.Diagram
import com.centurylink.mdw.studio.draw.DiagramEvent
import com.centurylink.mdw.studio.draw.Display
import com.centurylink.mdw.studio.draw.DragEvent
import com.centurylink.mdw.studio.edit.SelectListener
import com.centurylink.mdw.studio.edit.UpdateListeners
import com.centurylink.mdw.studio.edit.UpdateListenersDelegate
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.ui.UISettings
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.event.*
import javax.swing.*
import javax.swing.TransferHandler.*

class ProcessCanvas(val setup: ProjectSetup, var process: Process, val readonly: Boolean = false) :
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
                    revalidate()
                    repaint()
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
                        val deltaX = e.x - dragX
                        val deltaY = e.y - dragY
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

        // copy
        actionMap.put(getCopyAction().getValue(Action.NAME), getCopyAction())
        inputMap.put(KeyStroke.getKeyStroke("ctrl C"), getCopyAction().getValue(Action.NAME))

        if (!readonly) {
            // cut
            actionMap.put(getCutAction().getValue(Action.NAME), object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    diagram?.let {
                        if (it.hasSelection()) {
                            getCopyAction().actionPerformed(e)
                            it.onDelete()
                            notifyUpdateListeners(it.workflowObj)
                            invalidate()
                            repaint()
                        }
                    }
                }
            })
            inputMap.put(KeyStroke.getKeyStroke("ctrl X"), getCutAction().getValue(Action.NAME))

            // paste
            actionMap.put(getPasteAction().getValue(Action.NAME), getPasteAction())
            inputMap.put(KeyStroke.getKeyStroke("ctrl V"), getPasteAction().getValue(Action.NAME))

            // delete
            actionMap.put("mdw.delete", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    diagram?.let {
                        if (it.hasSelection()) {
                            it.onDelete()
                            notifyUpdateListeners(it.workflowObj)
                            invalidate()
                            repaint()
                        }
                    }
                }
            })
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "mdw.delete")
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
        val d = Diagram(g2d, initDisplay, setup, process, setup.implementors, readonly)
        diagram = d
        if (prevSelect == null) {
            // first time
            prevSelect = d.selection
            for (listener in selectListeners) {
                listener.onSelect(d.selection.selectObjs)
            }
        }
        else {
            d.selection = prevSelect
        }

//        val prevTransferData = transferHandler?.let {
//            (it as TransferHandler).transferData
//        }
        transferHandler = TransferHandler(d)
//         (transferHandler as TransferHandler).transferData = prevTransferData
        (transferHandler as TransferHandler).addUpdateListener { workflowObj ->
            notifyUpdateListeners(workflowObj)
            prevSelect = d.selection
            invalidate()
            repaint()
            d.selection.selectObjs.let {
                for (listener in selectListeners) {
                    listener.onSelect(it)
                }
            }
        }

        d.draw()
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
}