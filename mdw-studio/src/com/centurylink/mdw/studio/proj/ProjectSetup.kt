package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.cli.Operation
import com.centurylink.mdw.cli.ProgressMonitor
import com.centurylink.mdw.cli.Setup
import com.centurylink.mdw.config.PropertyException
import com.centurylink.mdw.config.PropertyGroup
import com.centurylink.mdw.config.YamlProperties
import com.centurylink.mdw.constant.PropertyNames
import com.centurylink.mdw.git.VersionControlGit
import com.centurylink.mdw.file.Packages
import com.centurylink.mdw.java.JavaNaming
import com.centurylink.mdw.model.project.Data
import com.centurylink.mdw.model.system.MdwVersion
import com.centurylink.mdw.model.workflow.ActivityImplementor
import com.centurylink.mdw.studio.console.MdwConsole
import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.AssetPackage
import com.centurylink.mdw.studio.file.AttributeDocumentHandler
import com.centurylink.mdw.studio.file.AttributeVirtualFileSystem
import com.centurylink.mdw.studio.prefs.MdwSettings
import com.centurylink.mdw.util.log.slf4j.Slf4JStandardLoggerImpl
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.ide.DataManager
import com.intellij.ide.plugins.PluginManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager.VFS_CHANGES
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.yaml.snakeyaml.error.YAMLException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import javax.swing.ImageIcon
import kotlin.reflect.KClass

class ProjectSetup(val project: Project) : ProjectComponent, com.centurylink.mdw.model.project.Project {

    // setup is null if not an mdw project
    private var setup: Setup? = null
    var assetDir: VirtualFile

    val isMdwProject: Boolean
        get() = setup != null

    val baseDir = project.baseDir

    override fun getHubRootUrl(): String? {
        return getMdwProp(PropertyNames.MDW_HUB_URL)
    }

    override fun getAssetRoot(): File {
        return File(assetDir.path)
    }

    fun getMdwCentralUrl(): String {
        val url = getMdwProp(PropertyNames.MDW_CENTRAL_URL)
        if (url != null)
            return url
        return "https://mdw-central.com"
    }

    val milestoneGroups: PropertyGroup? by lazy {
        setup?.mdwConfig?.let {
            val mdwYaml = YamlProperties("mdw", File("${setup?.configRoot ?: ""}/$it"))
            mdwYaml.getGroup(PropertyNames.MDW_MILESTONE_GROUPS)?.let { map ->
                val props = Properties()
                for ((k, v) in map) {
                    props[k] = v
                }
                PropertyGroup("milestone.groups", PropertyNames.MDW_MILESTONE_GROUPS, props)
            }
        }
    }

    fun getPluginVersion() : String? {
        return PluginManager.getPlugin(PluginId.getId(ProjectSetup.PLUGIN_ID))?.version
    }

    val projectYaml = File(project.basePath + "/project.yaml")

    override fun getMdwVersion(): MdwVersion {
        return if (projectYaml.exists()) {
            MdwVersion(YamlProperties(projectYaml).getString(Props.MDW_VERSION.prop))
        } else {
            MdwVersion(null)
        }
    }

    var git: VersionControlGit? = null
    val gitRoot: File?
        get() = setup?.gitRoot

    var tempDir: File? = null

    var isServerRunning = false

    val isFramework: Boolean
      get() = File("${project.basePath}/../mdw-common/src/com/centurylink/mdw/activity/ActivityException.java").isFile

