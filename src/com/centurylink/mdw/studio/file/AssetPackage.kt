package com.centurylink.mdw.studio.file

import com.centurylink.mdw.model.workflow.Package
import com.centurylink.mdw.util.file.VersionProperties
import com.centurylink.mdw.yaml.YamlLoader
import com.intellij.openapi.vfs.VirtualFile
import org.yaml.snakeyaml.error.YAMLException
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.util.*

class AssetPackage(val name: String, val dir: VirtualFile) {

    var version: Int

    val verString: String
        get() = Package.formatVersion(version)

    var schemaVersion = SCHEMA_VERSION

    val metaDir = dir.findChild(META_DIR) ?: throw FileNotFoundException("Missing metaDir: $dir/$META_DIR")
    val metaFile = dir.findFileByRelativePath(META_FILE) ?: throw FileNotFoundException("Missing metaFile: $dir/$META_FILE")

    var verFileTimeStamp = 0L
    var _versionProps: Properties = Properties()
    val versionProps: Properties
        get() {
            val propFile = metaDir.findChild(AssetPackage.VERSIONS)
            propFile?.let {
                if (verFileTimeStamp != propFile.timeStamp) {
                    _versionProps = VersionProperties(ByteArrayInputStream(propFile.contentsToByteArray()))
                    verFileTimeStamp = propFile.timeStamp
                }
            }
            return _versionProps
        }

    init {
        val loader = YamlLoader(String(metaFile.contentsToByteArray()))
        val topMap = loader.getRequiredMap("", loader.getTop(), "")
        val parsedName = loader.getRequired("name", topMap, "")
        if (name != parsedName) {
            throw YAMLException("$PACKAGE_YAML: $parsedName is not $name")
        }
        var ver = loader.getRequired("version", topMap, "")
        version = Package.parseVersion(ver)
        schemaVersion = loader.getRequired("schemaVersion", topMap, "")
    }

    companion object {
        const val META_DIR = ".mdw"
        const val PACKAGE_YAML = "package.yaml"
        const val META_FILE = "$META_DIR/$PACKAGE_YAML"
        const val VERSIONS = "versions"
        const val VERSIONS_FILE = "$META_DIR/$VERSIONS"
        const val SCHEMA_VERSION = "6.1"

        fun createPackageYaml(name: String, version: Int): String {
            return "schemaVersion: '$SCHEMA_VERSION'\nname: $name\nversion: ${Package.formatVersion(version)}"
        }

        fun isMeta(file: VirtualFile): Boolean {
            return if (file.isDirectory) {
                file.name == META_DIR
            }
            else {
                file.parent.name == META_DIR
            }
        }
    }
}