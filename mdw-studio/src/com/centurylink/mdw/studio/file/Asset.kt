package com.centurylink.mdw.studio.file

import com.intellij.openapi.vfs.VirtualFile
import java.security.MessageDigest

class Asset(val pkg: AssetPackage, val file: VirtualFile) {

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

        /**
         * TODO: honor .mdwignore
         */
        fun isIgnore(file: VirtualFile): Boolean {
            return IGNORED_FILES.contains(file.name)
        }
    }
}