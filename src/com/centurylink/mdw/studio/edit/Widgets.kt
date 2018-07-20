package com.centurylink.mdw.studio.edit

import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.edit.adapt.WidgetAdapter
import com.centurylink.mdw.studio.edit.apply.AbstractWidgetApplier
import com.centurylink.mdw.studio.edit.apply.AttributeApplier
import com.centurylink.mdw.studio.edit.apply.ObjectApplier
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

var Pagelet.Widget.label: String
    get() = attributes["label"] ?: name
    set(value) { attributes["label"] = value.toString() }
val Pagelet.Widget.section: String?
    get() = attributes["section"]
val Pagelet.Widget.isHidden: Boolean
    get() = "true" == attributes["hidden"]
var Pagelet.Widget.isReadonly: Boolean
    get() = "true" == attributes["readonly"]
    set(value) { attributes["readonly"] = value.toString() }
val Pagelet.Widget.source: String?
    get() = attributes["source"]
val Pagelet.Widget.default: String?
    get() = attributes["default"]
var Pagelet.Widget.url: String?
    get() = attributes["url"]
    set(value) { attributes["url"] = value }
val Pagelet.Widget.isMultiline: Boolean
    get() = "true" == attributes["multiline"]
val Pagelet.Widget.valueString: String?
    get() {
        value.let {
            return when (it) {
                null -> null
                is JsonPrimitive -> it.asString
                is JsonObject -> GsonBuilder().disableHtmlEscaping().create().toJson(it)
                else -> it.toString()
            }
        }
    }
val Pagelet.Widget.isTableType: Boolean
    get() = type == "table" || type == "mapping"
val Pagelet.Widget.width: Int
    get() = attributes["vw"]?.toInt() ?: 500
val Pagelet.Widget.rows: Int?
    get() = attributes["rows"]?.toInt()
val Pagelet.Widget.min: Int?
    get() = attributes["min"]?.toInt()
val Pagelet.Widget.max: Int?
    get() = attributes["max"]?.toInt()
val Pagelet.Widget.isHelpLink: Boolean
    get() = type == "link" && (url?.startsWith("help/") ?: false)

fun Pagelet.Widget.createApplier(category: String): AbstractWidgetApplier {
    var applierClass = attributes["applier"]
    applierClass?.let {
        if (!it.contains('.')) {
            // qualify with default package
            applierClass = Pagelet.Widget::createApplier::class.java.`package`.name + ".apply." + it
        }
        return Class.forName(applierClass).newInstance() as AbstractWidgetApplier
    }

    // fall back to template default
    when (category) {
        "object" -> return ObjectApplier()
        else -> return AttributeApplier()
    }
}

/**
 * Exclusively by convention based on widget type
 */
fun Pagelet.Widget.createAdapter(category: String): WidgetAdapter {
    val adapterClass = Pagelet.Widget::createApplier::class.java.`package`.name + ".adapt." +
            type.substring(0, 1).toUpperCase() + type.substring(1) + "Adapter"
    val applier = createApplier(category)
    return try {
        Class.forName(adapterClass).getConstructor(WidgetApplier::class.java).newInstance(applier   ) as WidgetAdapter
    }
    catch(ex: ClassNotFoundException) {
        WidgetAdapter(applier)
    }
}