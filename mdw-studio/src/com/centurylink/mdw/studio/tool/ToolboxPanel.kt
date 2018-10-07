package com.centurylink.mdw.studio.tool

import com.centurylink.mdw.draw.Display
import com.centurylink.mdw.draw.Display.Companion.ICON_HEIGHT
import com.centurylink.mdw.draw.Display.Companion.ICON_PAD
import com.centurylink.mdw.draw.Display.Companion.ICON_WIDTH
import com.centurylink.mdw.draw.Shape
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.draw.model.WorkflowType
import com.centurylink.mdw.model.workflow.ActivityImplementor
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.studio.proj.ImplementorChangeListener
import com.centurylink.mdw.studio.proj.Implementors
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.ui.UISettings
import com.intellij.ide.ui.customization.CustomActionsSchema
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.EmptyBorder


class ToolboxPanel(private val projectSetup: ProjectSetup) : JPanel(), Disposable, ImplementorChangeListener {

    private val toolPanels = mutableListOf<ToolPanel>()
    var selected: ToolPanel? = null

    init {
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        border = BorderFactory.createEmptyBorder(2, 2, 2, 0)
        initialize()
        projectSetup.addImplementorChangeListener(this)
    }

    private fun initialize() {
        for (implementor in projectSetup.implementors.toSortedList()) {
            val toolPanel = ToolPanel(projectSetup, implementor)
            toolPanel.border = BORDER_NOT
            toolPanel.addMouseListener(object: MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    selected?.let {
                        it.border = BORDER_NOT
                    }
                    toolPanel.border = BORDER_SELECTED
                    selected = toolPanel
                    if (SwingUtilities.isRightMouseButton(e)) {
                        val action = CustomActionsSchema.getInstance().getCorrectedAction(ToolPanel.CONTEXT_MENU_GROUP_ID)
                        val popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.EDITOR_POPUP, action as ActionGroup)
                        popupMenu.component.show(toolPanel, e.x, e.y)
                    }
                }
            })
            val iconPanel = IconPanel(toolPanel.icon)
            iconPanel.preferredSize = Dimension(ICON_WIDTH + ICON_PAD, ICON_HEIGHT + ICON_PAD)
            toolPanel.add(iconPanel)
            val toolLabel = JLabel(implementor.label)
            toolLabel.border = EmptyBorder(0, 0, ICON_PAD, 0)
            toolPanel.add(toolLabel)
            toolPanels.add(toolPanel)
            add(toolPanel)
        }
    }

    override fun onChange(implementors: Implementors) {
        for (toolPanel in toolPanels) {
            remove(toolPanel)
        }
        initialize()
        revalidate()
        repaint()
    }

    override fun dispose() {
    }

    companion object {
        const val TOOL_WIDTH = 200
        const val TOOL_HEIGHT = 35
        val BORDER_SELECTED: Border = BorderFactory.createLineBorder(Color(0x0b93d5), 2 )
        val BORDER_NOT: Border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    }
}

class ToolPanel(val projectSetup: ProjectSetup, val implementor: ActivityImplementor) :
        JPanel(FlowLayout(FlowLayout.LEFT)), DataProvider {

    val icon = ToolboxIcon(implementor)

    init {
        preferredSize = Dimension(ToolboxPanel.TOOL_WIDTH, ToolboxPanel.TOOL_HEIGHT)

        transferHandler = object : TransferHandler() {

            val transferData = "{ \"mdw.implementor\": \"${implementor.implementorClass}\" }"

            override fun createTransferable(c: JComponent): Transferable {
                return object : Transferable {
                    override fun getTransferData(flavor: DataFlavor): Any {
                        return transferData
                    }
                    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
                        return flavor.isMimeTypeEqual(DataFlavor("application/json"))
                    }
                    override fun getTransferDataFlavors(): Array<DataFlavor> {
                        return arrayOf(DataFlavor("application/json"))
                    }
                }
            }

            override fun getSourceActions(c: JComponent): Int {
                return TransferHandler.COPY
            }
            override fun canImport(support: TransferSupport): Boolean {
                // just to show the icon/cursor
                if (support.transferable.isDataFlavorSupported(jsonFlavor)) {
                    return support.transferable.getTransferData(jsonFlavor) == transferData
                }
                return false
            }
            override fun importData(support: TransferSupport): Boolean {
                return false
            }
        }

        val imageIcon = implementor.imageIcon
        if (imageIcon != null) {
            transferHandler.dragImage = imageIcon.image
            transferHandler.dragImageOffset = Point(-imageIcon.image.getWidth(null), -imageIcon.image.getHeight(null))
        }
        else {
            val w = ICON_WIDTH
            val h = ICON_HEIGHT
            val img = UIUtil.createImage(w + 2, h, BufferedImage.TYPE_INT_ARGB)
            val g2d = img.graphics as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            icon.draw(g2d)
            transferHandler.dragImage = img
            transferHandler.dragImageOffset = Point(-w, -h)
        }

        addMouseMotionListener(object: MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                implementor.let {
                    transferHandler.exportAsDrag(this@ToolPanel, e, TransferHandler.COPY)
                }
            }
        })
    }

    companion object {
        val jsonFlavor = DataFlavor("application/json")
        const val CONTEXT_MENU_GROUP_ID = "mdwToolboxContextActions"
    }

    override fun getData(dataId: String): Any? {
        if (dataId == Implementors.ACTIVITY_IMPLEMENTOR) {
            return implementor
        }
        return null
    }

    inner class ToolboxIcon(private val implementor: ActivityImplementor) {
        fun draw(g2d: Graphics2D) {
            val shape = IconShape(g2d, implementor)
            shape.draw()
        }
    }

    inner class IconShape(private val g2d: Graphics2D, val implementor: ActivityImplementor) :
            Shape(g2d, Display(0, 0, ICON_WIDTH, ICON_HEIGHT)) {

        override val workflowObj = object : WorkflowObj(projectSetup, Process(), WorkflowType.implementor, implementor.json) {
            override var id = implementor.implementorClass
            override var name = implementor.label
        }

        override fun draw(): Display {
            val imageIcon = implementor.imageIcon
            if (imageIcon != null) {
                g2d.drawImage(imageIcon.image, display.x, display.y, null)
            }
            else if (implementor.icon != null && implementor.icon.startsWith("shape:")) {
                val shape = implementor.icon.substring(6)
                when(shape) {
                    "start" -> {
                        drawOval(display.x, display.y + 1, display.w + 2, display.h - 1,
                                fill = Display.START_COLOR)
                    }
                    "stop" -> {
                        drawOval(display.x, display.y + 1, display.w + 2, display.h - 1,
                                fill = Display.STOP_COLOR)
                    }
                    "decision" -> {
                        drawDiamond()
                    }
                    else -> {
                        drawRect(display.x + 1, display.y + 2, display.w - 3, display.h - 3)
                    }
                }
            }
            else {
                drawRect(display.x + 1, display.y + 2, display.w - 3, display.h - 3)
            }

            return display
        }
    }

}

class IconPanel(private val toolboxIcon: ToolPanel.ToolboxIcon) : JPanel() {

    override fun paintComponent(g: Graphics) {
        toolboxIcon.draw(g as Graphics2D)
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

