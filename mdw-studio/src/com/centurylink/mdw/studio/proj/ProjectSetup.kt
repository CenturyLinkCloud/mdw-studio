package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.cli.Operation
import com.centurylink.mdw.cli.ProgressMonitor
import com.centurylink.mdw.cli.Setup
import com.centurylink.mdw.config.PropertyException
import com.centurylink.mdw.config.YamlProperties
import com.centurylink.mdw.constant.PropertyNames
import com.centurylink.mdw.dataaccess.file.VersionControlGit
import com.centurylink.mdw.model.system.MdwVersion
import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.AssetPackage
import com.centurylink.mdw.util.HttpHelper
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager.VFS_CHANGES
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import org.json.JSONException
import org.json.JSONObject
import org.yaml.snakeyaml.error.YAMLException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Thread.sleep
import java.net.URL
import javax.swing.ImageIcon
import kotlin.concurrent.thread

class Startup : StartupActivity {
    override fun runActivity(project: Project) {
        FileTypeManager.getInstance().getFileTypeByExtension("groovy").let {
            WriteAction.run<RuntimeException> {
                FileTypeManager.getInstance().associateExtension(it, "test")
            }
        }
        // start the server detection background thread
        val projectSetup = project.getComponent(ProjectSetup::class.java)
        projectSetup.hubRootUrl?.let {
            val serverCheckUrl = URL(it + "/services/AppSummary")
            thread(true, true, name = "mdwServerDetection") {
                while (true) {
                    sleep(ProjectSetup.SERVER_DETECT_INTERVAL)
                    try {
                        val response = HttpHelper(serverCheckUrl).get()
                        projectSetup.isServerRunning = JSONObject(response).has("mdwVersion")
                    } catch (e: IOException) {
                        projectSetup.isServerRunning = false
                    } catch (e: JSONException) {
                        projectSetup.isServerRunning = false
                    }
                }
            }
        }
    }
}

class ProjectSetup(val project: Project) : ProjectComponent, com.centurylink.mdw.model.Project {

    // setup is null if not an mdw project
    private var setup: Setup? = null
    val assetDir: VirtualFile

    val isMdwProject: Boolean
        get() = setup != null

    override fun getHubRootUrl(): String? {
        return getMdwProp(PropertyNames.MDW_HUB_URL)
    }

    override fun getAssetRoot(): File {
        return File(assetDir.path)
    }

    lateinit var implementors: Implementors
    private var implementorChangeListeners = mutableListOf<ImplementorChangeListener>()
    fun addImplementorChangeListener(listener: ImplementorChangeListener) {
        removeImplementorChangeListener(listener)
        implementorChangeListeners.add(listener)
    }

    fun removeImplementorChangeListener(listener: ImplementorChangeListener) {
        implementorChangeListeners.remove(listener)
    }

    fun reloadImplementors() {
        DumbService.getInstance(project).smartInvokeLater {
            implementors = Implementors(this)
            for (listener in implementorChangeListeners) {
                listener.onChange(implementors)
            }
        }
    }

    private val projectYaml = File(project.basePath + "/project.yaml")

    override fun getMdwVersion(): MdwVersion {
        return if (projectYaml.exists()) {
            MdwVersion(YamlProperties(projectYaml).getString(Props.MDW_VERSION.prop))
        } else {
            MdwVersion(null)
        }
    }

    val git: VersionControlGit?

