package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.cli.Operation
import com.centurylink.mdw.cli.ProgressMonitor
import com.centurylink.mdw.cli.Setup
import com.centurylink.mdw.dataaccess.file.PackageDir.META_DIR
import com.centurylink.mdw.studio.file.Asset
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import java.io.File
import java.io.IOException
import java.util.*
import javax.swing.ImageIcon


class Startup : StartupActivity {
    override fun runActivity(project: Project) {
        val projectSetup = project.getComponent(ProjectSetup::class.java)
    }
}

class ProjectSetup(val project: Project) : ProjectComponent {

    // setup is null if not an mdw project
    private var setup: Setup? = null
    val assetDir: VirtualFile

    val isMdwProject: Boolean
        get() = setup != null

    lateinit var implementors: Implementors

    init {
        val mdwYaml = File("${project.basePath}/${configLoc}/mdw.yaml")

        if (mdwYaml.exists()) {
            val mySetup = object : Setup(File(project.basePath)) {
                override fun run(vararg progressMonitors: ProgressMonitor?): Operation {
                    return this
                }
            }
            setup = mySetup
            assetDir = LocalFileSystem.getInstance().findFileByIoFile(mySetup.assetRoot)!!
        }
        else {
            assetDir = project.baseDir
        }
    }

    override fun projectOpened() {
        if (isMdwProject) {
            implementors = Implementors(this)
        }
    }

    override fun projectClosed() {
    }

    fun getPackageDirs(): List<VirtualFile> {
        var packageDirs = mutableListOf<VirtualFile>()
        VfsUtilCore.iterateChildrenRecursively(assetDir, VirtualFileFilter { it.isDirectory()}, ContentIterator {
            if (it.isDirectory() && it.findFileByRelativePath(".mdw/package.yaml") != null)
            packageDirs.add(it)
            true
        })
        return packageDirs
    }

    fun getPackageDir(name: String): VirtualFile? {
        return getPackageDirs().find {
            getPackageName(it) == name
        }
    }

    fun getAssetFile(assetPath: String): VirtualFile? {
        val slash = assetPath.lastIndexOf('/')
        if (slash == -1 || slash > assetPath.length - 2)
            throw IOException("Bad asset path: " + assetPath)
        val pkgPath = assetPath.substring(0, slash).replace('.', '/')
        return assetDir.findFileByRelativePath(pkgPath + "/" + assetPath.substring(slash + 1))
    }

    fun getAssetPath(file: VirtualFile): String? {
        if (file.exists()) {
            return getPackageName(file.parent) + "/" + file.name
        }
        else {
            return null
        }
    }

    fun getPackageName(packageDir: VirtualFile): String {
        return packageDir.toString().substring(assetDir.toString().length + 1)
                .replace('/', '.').replace('\\', '.')
    }

    fun getAsset(file: VirtualFile): Asset? {
        val pkg = getPackageName(file.parent)
        val pkgDir = getPackageDir(pkg)
        pkgDir?.let {
            return Asset(pkg, file, getAssetVersion(pkgDir, file.name))
        }
        return null
    }

    fun getVersionFile(packageDir: VirtualFile) {
        // TODO
    }

    fun getAssetVersion(packageDir: VirtualFile, assetName: String): Int {
        val metaDir = packageDir.findChild(META_DIR)
        metaDir?.let {
            val propFile = metaDir.findChild("versions")
            propFile?.let {
                val props = Properties()
                props.load(propFile.inputStream)
                val prop = props.getProperty(assetName)
                prop?.let {
                    return prop.split(" ")[0].toInt()
                }
            }
        }
        return 0
    }

    val icons = mutableMapOf<String,ImageIcon>()

    fun getIcon(assetPath: String): ImageIcon? {
        var icon = icons[assetPath]
        if (icon == null) {
            val asset = getAssetFile(assetPath)
            if (asset != null) {
                icon = ImageIcon(asset.contentsToByteArray())
            }
        }
        return icon
    }

    companion object {
        // TODO these values should not be static and should not be hardcoded
        val configLoc = "config"
        val hubRoot = "http://localhost:8080/mdw"

        const val SOURCE_REPO_URL = "https://github.com/CenturyLinkCloud/mdw"
        const val HELP_LINK_URL = "http://centurylinkcloud.github.io/mdw/docs"

        val isWindows: Boolean by lazy {
            System.getProperty("os.name").startsWith("Windows")
        }

        val documentTypes = mapOf(
            "org.w3c.dom.Document" to "xml",
            "org.apache.xmlbeans.XmlObject" to "xml",
            "java.lang.Object" to "java",
            "org.json.JSONObject" to "json",
            "groovy.util.Node" to "xml",
            "com.centurylink.mdw.xml.XmlBeanWrapper" to "xml",
            "com.centurylink.mdw.model.StringDocument" to "text",
            "com.centurylink.mdw.model.HTMLDocument" to "html",
            "javax.xml.bind.JAXBElement" to "xml",
            "org.apache.camel.component.cxf.CxfPayload" to "xml",
            "com.centurylink.mdw.common.service.Jsonable" to "json",
            "com.centurylink.mdw.model.Jsonable" to "json",
            "org.yaml.snakeyaml.Yaml" to "yaml",
            "java.lang.Exception" to "json",
            "java.util.List<Integer>" to "json",
            "java.util.List<Long>" to "json",
            "java.util.List<String>" to "json",
            "java.util.Map<String,String>" to "json"
        )

        // TODO better groups handling
        // (make this configurable somehow by the user)
        val workgroups = listOf("MDW Support", "Site Admin", "Developers")
    }
}