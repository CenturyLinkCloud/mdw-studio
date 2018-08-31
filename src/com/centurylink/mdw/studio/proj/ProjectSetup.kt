package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.cli.Operation
import com.centurylink.mdw.cli.ProgressMonitor
import com.centurylink.mdw.cli.Setup
import com.centurylink.mdw.config.PropertyException
import com.centurylink.mdw.config.YamlProperties
import com.centurylink.mdw.constant.PropertyNames
import com.centurylink.mdw.dataaccess.file.VersionControlGit
import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.AssetPackage
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager.VFS_CHANGES
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import org.yaml.snakeyaml.error.YAMLException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
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

    val hubRootUrl: String?
        get() {
            return getMdwProp(PropertyNames.MDW_HUB_URL)
        }

    lateinit var implementors: Implementors

    private val projectYaml = File(project.basePath + "/project.yaml")

    val git: VersionControlGit?

    val packages: List<AssetPackage>
        get() {
            val pkgs = mutableListOf<AssetPackage>()
            VfsUtilCore.iterateChildrenRecursively(assetDir, { it.isDirectory}, {
                if (it.isDirectory && it.findFileByRelativePath(AssetPackage.META_FILE) != null) {
                    try {
                        pkgs.add(AssetPackage(getPackageName(it), it))
                    }
                    catch (e: Exception) {
                        when(e) {
                            is FileNotFoundException -> { LOG.error(e)}
                            is YAMLException -> { LOG.error("Bad meta (YAMLException): $it", e)}
                            else -> throw e
                        }
                    }
                }
                true
            })
            return pkgs
        }

    init {
        assetDir = initialize()
        if (isMdwProject) {
            packages // trigger iterate children to load vfs
            val connection = project.messageBus.connect(project)
            connection.subscribe<BulkFileListener>(VFS_CHANGES, AssetFileListener(this))
        }
        val setup = this.setup
        if (setup == null || !setup.gitExists()) {
            git = null
        }
        else {
            git = VersionControlGit()
            git.connect(setup.gitRemoteUrl, setup.gitUser, null, setup.gitRoot)
        }
    }

    private fun initialize(): VirtualFile {
        if (projectYaml.exists()) {
            val yamlProps = YamlProperties(projectYaml)
            val assetLoc = yamlProps.getString(Props.ASSET_LOC.prop)
            assetLoc ?: throw PropertyException("Missing from project.yaml: ${Props.ASSET_LOC.prop}")
            val configLoc = yamlProps.getString(Props.CONFIG_LOC.prop)
            configLoc ?: throw PropertyException("Missing from project.yaml: ${Props.CONFIG_LOC.prop}")
            val mdwYaml = File("${project.basePath}/$configLoc/mdw.yaml")
            if (mdwYaml.exists()) {
                val mySetup = object : Setup(File(project.basePath)) {
                    override fun run(vararg progressMonitors: ProgressMonitor?): Operation {
                        return this
                    }
                }
                mySetup.configLoc = mdwYaml.parent
                mySetup.assetLoc = assetLoc
                setup = mySetup
                return LocalFileSystem.getInstance().findFileByIoFile(mySetup.assetRoot)!!
            }
            else {
                throw PropertyException("Missing: ${mdwYaml.absolutePath}")
            }
        }
        return project.baseDir
    }

    override fun projectOpened() {
        if (isMdwProject) {
            implementors = Implementors(this)
        }
    }

    override fun projectClosed() {
    }

    private fun getMdwProp(prop: String): String? {
        return setup?.mdwConfig?.let {
            YamlProperties("mdw", File("${setup?.configRoot ?: ""}/$it")).getString(prop)
        }
    }

    /**
     * Returns true even if dir is a <s>potential</s> asset dir.
     */
    fun isAssetSubdir(dir: VirtualFile): Boolean {
        var parentDir: VirtualFile? = dir.parent
        while (parentDir != null) {
            if (parentDir == assetDir) {
                return true
            }
            parentDir = parentDir.parent
        }
        return false
    }

    fun isAssetParent(rootDir: VirtualFile): Boolean {
        var dir: VirtualFile? = assetDir
        while (dir != null) {
            if (dir == rootDir) {
                return true
            }
            dir = dir.parent
        }
        return false
    }

    /**
     * Returns null if not found
     */
    fun getPackage(name: String): AssetPackage? {
        return packages.find {
            it.name == name
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

    private fun getPackageName(packageDir: VirtualFile): String {
        return packageDir.toString().substring(assetDir.toString().length + 1)
                .replace('/', '.').replace('\\', '.')
    }

    fun getPackage(dir: VirtualFile): AssetPackage? {
        if (dir.toString().length < assetDir.toString().length + 2)
            return null
        return getPackage(getPackageName(dir))
    }

    fun getAsset(file: VirtualFile): Asset? {
        val pkg = getPackage(file.parent)
        pkg?.let {
            return Asset(pkg, file)
        }
        return null
    }

    fun createAsset(file: VirtualFile): Asset {
        val pkgName = getPackageName(file.parent)
        val pkg = getPackage(pkgName) ?: createPackage(pkgName)
        val asset = Asset(pkg, file)
        setVersion(asset, 1)
        return asset
    }

    /**
     * Expects the directory to be already there, and creates .mdw/package.yaml metafile.
     */
    private fun createPackage(packageName: String): AssetPackage {
        val pkgDir = assetDir.findFileByRelativePath(packageName.replace('.', '/'))
        pkgDir ?: throw IOException("Package directory not found: $packageName")
        val metaDir = WriteAction.compute<VirtualFile,IOException> {
            var metaDir = pkgDir.findFileByRelativePath(AssetPackage.META_DIR)
            if (metaDir == null) {
                metaDir = pkgDir.createChildDirectory(this, AssetPackage.META_DIR)
            }
            val packageYaml = metaDir.findFileByRelativePath(AssetPackage.PACKAGE_YAML) ?:
                    metaDir.createChildData(this, AssetPackage.PACKAGE_YAML)
            packageYaml.setBinaryContent(AssetPackage.createPackageYaml(packageName, 1).toByteArray())
            metaDir
        }
        return AssetPackage(getPackageName(pkgDir), pkgDir)
    }

    private fun createVersionsFile(pkg: AssetPackage): VirtualFile {
        return WriteAction.compute<VirtualFile,IOException> {
            pkg.metaDir.createChildData(this, AssetPackage.VERSIONS)
        }
    }

    fun setVersion(asset: Asset, version: Int) {
        val verFile = asset.pkg.dir.findFileByRelativePath(AssetPackage.VERSIONS_FILE) ?: createVersionsFile(asset.pkg)
        val versionProps = asset.pkg.versionProps
        WriteAction.run<IOException> {
            if (version > 0) {
                versionProps[asset.name] = version.toString()
            }
            else {
                versionProps.remove(asset.name)
            }
            val out = ByteArrayOutputStream()
            versionProps.store(out, null)
            verFile.setBinaryContent(out.toByteArray())
            setVersion(asset.pkg, asset.pkg.version + 1)
        }
    }

    fun setVersion(pkg: AssetPackage, version: Int) {
        WriteAction.run<IOException> {
            pkg.metaDir.findFileByRelativePath(AssetPackage.PACKAGE_YAML)?.let { pkgYaml ->
                pkg.version = version
                pkgYaml.setBinaryContent(AssetPackage.createPackageYaml(pkg.name, pkg.version).toByteArray())
            }
        }
    }

    private val iconAssets = mutableMapOf<String,ImageIcon>()
    fun getIconAsset(assetPath: String): ImageIcon? {
        var icon = iconAssets[assetPath]
        if (icon == null) {
            val asset = getAssetFile(assetPath)
            if (asset != null) {
                icon = ImageIcon(asset.contentsToByteArray())
            }
        }
        return icon
    }

    companion object {
        val LOG = Logger.getInstance(ProjectSetup.javaClass)

        // TODO these values should not be static and should not be hardcoded
        val HUB_ROOT = "http://localhost:8080/mdw"

        const val SOURCE_REPO_URL = "https://github.com/CenturyLinkCloud/mdw"
        const val HELP_LINK_URL = "http://centurylinkcloud.github.io/mdw/docs"

        val isWindows: Boolean by lazy {
            SystemInfo.isWindows
        }

        val isMac: Boolean by lazy {
            SystemInfo.isMac
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
        val categories = mapOf(
                "Ordering" to "ORD",
                "General Inquiry" to "GEN",
                "Billing" to "BIL",
                "Complaint" to "COM",
                "Portal Support" to "POR",
                "Training" to "TRN",
                "Repair" to "RPR",
                "Inventory" to "INV",
                "Test" to "TST",
                "Vacation Planning" to "VAC",
                "Customer Contact" to "CNT"
        )
    }

    enum class Props(val prop: String) {
        ASSET_LOC("project.asset.location"),
        CONFIG_LOC("project.config.location")
    }
}