    var isServerRunning = false

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
        if (setup == null) {
            git = null
        }
        else {
            com.centurylink.mdw.cli.Props.init("mdw.yaml")
            if (setup.gitExists()) {
                git = VersionControlGit()
                git.connect(setup.gitRemoteUrl, setup.gitUser, null, setup.gitRoot)
            }
            else {
                git = null
                val note = Notification("MDW", "MDW Project",
                        """Git root not found: ${setup.gitRoot.absolutePath}.
                            Asset version updates will not be applied.  If you're using Git, fix git.local.path in mdw.yaml and restart IntelliJ.""",
                        NotificationType.ERROR)
                Notifications.Bus.notify(note, project)
            }
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
                mySetup.configLoc = projectYaml.parentFile.absolutePath + "/" + configLoc
                mySetup.assetLoc = projectYaml.parentFile.absolutePath + "/" + assetLoc
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
            DumbService.getInstance(project).smartInvokeLater {
                implementors = Implementors(this)
            }
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
        if (assetPath.startsWith("\${")) {
            return null
        }
        val slash = assetPath.lastIndexOf('/')
        if (slash == -1 || slash > assetPath.length - 2)
            throw IOException("Bad asset path: $assetPath")
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

    fun getPackage(dir: VirtualFile): AssetPackage? {
        if (dir.toString().length < assetDir.toString().length + 2)
            return null
        return getPackage(getPackageName(dir))
    }

    fun getAsset(path: String): Asset? {
        return getAssetFile(path)?.let { getAsset(it) }
    }

    fun getAsset(file: VirtualFile): Asset? {
        val pkg = getPackage(file.parent)
        pkg?.let {
            return Asset(pkg, file)
        }
        return null
    }

    fun findAssetsOfType(ext: String): List<Asset> {
        val assets = mutableListOf<Asset>()
        for (pkg in packages) {
            for (file in pkg.dir.children) {
                if (file.exists() && !file.isDirectory && file.extension == ext) {
                    assets.add(Asset(pkg, file))
                }
            }
        }
        return assets
    }

    fun findAnnotatedAssets(annotation: String): Map<Asset,List<PsiAnnotation>> {
        val annotatedAssets = mutableMapOf<Asset,MutableList<PsiAnnotation>>()
        for (javaAsset in findAssetsOfType("java")) {
            val psiFile = PsiManager.getInstance(project).findFile(javaAsset.file)
            psiFile?.let { assetPsiFile ->
                if (psiFile is PsiClassOwner) {
                    for (psiClass in psiFile.classes) {
                        AnnotationUtil.findAnnotation(psiClass, annotation)?.let { psiAnnotation ->
                            val annotations = annotatedAssets[javaAsset]
                            if (annotations == null) {
                                annotatedAssets[javaAsset] = mutableListOf(psiAnnotation)
                            }
                            else {
                                annotations.add(psiAnnotation)
                            }
                        }
                    }
                }
            }
        }
        for (ktAsset in findAssetsOfType("kt")) {
            val psiFile = PsiManager.getInstance(project).findFile(ktAsset.file)
            psiFile?.let { assetPsiFile ->
                if (psiFile is PsiClassOwner) {
                    for (psiClass in psiFile.classes) {
                        AnnotationUtil.findAnnotation(psiClass, annotation)?.let { psiAnnotation ->
                            val annotations = annotatedAssets[ktAsset]
                            if (annotations == null) {
                                annotatedAssets[ktAsset] = mutableListOf(psiAnnotation)
                            }
                            else {
                                annotations.add(psiAnnotation)
                            }
                        }
                    }
                }
            }
        }
        return annotatedAssets
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
        return WriteAction.compute<AssetPackage,IOException> {
            var metaDir = pkgDir.findFileByRelativePath(AssetPackage.META_DIR)
            if (metaDir == null) {
                metaDir = pkgDir.createChildDirectory(this, AssetPackage.META_DIR)
            }
            val packageYaml = metaDir.findFileByRelativePath(AssetPackage.PACKAGE_YAML)
                    ?: metaDir.createChildData(this, AssetPackage.PACKAGE_YAML)
            ApplicationManager.getApplication().invokeAndWait {
                packageYaml.setBinaryContent(AssetPackage.createPackageYaml(packageName, 1).toByteArray())
            }
            AssetPackage(getPackageName(pkgDir), pkgDir)
        }
    }

    private fun createVersionsFile(pkg: AssetPackage): VirtualFile {
        return WriteAction.compute<VirtualFile,IOException> {
            pkg.metaDir.createChildData(this, AssetPackage.VERSIONS)
        }
    }

    fun setVersion(asset: Asset, version: Int) {
        val verFile = asset.pkg.dir.findFileByRelativePath(AssetPackage.VERSIONS_FILE) ?: createVersionsFile(asset.pkg)
        val versionProps = asset.pkg.versionProps
        if (version > 0) {
            versionProps[asset.name] = version.toString()
        }
        else {
            versionProps.remove(asset.name)
        }
        val out = ByteArrayOutputStream()
        versionProps.store(out, null)
        // ensure any indexing is completed
        DumbService.getInstance(project).smartInvokeLater {
            WriteAction.run<IOException> {
                verFile.setBinaryContent(out.toByteArray())
            }
        }
        setVersion(asset.pkg, asset.pkg.version + 1)
    }

    fun setVersion(pkg: AssetPackage, version: Int) {
        pkg.metaDir.findFileByRelativePath(AssetPackage.PACKAGE_YAML)?.let { pkgYaml ->
            pkg.version = version
            // ensure any indexing is completed
            DumbService.getInstance(project).smartInvokeLater {
                WriteAction.run<IOException> {
                    pkgYaml.setBinaryContent(AssetPackage.createPackageYaml(pkg.name, pkg.version).toByteArray())
                }
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
        val LOG = Logger.getInstance(ProjectSetup::class.java)
        const val SERVER_DETECT_INTERVAL = 3000L

        val isWindows: Boolean by lazy {
            SystemInfo.isWindows
        }

        val isMac: Boolean by lazy {
            SystemInfo.isMac
        }
    }

    enum class Props(val prop: String) {
        ASSET_LOC("project.asset.location"),
        CONFIG_LOC("project.config.location"),
        MDW_VERSION("project.mdw.version")
    }
}