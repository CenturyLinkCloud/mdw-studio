package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.apply.WidgetApplier

class TableAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {
    override fun didInit(widget: Widget) {
//                if (widget.value)
//                    widget.value = Compatibility.getTable(widget.value);
//                this.initTableValues(widget);
    }

//    Configurator.prototype.initTableValues = function(tblWidget, assetOptions) {
//        tblWidget.widgetRows = [];
//        var assetWidgets = []; // must be uniform source
//        if (!tblWidget.value || tblWidget.value.length === 0) {
//            tblWidget.value = [];
//            tblWidget.value.push([]);
//            tblWidget.widgets.forEach(function(widget) {
//                if (widget.source !== 'ProcessVersion' && widget.source !== 'AssetVersion')
//                    tblWidget.value[0].push('');
//            });
//        }
//        for (let i = 0; i < tblWidget.value.length; i++) {
//            var widgetRow = [];
//            for (let j = 0; j < tblWidget.widgets.length; j++) {
//            var widget = tblWidget.widgets[j];
//            if (widget.source !== 'ProcessVersion' && widget.source !== 'AssetVersion') {
//                var rowWidget = {
//                    type: widget.type,
//                    value: tblWidget.value[i][j],
//                    parent: tblWidget,
//                    configurator: this
//                };
//                if (!rowWidget.value && widget.default)
//                    rowWidget.value = widget.default;
//                if (widget.readonly)
//                    rowWidget.readonly = widget.readonly;
//                else if (tblWidget.readonly)
//                    rowWidget.readonly = tblWidget.readonly;
//                if (widget.type === 'asset') {
//                    rowWidget.source = widget.source;
//                    assetWidgets.push(rowWidget);
//                }
//                if (widget.options)
//                    rowWidget.options = widget.options;
//                if (widget.vw)
//                    rowWidget.width = widget.vw;
//                widgetRow.push(rowWidget);
//            }
//        }
//            tblWidget.widgetRows.push(widgetRow);
//        }
//        if (assetWidgets.length > 0)
//            this.initAssetOptions(assetWidgets, assetOptions);
//    };

}