package com.centurylink.mdw.studio.file

import com.intellij.openapi.vfs.VirtualFile
import java.security.MessageDigest
import kotlin.experimental.and


class Asset(val packageName: String, val file: VirtualFile, val version: Int) {

    val id: Long
        get() = hexId.toLong(16)
    val hexId: String
        get() = hash(logicalPath)

    val name: String
        get() = file.name

    val versionString: String
        get() = if (version == 0) "0" else (version/1000).toString() + "." + version%1000
    val formattedVersion: String
        get() = "v$versionString"

    val logicalPath: String
        get() = packageName + "/" + name + " " + formattedVersion

    companion object {
        fun hash(logicalPath: String): String {
            val blob = "blob " + logicalPath.length + "\u0000" + logicalPath
            val md = MessageDigest.getInstance("SHA-1")
            val bytes = md.digest(blob.toByteArray())
            return byteArrayToHexString(bytes).substring(0, 7)
        }

        fun byteArrayToHexString(b: ByteArray): String {
            var result = ""
            for (i in b.indices) {
                result += Integer.toString((b[i].toInt() and 0xff.toInt()) + 0x100, 16).substring(1)
            }
            return result
        }
    }
}