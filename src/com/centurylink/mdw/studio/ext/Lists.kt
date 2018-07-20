package com.centurylink.mdw.studio.ext

fun <T> List<T>.findIndex(checker: (item: T) -> Boolean): Int {
    for (i in indices) {
        if (checker(this[i]))
            return i
    }
    return -1
}

fun <T> MutableList<T>.replaceAt(index: Int, item: T) {
    removeAt(index)
    add(index, item)
}