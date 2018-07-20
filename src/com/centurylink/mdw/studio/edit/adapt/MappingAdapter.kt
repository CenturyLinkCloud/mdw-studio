package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.apply.WidgetApplier

class MappingAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {
    override fun didInit(widget: Widget) {
//                if (widget.value)
//                    widget.value = Compatibility.getMap(widget.value);
//                if (widget.source === 'Subprocess')
//                    this.initSubprocBindings(widget, this.workflowObj.attributes.processname);
//                else
//                    this.initBindings(widget, this.process.variables);
    }


//    Configurator.prototype.initSubprocBindings = function(widget, subproc) {
//        var spaceV = subproc.lastIndexOf(' v');
//        if (spaceV > 0)
//            subproc = subproc(0, spaceV);
//
//        var configurator = this;
//        $http.get(mdw.roots.services + '/services/Workflow/' + subproc + "?app=mdw-admin").then(function(res) {
//            if (res.data.variables) {
//                configurator.initBindings(widget, res.data.variables, true);
//            }
//        });
//    };
//
//    // init bindings
//    Configurator.prototype.initBindings = function(widget, vars, includeOuts) {
//        widget.bindingVars = [];
//        util.getProperties(vars).forEach(function(varName) {
//            var variable = vars[varName];
//            if (variable.category === 'INPUT' || variable.category === 'INOUT' || (includeOuts && variable.category === 'OUTPUT')) {
//                variable.name = varName;
//                widget.bindingVars.push(variable);
//            }
//        });
//        widget.bindingVars.sort(function(v1, v2) {
//            if (widget.value) {
//                if (widget.value[v1.name] && !widget.value[v2.name])
//                    return -1;
//                else if (widget.value[v2.name] && !widget.value[v1.name])
//                    return 1;
//            }
//            return v1.name.localeCompare(v2.name);
//        });
//    };

}