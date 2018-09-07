package com.centurylink.mdw.draw

import com.centurylink.mdw.model.workflow.ActivityImplementor
import org.json.JSONObject
import javax.swing.ImageIcon

class Impl(val assetPath: String?, json: JSONObject) : ActivityImplementor(json) {
    var icon: ImageIcon? = null
    val category: String
        get() = baseClassName

    // generic implementor for unfound impls
    constructor(implClass: String) : this(null, JSONObject("{\"implementorClass\":\"$implClass\"}")) {
        implementorClassName = implClass
        iconName = "shape:activity"
        baseClassName = "com.centurylink.mdw.activity.types.GeneralActivity"
    }

    constructor(category: String, label: String, icon: String, implClass: String) : this(implClass) {
        baseClassName = category
        implementorClassName = implClass
        iconName = icon
    }

    companion object {
        val DUMMY = Impl("com.centurylink.mdw.workflow.activity.DefaultActivityImpl")
        const val BASE_PKG = "com.centurylink.mdw.base"
        val PSEUDO_IMPLS = listOf(
                Impl("subflow", "Exception Handler Subflow", "$BASE_PKG/subflow.png", "Exception Handler"),
                Impl("subflow", "Cancellation Handler Subflow", "$BASE_PKG/subflow.png", "Cancellation Handler"),
                Impl("subflow", "Delay Handler Subflow", "$BASE_PKG/subflow.png", "Delay Handler"),
                Impl("note", "Text Note", "$BASE_PKG/note.png", "TextNote")
        )
        const val START_IMPL = "com.centurylink.mdw.workflow.activity.process.ProcessStartActivity"
        const val STOP_IMPL = "com.centurylink.mdw.workflow.activity.process.ProcessFinishActivity"
    }
}