    val settings = ProjectSettings(project)

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
                            is YAMLException -> {
                                LOG.warn(e)
                                val note = Notification("MDW", "Invalid Package", "${e.message}", NotificationType.WARNING)
                                Notifications.Bus.notify(note, project)
                            }
                            else -> throw e
                        }
                    }
                }
                true
            })
            return pkgs
        }

    val packageMetaFiles: List<VirtualFile>
        get() {
            val pkgYamlFiles = mutableListOf<VirtualFile>()
            for (pkg in packages) {
                pkgYamlFiles.add(pkg.metaFile)
            }
            return pkgYamlFiles
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

    /**
     * For Project interface.  Supply activity implementors coming from compiled code annotations.
     */
    override fun getActivityImplementors(): Map<String,ActivityImplementor> {
        return implementors.toSortedMap()
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

    lateinit var attributeDocumentHandler: AttributeDocumentHandler

    init {
        assetDir = initialize()
        configure()
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
        return baseDir
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
            tempDir = null
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
            tempDir = setup.tempDir
        }
    }

    private val _data : Data by lazy { Data(this) }
    override fun getData(): Data {
        return _data
    }

    override fun projectOpened() {
        if (isMdwProject) {
            attributeDocumentHandler = AttributeDocumentHandler(this)
            EditorFactory.getInstance().eventMulticaster.addDocumentListener(attributeDocumentHandler)
        }
    }

    override fun projectClosed() {
        if (isMdwProject) {
            EditorFactory.getInstance().eventMulticaster.removeDocumentListener(attributeDocumentHandler)
            AttributeVirtualFileSystem.instance.clear(this)
        }
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

    /**
     * Finds assets in the designated package that match extension based on their
     * normalized Java name.
     */
    fun findAssetFromNormalizedName(packageName: String, name: String, ext: String): Asset? {
        getPackage(packageName)?.let { pkg ->
            for (file in pkg.dir.children) {
                if (file.exists() && !file.isDirectory && !Asset.isIgnore(file) && file.extension == ext) {
                    if (JavaNaming.getValidClassName(file.nameWithoutExtension) == name) {
                        return Asset(pkg, file)
                    }
                }
            }
        }
        return null
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

    /**
     * Finds all annotations from project source
     */
    fun findAnnotatedSource(annotation: PsiClass): Map<PsiClass,PsiAnnotation> {
        val annotatedSources = mutableMapOf<PsiClass,PsiAnnotation>()
        AnnotatedElementsSearch.searchPsiClasses(annotation, GlobalSearchScope.allScope(project)).forEach { psiClass ->
            val psiAnnotation = AnnotationUtil.findAnnotation(psiClass, true, annotation.qualifiedName)
            if (psiAnnotation != null) {
                annotatedSources[psiClass] = psiAnnotation
            }
        }
        return annotatedSources
    }

    /**
     * Finds annotations in Java or Kotlin assets.
     */
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
    private fun createPackage(packageName: String): AssetPackage {
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
                packageYaml.setBinaryContent(AssetPackage.createPackageYaml(packageName, "0.0.01").toByteArray())
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
        if (!Packages.isMdwPackage(asset.pkg.name) && !asset.pkg.snapshot && !MdwSettings.instance.isSuppressPackageIncrement) {
            setVersion(asset.pkg, asset.pkg.version + 1)
        }
    }

    fun setVersion(pkg: AssetPackage, version: Int) {
        pkg.metaDir.findFileByRelativePath(AssetPackage.PACKAGE_YAML)?.let { pkgYaml ->
            pkg.version = version
            // ensure any indexing is completed
            DumbService.getInstance(project).smartInvokeLater {
                WriteAction.run<IOException> {
                    pkgYaml.setBinaryContent(pkg.yaml.toByteArray())
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

    fun getVirtualFile(file: File): VirtualFile? {
        val basePath = File(baseDir.path).absolutePath.replace('\\', '/')
        val filePath = if (file.isAbsolute) {
            file.absolutePath.replace('\\', '/')
        }
        else {
            File(baseDir.path + "/" + file.path).absolutePath.replace('\\', '/')
        }
        return if (filePath.startsWith(basePath) && filePath.length > basePath.length) {
            baseDir.findFileByRelativePath(filePath.substring(basePath.length))
        }
        else {
            null
        }
    }

    fun markExcluded(dir: VirtualFile) {
        val dataContext = object: DataContext {
            override fun getData(dataId: String): Any? {
                return if (dataId == CommonDataKeys.VIRTUAL_FILE_ARRAY.name) {
                    arrayOf(dir)
                } else if (dataId == CommonDataKeys.PROJECT.name) {
                    project
                } else {
                    null
                }
            }
        }
        val markExcludeAction = ActionManager.getInstance().getAction("MarkExcludeRoot")
        val actionEvent = AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN,
                Presentation(), ActionManager.getInstance(), 0)
        markExcludeAction.actionPerformed(actionEvent)
    }

    val hasPackageDependencies: Boolean
        get() {
            for (pkg in packages) {
                if (pkg.dependencies.isNotEmpty()) {
                    return true
                }
            }
            return false
        }

    val mdwLibraryDependencies: Map<String,MdwVersion>
        get() {
            val libs = mutableMapOf<String,MdwVersion>()
            for (module in ModuleManager.getInstance(project).modules) {
                for (orderEntry in ModuleRootManager.getInstance(module).orderEntries) {
                    if (orderEntry is LibraryOrderEntry) {
                        val nameVer: Pair<String,MdwVersion>? = orderEntry.libraryName?.let { libName ->

                            if (libName.startsWith("Gradle: com.centurylink.mdw:")) {
                                val colon = libName.indexOf(":", 28)
                                val name = libName.substring(28, colon)
                                Pair(name, MdwVersion(libName.substring(colon + 1)))
                            } else if (libName.startsWith("Maven: com.centurylink.mdw:")) {
                                val colon = libName.indexOf(":", 29)
                                val name = libName.substring(29, colon)
                                Pair(name, MdwVersion(libName.substring(colon + 1)))
                            } else {
                                null
                            }
                        }
                        if (nameVer != null) {
                            val exist = libs[nameVer.first]
                            if (exist == null || exist.compareTo(nameVer.second) < 1) {
                                libs.put(nameVer.first, nameVer.second)
                            }
                        }
                    }
                }
            }
            return libs
        }

    @Volatile private var mdwConsole: MdwConsole? = null
    val console: MdwConsole
        get() {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            var toolWindow = toolWindowManager.getToolWindow(MdwConsole.ID)
            if (toolWindow == null) {
                toolWindow = toolWindowManager.registerToolWindow(MdwConsole.ID, false,
                        ToolWindowAnchor.BOTTOM)
            }
            return mdwConsole ?: synchronized(this) {
                mdwConsole ?: MdwConsole(this, toolWindow).also { mdwConsole = it }
            }
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
        init {
            System.setProperty("mdw.logger.impl", Slf4JStandardLoggerImpl::class.java.name)
        }

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
                return openProjects.find { proj ->
                    val win = WindowManager.getInstance().suggestParentWindow(proj)
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