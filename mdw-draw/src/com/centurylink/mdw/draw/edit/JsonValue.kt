package com.centurylink.mdw.draw.edit

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.ReadContext
import org.json.JSONObject

class JsonValue(val json: JSONObject, val name: String, val descrip: String? = null) : Comparable<JsonValue> {

    val label: String by lazy {
        if (isPath(name)) {
            evalPath(name)
        } else {
            name
        }
    }

    val description: String? by lazy {
        descrip?.let {
            if (isPath(it)) {
                evalPath(it)
            } else {
                it
            }
        }
    }

    private val readContext: ReadContext by lazy {
        val config = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS)
        JsonPath.using(config).parse(json.toString())
    }

    override fun toString(): String {
        return label
    }

    fun copy(): JsonValue {
        return JsonValue(json, name, descrip)
    }

    fun evalPath(path: String): String {
        return if (isPath(path)) {
            if (path == "\$") {
                json.toString()
            }
            else {
                val value = StringBuilder()
                path.split("/", "\n").forEach { segment ->
                    if (value.isNotEmpty()) {
                        if (path.contains("\n")) {
                            value.append("\n")
                        } else {
                            value.append(" / ")
                        }
                    }
                    value.append((readContext.read(segment) as Any?).toString())
                }
                value.toString()
            }
        } else {
            path
        }
    }

    override fun compareTo(other: JsonValue): Int {
        return label.compareTo(other.label)
    }

    companion object {
        fun isPath(name: String) = name.contains("\$.") || name == "\$"
    }
}