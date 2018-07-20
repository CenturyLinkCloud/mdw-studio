package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.apply.WidgetApplier

class DatetimeAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {
    override fun didInit(widget: Widget) {
//                if (widget.value) {
//                    widget.units = this.workflowObj.attributes[widget.name + '_UNITS'];
//                    if (!widget.units)
//                        widget.units = 'Hours';
//                    if (widget.units == 'Minutes')
//                        widget.value = parseInt(widget.value) / 60;
//                    else if (widget.units == 'Hours')
//                        widget.value = parseInt(widget.value) / 3600;
//                    else if (widget.units == 'Days')
//                        widget.value = parseInt(widget.value) / 86400;
//                }
    }
}