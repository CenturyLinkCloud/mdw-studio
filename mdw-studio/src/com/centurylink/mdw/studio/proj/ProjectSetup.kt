package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.cli.Operation
import com.centurylink.mdw.cli.ProgressMonitor
import com.centurylink.mdw.cli.Setup
import com.centurylink.mdw.config.PropertyException
import com.centurylink.mdw.config.YamlProperties
import com.centurylink.mdw.constant.PropertyNames
import com.centurylink.mdw.dataaccess.file.VersionControlGit
import com.centurylink.mdw.model.system.MdwVersion
import com.centurylink.mdw.studio.MdwSettings
import com.centurylink.mdw.studio.action.AssetUpdate
import com.centurylink.mdw.studio.action.UpdateNotificationAction
import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.AssetPackage
import com.centurylink.mdw.util.HttpHelper
import com.centurylink.mdw.util.file.Packages
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.ide.DataManager
import com.intellij.ide.plugins.PluginManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager.VFS_CHANGES
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.wm.WindowManager
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
import java.util.*
import javax.swing.ImageIcon
import kotlin.concurrent.thread
import kotlin.reflect.KClass

class Startup : StartupActivity {
    override fun runActivity(project: Project) {

        val projectSetup = project.getComponent(ProjectSetup::class.java)
        if (!projectSetup.isMdwProject) {
            // one more try in case new project
            projectSetup.assetDir = projectSetup.initialize()
            projectSetup.configure(false)
        }
        if (projectSetup.isMdwProject) {
            val pluginVer = PluginManager.getPlugin(PluginId.getId(ProjectSetup.PLUGIN_ID))?.version
            try {
                LOG.info("MDW Studio: $pluginVer (${MdwVersion.getRuntimeVersion()})")
                val ideaVer = ApplicationInfo.getInstance().build.asString()
                LOG.info("  IDEA: $ideaVer")
                val kotlinVer = PluginManager.getPlugin(PluginId.getId("org.jetbrains.kotlin"))?.version
                LOG.info("  Kotlin: $kotlinVer")
            }
            catch (ex: Exception) {
                LOG.info("MDW Studio: $pluginVer")
                LOG.warn(ex)
            }

            // reload implementors (after dumb) to pick up annotation-driven impls
            projectSetup.reloadImplementors()

            // check mdw assets
            val updateStatus = AssetUpdate(projectSetup).status
            if (updateStatus.isUpdateNeeded) {
                val note = Notification("MDW", "MDW Assets", updateStatus.reason, NotificationType.WARNING)
                note.addAction(UpdateNotificationAction(projectSetup, "Update MDW Assets"))
                Notifications.Bus.notify(note, project)
            }
            // associate .test files with groovy
            FileTypeManager.getInstance().getFileTypeByExtension("groovy").let {
                WriteAction.run<RuntimeException> {
                    FileTypeManager.getInstance().associateExtension(it, "test")
                }
            }
            // start the server detection background thread
            projectSetup.hubRootUrl?.let {
                val serverCheckUrl = URL("$it/services/AppSummary")
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
            MdwSettings.instance.getOrMakeMdwHome()
        }
    }
    companion object {
        val LOG = Logger.getInstance(Startup::class.java)
    }
}

class ProjectSetup(val project: Project) : ProjectComponent, com.centurylink.mdw.model.Project {

    // setup is null if not an mdw project
    private var setup: Setup? = null
    internal var assetDir: VirtualFile

    val isMdwProject: Boolean
        get() = setup != null

    override fun getHubRootUrl(): String? {
        return getMdwProp(PropertyNames.MDW_HUB_URL)
    }

    override fun getAssetRoot(): File {
        return File(assetDir.path)
    }

    private val projectYaml = File(project.basePath + "/project.yaml")

    override fun getMdwVersion(): MdwVersion {
        return if (projectYaml.exists()) {
            MdwVersion(YamlProperties(projectYaml).getString(Props.MDW_VERSION.prop))
        } else {
            MdwVersion(null)
        }
    }

