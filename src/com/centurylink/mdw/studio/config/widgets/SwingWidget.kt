package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.UpdateListeners
import com.centurylink.mdw.studio.edit.UpdateListenersDelegate
import com.centurylink.mdw.studio.edit.apply.AbstractWidgetApplier
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.proj.ProjectSetup
import java.awt.FlowLayout
import java.awt.LayoutManager
import javax.swing.JPanel
import kotlin.reflect.full.isSubclassOf

open class SwingWidget(val widget: Widget, layout: LayoutManager = defaultLayout) :
        JPanel(layout), UpdateListeners by UpdateListenersDelegate() {

    fun applyUpdate() {
        if (widget.adapter is WidgetApplier) {
            val applier = widget.adapter as WidgetApplier
            applier.update()
            notifyUpdateListeners(applier.workflowObj)
        }
    }

    companion object {
        val defaultLayout = FlowLayout(FlowLayout.LEFT, 5, if (ProjectSetup.isWindows) 2 else 0)
    }
}