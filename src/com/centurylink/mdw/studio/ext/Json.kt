package com.centurylink.mdw.studio.ext

import com.google.gson.*
import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.toGson(): JsonObject {
    return JsonParser().parse(toString()).asJsonObject
}

fun JSONArray.toStrings(): List<String> {
    val list = mutableListOf<String>()
    for (i in 0 until length()) {
        list.add(getString(i))
    }
    return list
}

fun JsonObject(content: String): JsonObject {
    return JsonParser().parse(content).asJsonObject
}

fun JsonArray.contains(string: String): Boolean {
    for (it in this) {
        if (it.isJsonPrimitive) {
            if (it.asJsonPrimitive.isString && it.asString == string) {
                return true
            }
        }
    }
    return false
}

fun JsonObject.toJSONObject(): JSONObject {
    return JSONObject(toString())
}

fun JsonElement.toString(pretty: Boolean): String {
    val builder = GsonBuilder().disableHtmlEscaping()
    if (pretty) {
        builder.setPrettyPrinting()
    }
    return builder.create().toJson(this)
}

class Json {
    companion object {
        /**
         * String representation of a JSONObject's property value.
         */
        fun toString(json: JSONObject, property: String, pretty: Boolean = true): String {
            val value = json.opt(property)
            return when (value) {
                null -> "null"
                is JSONObject -> if (pretty) value.toString(2) else value.toString()
                is JSONArray -> if (pretty) value.toString(2) else value.toString()
                else -> value.toString()
            }
        }

        fun toJSONArray(strings: List<String>): JSONArray {
            val jsonArray = JSONArray()
            for (string in strings) {
                jsonArray.put(string)
            }
            return jsonArray
        }
    }
}
