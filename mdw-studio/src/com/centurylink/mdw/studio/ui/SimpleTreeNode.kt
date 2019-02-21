package com.centurylink.mdw.studio.ui

import java.util.*
import javax.swing.tree.TreeNode

class SimpleTreeNode(private val parentNode: TreeNode, public val name: String) : TreeNode {

    override fun getParent() = parentNode
    override fun isLeaf() = true
    override fun getAllowsChildren() = false
    override fun getChildCount() = 0
    override fun children() = listOf<String>().toEnumeration()
    override fun getChildAt(childIndex: Int) = null
    override fun getIndex(node: TreeNode) = -1
}

fun <T> List<T>.toEnumeration(): Enumeration<T> {
    return object : Enumeration<T> {
        var count = 0

        override fun hasMoreElements(): Boolean {
            return this.count < size
        }

        override fun nextElement(): T {
            if (this.count < size) {
                return get(this.count++)
            }
            throw NoSuchElementException()
        }
    }
}