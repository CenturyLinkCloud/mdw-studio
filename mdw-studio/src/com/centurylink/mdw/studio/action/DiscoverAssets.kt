package com.centurylink.mdw.studio.action

import com.centurylink.mdw.discovery.GitDiscoverer
import com.centurylink.mdw.discovery.GitHubDiscoverer
import com.centurylink.mdw.discovery.GitLabDiscoverer
import com.centurylink.mdw.model.project.Data
import com.centurylink.mdw.studio.prefs.MdwConfig
import com.centurylink.mdw.studio.prefs.MdwSettings
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.centurylink.mdw.studio.ui.LinkCheckBoxList
import com.centurylink.mdw.studio.ui.LinkListListener
import com.centurylink.mdw.studio.ui.widgets.LinkLabel
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBSplitter
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.concurrency.SwingWorker
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.IOException
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class DiscoverAssets : AssetToolsAction() {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).projectSetup?.let { projectSetup ->
            val discoveryDialog = DiscoveryDialog(projectSetup)
            if (discoveryDialog.showAndGet()) {
                discoveryDialog.selectedDiscoverer?.let { discoverer ->
                    discoveryDialog.selectedPackages?.let { packages ->
                        val conflicts = mutableListOf<String>()
                        for (pkg in packages) {
                            projectSetup.getPackage(pkg)?.let { localPkg ->
                                conflicts.add("${localPkg.name} v${localPkg.verString}")
                            }
                        }
                        if (conflicts.isEmpty() || MessageDialogBuilder
                                .yesNo("Overwrite Existing Packages?",
                                        "Overwrite these local packages (and their subpackages) with ${discoverer.ref} from ${discoverer.repoName}?\n\n" +
                                                "  " + '\u2022' + " " + conflicts.joinToString("\n  " + '\u2022' + " "))
                                .show() == Messages.YES) {
                            if (discoverer.repoUrl.toString() == Data.GIT_REPO_URL) {
                                // import from maven central (honoring ref)
                                AssetUpdate(projectSetup).doUpdate(packages)
                            }
                            else {
                                // import from git repository
                                GitImport(projectSetup, discoverer).doImport(packages)
                            }
                        }
                    }
                }
            }
        }
    }
}

class DiscoveryDialog(projectSetup: ProjectSetup) : DialogWrapper(projectSetup.project) {

    private val centerPanel = object: JPanel(BorderLayout()) {
        override fun getMinimumSize(): Dimension {
            return Dimension(780, super.getMinimumSize().width)
        }
    }
    private val okButton: JButton?
        get() = getButton(okAction)

    private val rootNode = DefaultMutableTreeNode("Discovery Repositories")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree: Tree
    private val packageList = LinkCheckBoxList()
    private val buttonPanel = object: JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {
        override fun getPreferredSize(): Dimension {
            return Dimension(super.getPreferredSize().width, BTM_HT)
        }
    }
    private val allButton = JButton("Select All")
    private val noneButton = JButton("Select None")

    var selectedDiscoverer: GitDiscoverer? = null
    var selectedPackages: List<String>? = null

