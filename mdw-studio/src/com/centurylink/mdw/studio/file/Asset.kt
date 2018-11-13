package com.centurylink.mdw.studio.file

import com.centurylink.mdw.draw.model.Data
import com.centurylink.mdw.util.file.MdwIgnore
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.security.MessageDigest

class Asset(val pkg: AssetPackage, val file: VirtualFile) : Comparable<Asset> {

    val id: Long
        get() = hexId.toLong(16)
    val hexId: String
        get() = hash(logicalPath)

    val name: String
        get() = file.name
    val encodedName: String
        get() = file.name.replace(" ", "%20")  // URLEncoder uses '+' for spaces
    val ext: String
        get() = name.substring(name.lastIndexOf('.') + 1)
    val rootName: String
        get() = name.substring(0, name.lastIndexOf('.'))

    val version: Int
        get() {
            pkg.versionProps.let { verProps ->
                verProps.getProperty(name)?.let { verProp ->
                    return verProp.split(" ")[0].toInt()
                }
            }
            return 0
        }

    val verString: String
        get() = if (version == 0) "0" else (version/1000).toString() + "." + version%1000

    val path: String
        get() = "${pkg.name}/$name"
    private val logicalPath: String
        get() = "$path v$verString"

    val contents: ByteArray
        get() = file.contentsToByteArray()

    override fun toString(): String {
        return path
    }

    override fun equals(other: Any?): Boolean {
        return other is Asset && other.path == path
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }

    override fun compareTo(other: Asset): Int {
        return path.compareTo(other.path)
    }

    companion object {
        private val IGNORED_FILES = arrayOf(".DS_Store")

        fun hash(logicalPath: String): String {
            val blob = "blob " + logicalPath.length + "\u0000" + logicalPath
            val md = MessageDigest.getInstance("SHA-1")
            val bytes = md.digest(blob.toByteArray())
            return byteArrayToHexString(bytes).substring(0, 7)
        }

        private fun byteArrayToHexString(b: ByteArray): String {
            var result = ""
            for (i in b.indices) {
                result += Integer.toString((b[i].toInt() and 0xff.toInt()) + 0x100, 16).substring(1)
            }
            return result
        }

        fun isIgnore(file: VirtualFile): Boolean {
            if (file.isDirectory) {
                return true
            }
            if (AssetPackage.isMeta(file)) {
                return true
            }
            if (IGNORED_FILES.contains(file.name)) {
                return true
            }
            if (file.parent != null && file.parent.isDirectory) {
                val mdwIgnore = MdwIgnore(File("${file.parent.path}"))
                if (mdwIgnore.isIgnore(File(file.path))) {
                    return true
                }
            }
            // walk the parent paths looking for .mdwignores
            var child = file
            var parent: VirtualFile? = file.parent
            while (parent != null && parent.isDirectory) {
                val mdwIgnore = MdwIgnore(File(parent.path))
                if (mdwIgnore.isIgnore(File(child.path))) {
                    return true
                }
                child = parent
                parent = parent.parent
            }
            return false
        }
    }
}