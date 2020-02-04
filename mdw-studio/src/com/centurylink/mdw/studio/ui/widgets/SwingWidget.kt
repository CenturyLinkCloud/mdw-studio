package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.UpdateListeners
import com.centurylink.mdw.draw.edit.UpdateListenersDelegate
import com.centurylink.mdw.draw.edit.apply.WidgetApplier
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.proj.ProjectSetup
import java.awt.FlowLayout
import java.awt.LayoutManager
import javax.swing.JPanel

open class SwingWidget(val widget: Widget, layout: LayoutManager = DEFAULT_LAYOUT) :
        JPanel(layout), UpdateListeners by UpdateListenersDelegate() {

    protected val workflowObj = (widget.adapter as WidgetApplier).workflowObj
    protected val projectSetup = workflowObj.project as ProjectSetup

    // name of (previously-added) widget whose updates I'm interested in
    open val listenTo: String? = null

    fun applyUpdate() {
        if (widget.adapter is WidgetApplier) {
            widget.adapter.willUpdate(widget)
            val applier = widget.adapter as WidgetApplier
            applier.update()
            notifyUpdateListeners(applier.workflowObj)
            // reflect updates prior to any subsequent value changes
            widget.adapter.didInit(widget)
        }
    }

    open fun dispose() {
    }

    companion object {
        val DEFAULT_LAYOUT = FlowLayout(FlowLayout.LEFT, 5, if (ProjectSetup.isWindows) 2 else 0)

        fun create(widget: Pagelet.Widget): SwingWidget {
            val swingWidgetClass = SwingWidget::class.java.`package`.name + "." +
                    widget.type.substring(0, 1).toUpperCase() + widget.type.substring(1)
            val widgetConstructor = Class.forName(swingWidgetClass).getConstructor(Pagelet.Widget::class.java)
            return widgetConstructor.newInstance(widget) as SwingWidget
        }
    }
}