    init {
        init()
        title = "MDW Asset Discovery"
        okButton?.isEnabled = false

        MdwSettings.instance.discoverers.forEach { discoverer ->
            val discovererNode = DefaultMutableTreeNode(discoverer)
            discovererNode.add(RefsNode(discoverer, RefsNode.RefType.Tags))
            discovererNode.add(RefsNode(discoverer, RefsNode.RefType.Branches))
            rootNode.add(discovererNode)
        }

        val treePanel = JPanel(BorderLayout(5, 5))
        treePanel.add(JLabel("Select a Repository Tag or Branch:"), BorderLayout.NORTH)

        tree = Tree()
        tree.model = treeModel
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.alignmentX = Component.LEFT_ALIGNMENT
        tree.border = BorderFactory.createEmptyBorder()
        tree.cellRenderer = object: NodeRenderer() {
            override fun customizeCellRenderer(tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
                val selectable = leaf
                super.customizeCellRenderer(tree, value, selectable && selected, expanded, leaf, row, selectable && hasFocus)
            }
        }
        tree.addTreeWillExpandListener(object: TreeWillExpandListener {
            override fun treeWillExpand(event: TreeExpansionEvent) {
                val path = event.path
                if (path.lastPathComponent is RefsNode) {
                    val refsNode = path.lastPathComponent as RefsNode
                    refsNode.add(DefaultMutableTreeNode("Loading...", false))
                    object: SwingWorker<Any?>() {
                        override fun construct(): Any? {
                            return try {
                                refsNode.refs
                                null
                            } catch(ex: IOException) {
                                LOG.warn(ex)
                                ex
                            }
                        }
                        override fun finished() {
                            refsNode.removeAllChildren()
                            val ex = get()
                            if (ex == null) {
                                refsNode.refs.forEach { ref ->
                                    refsNode.add(DefaultMutableTreeNode(ref))
                                }
                            }
                            else {
                                JOptionPane.showMessageDialog(centerPanel, (ex as Exception).message,
                                        "Git Retrieval Error", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)
                            }
                            treeModel.nodeStructureChanged(refsNode)
                        }
                    }.start()
                }
            }
            override fun treeWillCollapse(event: TreeExpansionEvent) {
            }
        })
        tree.addTreeSelectionListener {
            okButton?.isEnabled = false
            tree.lastSelectedPathComponent?.let {
                if (it is DefaultMutableTreeNode) {
                    if (it.isLeaf) {
                        packageList.setEmptyText("Loading...")
                        packageList.setItems(listOf<String>(), null)
                        packageList.linkListener = null
                        packageList.repaint()
                        val refsNode = it.parent as RefsNode
                        val discoverer = refsNode.discoverer
                        discoverer.ref = it.userObject as String
                        object: SwingWorker<Any?>() {
                            override fun construct(): Any? {
                                return try {
                                    val packageInfo = discoverer.packageInfo
                                    selectedDiscoverer = discoverer
                                    packageInfo
                                } catch(ex: IOException) {
                                    LOG.warn(ex)
                                    ex
                                }
                            }
                            override fun finished() {
                                packageList.setEmptyText("Nothing to show")
                                val res = get()
                                if (res is Exception) {
                                    JOptionPane.showMessageDialog(centerPanel, res.message,
                                            "Git Retrieval Error", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)
                                }
                                else {
                                    val packageInfo = res as Map<*,*>
                                    for (pkg in discoverer.packages) {
                                        packageList.addItem(pkg.toString(), packageInfo[pkg].toString(), false)
                                    }
                                    packageList.linkListener = object: LinkListListener {
                                        override fun itemLinkClicked(index: Int) {
                                            selectedDiscoverer?.let { discoverer ->
                                                packageList.getItemAt(index)?.let { pkg ->
                                                    var url = discoverer.repoUrl.toString()
                                                    if (discoverer.repoUrl.query != null) {
                                                        url = url.substring(0, url.length - discoverer.repoUrl.query.length - 1)
                                                    }
                                                    url = url.substring(0, url.length - discoverer.repoUrl.path.length)
                                                    if (discoverer.repoPath != null) {
                                                        url += "/${discoverer.repoPath}"
                                                    }
                                                    val pkgPath = pkg.replace('.', '/')
                                                    if (discoverer is GitHubDiscoverer) {
                                                        url += "/blob/${discoverer.ref}/${discoverer.assetPath}/$pkgPath/readme.md"
                                                        BrowserUtil.browse(url)
                                                    }
                                                    else if (discoverer is GitLabDiscoverer) {

                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                packageList.repaint()
                                allButton.isVisible = packageList.model.size > 0
                                noneButton.isVisible = packageList.model.size > 0
                                buttonPanel.revalidate()
                                buttonPanel.repaint()
                            }
                        }.start()
                    }
                    else {
                        packageList.setEmptyText("Nothing to show")
                        packageList.setItems(listOf<String>(), null)
                        packageList.repaint()
                    }
                }
            }
        }

        treePanel.add(JScrollPane(tree), BorderLayout.CENTER)

        // link to prefs
        val link = LinkLabel("Change discovery repositories...")
        link.alignmentX = Component.LEFT_ALIGNMENT
        link.clickListener = {
            ShowSettingsUtil.getInstance().showSettingsDialog(projectSetup.project, MdwConfig::class.java)
            this@DiscoveryDialog.close(0)
        }
        val linkPanel = object: JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {
            override fun getPreferredSize(): Dimension {
                return Dimension(super.getPreferredSize().width, BTM_HT)
            }
        }
        linkPanel.add(link)
        treePanel.add(linkPanel, BorderLayout.SOUTH)

        val packagePanel = JPanel(BorderLayout(5, 5))
        packagePanel.add(JLabel("Packages to Import:"), BorderLayout.NORTH)

        packageList.border = BorderFactory.createEmptyBorder()
        packageList.setCheckBoxListListener { _, _ ->
            val packages = mutableListOf<String>()
            for (i in 0 until packageList.model.size) {
                if (packageList.isItemSelected(i)) {
                    packageList.getItemAt(i)?.let { packages.add(it) }
                }
            }
            selectedPackages = packages
            okButton?.isEnabled = !packages.isEmpty()
        }
        packagePanel.add(JScrollPane(packageList), BorderLayout.CENTER)

        allButton.addActionListener {
            for (i in 0 until packageList.model.size) {
                packageList.setItemSelected(packageList.getItemAt(i), true)
            }
            packageList.repaint()
            okButton?.isEnabled = true
        }
        allButton.isVisible = false
        buttonPanel.add(allButton)
        noneButton.addActionListener {
            for (i in 0 until packageList.model.size) {
                packageList.setItemSelected(packageList.getItemAt(i), false)
            }
            packageList.repaint()
            okButton?.isEnabled = false
        }
        noneButton.isVisible = false
        buttonPanel.add(noneButton)
        packagePanel.add(buttonPanel, BorderLayout.SOUTH)

        val splitter = JBSplitter(false, 0.5f)
        splitter.firstComponent = treePanel
        splitter.secondComponent = packagePanel
        centerPanel.add(splitter, BorderLayout.CENTER)
    }

    override fun createCenterPanel(): JComponent {
        return centerPanel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return tree
    }

    companion object {
        val LOG = Logger.getInstance(DiscoverAssets::class.java)
        const val BTM_HT = 60
    }
}

class RefsNode(val discoverer: GitDiscoverer, val refType: RefType) : DefaultMutableTreeNode(refType) {

    enum class RefType {
        Tags,
        Branches
    }

    override fun isLeaf() = false

    val refs: List<String> by lazy {
        when (refType) {
            RefType.Branches -> {
                discoverer.getBranches(MdwSettings.instance.discoveryMaxBranchesTags)
            }
            RefType.Tags -> {
                discoverer.getTags(MdwSettings.instance.discoveryMaxBranchesTags)
            }
        }
    }
}