    var git: VersionControlGit? = null

    var isServerRunning = false

    // pass-through properties
    val configLoc: String?
        get() = setup?.configLoc

    val packages: List<AssetPackage>
        get() {
            val pkgs = mutableListOf<AssetPackage>()
            VfsUtilCore.iterateChildrenRecursively(assetDir, { it.isDirectory }, {
                if (it.isDirectory && it.findFileByRelativePath(AssetPackage.META_FILE) != null && !AssetPackage.isIgnore(it)) {
                    try {
                        pkgs.add(AssetPackage(getPackageName(it), it))
                    }
                    catch (e: Exception) {
                        when(e) {
                            is FileNotFoundException -> { LOG.warn(e)}
                            is YAMLException -> { LOG.warn("Bad meta (YAMLException): $it", e)}
                            else -> throw e
                        }
                    }
                }
                true
            })
            return pkgs
        }

    private val iconAssets = mutableMapOf<String,ImageIcon>()

    private var _implementors: Implementors? = null
    val implementors: Implementors
        get() {
            if (_implementors == null) {
                _implementors = Implementors(this)
            }
            return _implementors as Implementors
        }

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
            _implementors = Implementors(this)
            for (listener in implementorChangeListeners) {
                listener.onChange(implementors)
            }
        }
    }

    init {
        assetDir = initialize()
        configure()
        if (isMdwProject) {
            DumbService.getInstance(project).smartInvokeLater {
                implementors // trigger implementor load
            }
        }
    }

    /**
     * Creates a setup instance reflecting project.yaml asset and config locations.
     */
    internal fun initialize(): VirtualFile {
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
                mySetup.assetLoc = projectYaml.parentFile.absolutePath + "/" + assetLoc
                mySetup.configLoc = projectYaml.parentFile.absolutePath + "/" + configLoc
                setup = mySetup
                return LocalFileSystem.getInstance().findFileByIoFile(mySetup.assetRoot)!!
            }
            else {
                throw PropertyException("Missing: ${mdwYaml.absolutePath}")
            }
        }
        return project.baseDir
    }

    /**
     * Initializes file listening and git.
     */
    internal fun configure(warnGitNotFound: Boolean = true) {
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
                git?.connect(setup.gitRemoteUrl, setup.gitUser, null, setup.gitRoot)
            }
            else {
                git = null
                if (warnGitNotFound) {
                    val msg = """Git root not found: ${setup.gitRoot.absolutePath}.
                            Asset version updates will not be applied.  If you're using Git, fix git.local.path in mdw.yaml and restart IntelliJ."""
                    LOG.warn(msg)
                    val note = Notification("MDW", "MDW Project", msg, NotificationType.INFORMATION)
                    Notifications.Bus.notify(note, project)
                }
            }
        }
    }

    override fun projectOpened() {
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
        assets.sort()
        return assets
    }

    fun findAnnotatedAssets(annotation: KClass<*>): Map<Asset,List<PsiAnnotation>> {

        val annotatedAssets = mutableMapOf<Asset,MutableList<PsiAnnotation>>()
        for (javaAsset in findAssetsOfType("java")) {
            findPsiAnnotation(javaAsset, annotation)?.let { psiAnnotation ->
                val annotations = annotatedAssets[javaAsset]
                if (annotations == null) {
                    annotatedAssets[javaAsset] = mutableListOf(psiAnnotation)
                } else {
                    annotations.add(psiAnnotation)
                }
            }
        }
        for (ktAsset in findAssetsOfType("kt")) {
            findPsiAnnotation(ktAsset, annotation)?.let { psiAnnotation ->
                val annotations = annotatedAssets[ktAsset]
                if (annotations == null) {
                    annotatedAssets[ktAsset] = mutableListOf(psiAnnotation)
                } else {
                    annotations.add(psiAnnotation)
                }
            }
        }
        return annotatedAssets
    }

    fun findPsiAnnotation(asset: Asset, annotation: KClass<*>): PsiAnnotation? {
        val psiFile = PsiManager.getInstance(project).findFile(asset.file)
        psiFile?.let { _ ->
            if (psiFile is PsiClassOwner) {
                for (psiClass in psiFile.classes) {
                    return AnnotationUtil.findAnnotation(psiClass, true, annotation.qualifiedName)
                }
            }
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
    fun createPackage(packageName: String): AssetPackage {
        val pkgDir = assetDir.findFileByRelativePath(packageName.replace('.', '/'))
        pkgDir ?: throw IOException("Package directory not found: $packageName")
        return createPackage(pkgDir)
    }

    fun createPackage(packageDir: VirtualFile): AssetPackage {
        return WriteAction.compute<AssetPackage,IOException> {
            ApplicationManager.getApplication().invokeAndWait {
                var metaDir = packageDir.findFileByRelativePath(AssetPackage.META_DIR)
                if (metaDir == null) {
                    metaDir = packageDir.createChildDirectory(this, AssetPackage.META_DIR)
                }
                val packageYaml = metaDir.findFileByRelativePath(AssetPackage.PACKAGE_YAML)
                        ?: metaDir.createChildData(this, AssetPackage.PACKAGE_YAML)
                val packageName = packageDir.path.substring(assetDir.path.length + 1).replace('/', '.')
                packageYaml.setBinaryContent(AssetPackage.createPackageYaml(packageName, 1).toByteArray())
            }
            AssetPackage(getPackageName(packageDir), packageDir)
        }
    }

    private fun createVersionsFile(pkg: AssetPackage): VirtualFile {
        return WriteAction.compute<VirtualFile,IOException> {
            ApplicationManager.getApplication().invokeAndWait {
                pkg.metaDir.createChildData(this, AssetPackage.VERSIONS)
            }
            pkg.metaDir.findFileByRelativePath(AssetPackage.VERSIONS)
        }
    }

    fun setVersion(asset: Asset, version: Int) {
        if (version == 0) {
            // don't create the versions file just to remove the version
            if (asset.pkg.dir.findFileByRelativePath(AssetPackage.VERSIONS_FILE) == null) {
                return
            }
        }

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
        if (!Packages.isMdwPackage(asset.pkg.name)) {
            setVersion(asset.pkg, asset.pkg.version + 1)
        }
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

    fun syncPackage(pkg: AssetPackage) {
        pkg.dir.refresh(true, true)
    }

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

    override fun readData(name: String): String? {
        return YamlProperties(projectYaml).getString(name)
    }

    override fun readDataList(name: String): List<String>? {
        return YamlProperties(projectYaml).getList(name)
    }

    override fun readDataMap(name: String): SortedMap<String,String>? {
        return YamlProperties(projectYaml).getMap(name)?.toSortedMap()
    }

    companion object {
        const val PLUGIN_ID = "com.centurylink.mdw.studio"
        val LOG = Logger.getInstance(ProjectSetup::class.java)
        const val SERVER_DETECT_INTERVAL = 3000L

        val isWindows: Boolean by lazy {
            SystemInfo.isWindows
        }

        val isMac: Boolean by lazy {
            SystemInfo.isMac
        }

        /**
         * TODO: not good
         */
        val activeProject: Project?
            get() {
                val openProjects = ProjectManager.getInstance().openProjects
                if (openProjects.size == 1) {
                    return openProjects[0]
                }
                val project = DataManager.getInstance().dataContextFromFocus.result?.getData(CommonDataKeys.PROJECT.name)
                if (project is Project) {
                    return project
                }
                return openProjects.find { project ->
                    val win = WindowManager.getInstance().suggestParentWindow(project)
                    win?.isActive ?: false
                }
            }
    }

    enum class Props(val prop: String) {
        ASSET_LOC("project.asset.location"),
        CONFIG_LOC("project.config.location"),
        MDW_VERSION("project.mdw.version")
    }
}