package com.centurylink.mdw.draw.model

import com.centurylink.mdw.model.workflow.ActivityImplementor
import org.json.JSONObject
import javax.swing.ImageIcon

class Implementor(val assetPath: String?, json: JSONObject) : ActivityImplementor(json) {
    var icon: ImageIcon? = null
    val category: String
        get() = baseClassName

    // generic implementor for unfound impls
    constructor(implClass: String) : this(null, JSONObject("{\"implementorClass\":\"$implClass\"}")) {
        implementorClassName = implClass
        iconName = "shape:activity"
        baseClassName = "com.centurylink.mdw.activity.types.GeneralActivity"
    }

    constructor(category: String, label: String, icon: String, implClass: String, pagelet: String? = null) : this(implClass) {
        baseClassName = category
        this.label = label
        implementorClassName = implClass
        iconName = icon
        attributeDescription = pagelet
    }

    companion object {
        val DUMMY = Implementor("com.centurylink.mdw.workflow.activity.DefaultActivityImpl")
        val PSEUDO_IMPLS = listOf(
                Implementor("subflow", "Exception Handler Subflow", "${Data.BASE_PKG}/subflow.png", "Exception Handler"),
                Implementor("subflow", "Cancellation Handler Subflow", "${Data.BASE_PKG}/subflow.png", "Cancellation Handler"),
                Implementor("subflow", "Delay Handler Subflow", "${Data.BASE_PKG}/subflow.png", "Delay Handler"),
                Implementor("note", "Text Note", "${Data.BASE_PKG}/note.png", "TextNote")
        )
        const val START_IMPL = "com.centurylink.mdw.workflow.activity.process.ProcessStartActivity"
        const val STOP_IMPL = "com.centurylink.mdw.workflow.activity.process.ProcessFinishActivity"
    }
}