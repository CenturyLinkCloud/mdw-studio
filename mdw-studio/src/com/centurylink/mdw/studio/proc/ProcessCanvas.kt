package com.centurylink.mdw.studio.proc

import com.centurylink.mdw.draw.*
import com.centurylink.mdw.draw.Shape
import com.centurylink.mdw.draw.edit.*
import com.centurylink.mdw.draw.model.DrawProps
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.studio.MdwSettings
import com.centurylink.mdw.studio.action.ActivityAssetAction
import com.centurylink.mdw.studio.action.ActivityEditAction
import com.centurylink.mdw.studio.action.ImplementorSource
import com.centurylink.mdw.studio.file.Icons
import com.centurylink.mdw.studio.proj.Implementors
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.ui.UISettings
import com.intellij.ide.ui.customization.CustomActionsSchema
import com.intellij.openapi.actionSystem.*
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.*
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.UIManager

class ProcessCanvas(private val setup: ProjectSetup, internal var process: Process,
        val drawProps: DrawProps = DrawProps(milestoneGroups = setup.milestoneGroups)) :
                JPanel(BorderLayout()), DataProvider, UpdateListeners by UpdateListenersDelegate() {

    private var _zoom = 100
    var zoom
        get() = _zoom
        set(value) {
            _zoom = value
            diagram?.let {
                revalidate()
                repaint()
            }
        }
    private val scale
        get() = if (_zoom == 100) 1f else _zoom / 100f

    private fun scale(i: Int): Int {
        return if (scale == 1f) {
            i
        } else {
            (i / scale).toInt()
        }
    }

    var isShowGrid
        get() = diagram?.isShowGrid ?: true
        set(value) {
            diagram?.let {
                it.isShowGrid = value
                revalidate()
                repaint()
            }
        }

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

    private var actionProvider: CanvasActions? = null

    init {
        Display.DEFAULT_COLOR = UIManager.getColor("EditorPane.foreground")
        Display.GRID_COLOR = if (UIUtil.isUnderDarcula()) {
            Color(120, 120, 120)
        }
        else {
            Color.LIGHT_GRAY
        }
        Display.OUTLINE_COLOR = UIManager.getColor("EditorPane.foreground")
        Display.SHADOW_COLOR = Color(0, 0, 0, 50)
        Display.META_COLOR = Color.GRAY
        Display.BACKGROUND_COLOR = if (UIUtil.isUnderDarcula()) {
            Color(43, 43, 43)
        }
        else {
            UIManager.getColor("EditorPane.background")
        }
        if (UIUtil.isUnderDarcula()) {
            Subflow.BOX_OUTLINE_COLOR = Color(75, 165, 199)
        }

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
                val x = scale(e.x)
                val y = scale(e.y)
                downX = x
                downY = y
                val shift = (e.modifiers and ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK
                val ctrl = (e.modifiers and ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK
                diagram?.onMouseDown(DiagramEvent(x, y, shift, ctrl, false))
                revalidate()
                repaint()

                diagram?.selection?.selectObjs?.let {
                    for (listener in selectListeners) {
                        listener.onSelect(it, !e.isPopupTrigger && SwingUtilities.isLeftMouseButton(e) && e.clickCount == 2)
                    }
                }

                if (SwingUtilities.isRightMouseButton(e)) {
                    val action = CustomActionsSchema.getInstance().getCorrectedAction(CanvasActions.CONTEXT_MENU_GROUP_ID)
                    var actionGroup = action as ActionGroup
                    if (action is DefaultActionGroup) {
                        diagram?.let { diagram ->
                            if (diagram.selection.selectObjs.size == 1 && diagram.selection.selectObj is Step) {
                                val step = diagram.selection.selectObj as Step
                                val actions = action.childActionsOrStubs.toMutableList()
                                actions.add(0, ImplementorSource())
                                step.associatedAsset?.let { asset ->
                                    actions.add(0, ActivityAssetAction(asset))
                                }
                                step.associatedEdit?.let { edit ->
                                    val editAction = ActivityEditAction(step.workflowObj, edit)
                                    editAction.addUpdateListener { obj ->
                                        obj.updateAsset()
                                        notifyUpdateListeners(obj)
                                    }
                                    actions.add(0, editAction)
                                }
                                actionGroup = DefaultActionGroup(actions)
                            }
                        }
                    }
                    val popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.EDITOR_POPUP, actionGroup)
                    popupMenu.component.show(this@ProcessCanvas, x, y)
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                mouseDown = false
                val x = scale(e.x)
                val y = scale(e.y)
                if (!drawProps.isReadonly) {
                    val shift = (e.modifiers and ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK
                    val ctrl = (e.modifiers and ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK
                    diagram?.let {
                        it.onMouseUp(DiagramEvent(x, y, shift, ctrl, drag = dragging))
                        if (dragging) {
                            notifyUpdateListeners(it.workflowObj)
                            it.selection.selectObjs.let { selObj ->
                                for (listener in selectListeners) {
                                    listener.onSelect(selObj)
                                }
                            }
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
                val x = scale(e.x)
                val y = scale(e.y)
                diagram?.let {
                    val cursor = it.onMouseMove(DiagramEvent(x, y))
                    UIUtil.setCursor(this@ProcessCanvas, cursor)
                }
            }

            override fun mouseDragged(e: MouseEvent) {
                if (!drawProps.isReadonly) {
                    val x = scale(e.x)
                    val y = scale(e.y)
                    val shift = (e.modifiers and ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK
                    val ctrl = (e.modifiers and ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK
                    if (!dragging || dragX == -1) {
                        dragX = downX
                        dragY = downY
                        dragging = true
                    }
                    diagram?.let {
                        val deltaX = x - dragX
                        val deltaY = y - dragY
                        it.onMouseDrag(DiagramEvent(x, y, shift = shift, ctrl = ctrl, drag = true),
                                        DragEvent(downX, downY, deltaX, deltaY))

                        invalidate()
                        repaint()

                        dragX = x
                        dragY = y

                        val visMinX = maxOf(x - Diagram.BOUNDARY_DIM, 0)
                        val visMaxX = x + Diagram.BOUNDARY_DIM
                        val visMinY = maxOf(y - Diagram.BOUNDARY_DIM, 0)
                        val visMaxY = y + Diagram.BOUNDARY_DIM
                        scrollRectToVisible(Rectangle(visMinX, visMinY, visMaxX - visMinX, visMaxY - visMinY))
                    }
                }
            }
        })
    }

    var preSelectedId: String? = null
    fun preSelect(id: String) {
        preSelectedId = id
        revalidate()
        repaint()
    }

    private fun select(drawable: Drawable) {
        diagram?.let { d ->
            d.selection = Selection(drawable)
            if (drawable is Shape) {
                scrollRectToVisible(drawable.display.toRect())
            }
            for (listener in selectListeners) {
                listener.onSelect(d.selection.selectObjs)
            }
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D

        _zoom = MdwSettings.instance.canvasZoom
        if (_zoom != 100) {
            val scale = _zoom / 100.0
            g2d.scale(scale, scale)
        }

        // draw the process diagram
        var prevSelect = diagram?.selection
        val d = Diagram(g2d, initDisplay, setup, process, setup.implementors, drawProps)
        d.isShowGrid = !MdwSettings.instance.isHideCanvasGridLines
        diagram = d
        preSelectedId?.let { selectId ->
            val drawable = d.findObj(selectId)
            if (drawable == null) {
                for (subflow in d.subflows) {
                    val subdrawable = subflow.findObj(selectId)
                    if (subdrawable != null) {
                        select(subdrawable)
                    }
                }
            }
            else {
                select(drawable)
            }
            prevSelect = d.selection
            preSelectedId = null
        }
        if (prevSelect == null) {
            // first time
            prevSelect = d.selection
            for (listener in selectListeners) {
                listener.onSelect(d.selection.selectObjs)
            }
        }
        else {
            d.selection = prevSelect as Selection
        }

        actionProvider = CanvasActions(d)
        (actionProvider as CanvasActions).addUpdateListener { workflowObj ->
            notifyUpdateListeners(workflowObj)
            prevSelect = d.selection
            invalidate()
            repaint()
            d.selection.selectObjs.let {
                for (listener in selectListeners) {
                    listener.onSelect(it)
                }
            }
            grabFocus()
        }

        transferHandler = TransferHandler(d)
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

    override fun getData(dataId: String): Any? {
        if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER.`is`(dataId) ||
                PlatformDataKeys.COPY_PROVIDER.`is`(dataId) ||
                PlatformDataKeys.CUT_PROVIDER.`is`(dataId) ||
                PlatformDataKeys.PASTE_PROVIDER.`is`(dataId)) {
            return actionProvider
        }
        else if (Implementors.IMPLEMENTOR_DATA_KEY.`is`(dataId)) {
            this.diagram?.selection?.selectObj?.let { selObj ->
                if (selObj is Step) {
                    return selObj.implementor
                }
            }
        }
        return null
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
        super.paint(g)
    }

    fun rename(newName: String) {
        process.name = newName
        diagram?.rename(newName)
        revalidate()
        repaint()
    }

    companion object {
        init {
            Display.START_ICON = Icons.readIcon("/icons/start.png")
            Display.STOP_ICON = Icons.readIcon("/icons/stop.png")
        }
    }
}