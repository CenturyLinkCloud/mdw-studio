package com.centurylink.mdw.studio.action

import com.centurylink.mdw.discovery.GitDiscoverer
import com.centurylink.mdw.discovery.GitHubDiscoverer
import com.centurylink.mdw.discovery.GitLabDiscoverer
import com.centurylink.mdw.studio.MdwConfig
import com.centurylink.mdw.studio.MdwSettings
import com.centurylink.mdw.studio.Secrets
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.IOException
import java.net.URL
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class DiscoverAssets : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).getProjectSetup()?.let { projectSetup ->
            val discoveryDialog = DiscoveryDialog(projectSetup)
            if (discoveryDialog.showAndGet()) {
                println("DISCOVER")
            }
        }
    }

    override fun update(event: AnActionEvent) {
        var applicable = false
        Locator(event).getProjectSetup()?.let { projectSetup ->
            val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
            applicable = file == projectSetup.project.baseDir || file == projectSetup.assetDir
        }
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }
}

class DiscoveryDialog(projectSetup: ProjectSetup) : DialogWrapper(projectSetup.project, true) {

    private val centerPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
    private val okButton: JButton?
        get() = getButton(okAction)

    private val discoverers: List<GitDiscoverer> by lazy {
        MdwSettings.instance.discoveryRepoUrls.map { url ->
            val repoUrl = URL(url)
            val discoverer = if (repoUrl.host == "github.com") {
                GitHubDiscoverer(repoUrl)
            } else {
                GitLabDiscoverer(repoUrl)
            }
            Secrets.DISCOVERY_TOKENS["${repoUrl.host}"]?.let {
                discoverer.setToken(it)
            }
            discoverer
        }
    }

    private val rootNode = DefaultMutableTreeNode("Discovery Repositories")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree: Tree

    init {
        init()
        title = "MDW Asset Discovery"
        okButton?.isEnabled = false

        discoverers.forEach { discoverer ->
            val discovererNode = DefaultMutableTreeNode(discoverer)
            discovererNode.add(RefsNode(discoverer, RefsNode.RefType.Tags))
            discovererNode.add(RefsNode(discoverer, RefsNode.RefType.Branches))
            rootNode.add(discovererNode)
        }

        val treePanel = object: JPanel(BorderLayout(5, 5)) {
            override fun getPreferredSize(): Dimension {
                val size = super.getPreferredSize()
                // account for vertical scrollbar when expanded
                return Dimension(size.width + 20, size.height)
            }
        }
        centerPanel.add(treePanel)
        treePanel.add(JLabel("Select Repository Tag or Branch:"), BorderLayout.NORTH)

        tree = Tree()
        tree.model = treeModel
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.alignmentX = Component.LEFT_ALIGNMENT
        tree.addTreeWillExpandListener(object : TreeWillExpandListener {
            override fun treeWillExpand(event: TreeExpansionEvent) {
                val path = event.path
                if (path.lastPathComponent is RefsNode) {
                    val refsNode = path.lastPathComponent as RefsNode
                    try {
                        refsNode.refs.forEach { ref ->
                            refsNode.add(DefaultMutableTreeNode(ref))
                        }
                    } catch(ex: IOException) {
                        LOG.error(ex)
                        JOptionPane.showMessageDialog(centerPanel, ex.message,
                                "Git Retrieval Error", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)
                    }
                }
            }
            override fun treeWillCollapse(event: TreeExpansionEvent) {
            }
        })

        treePanel.add(JScrollPane(tree), BorderLayout.CENTER)

        // link to prefs
        val linkText = "Change discovery repositories..."
        val linkHtml = if (UIUtil.isUnderDarcula()) {
            "<html><a href='.' style='color:white;'>$linkText</a></html>"
        }
        else {
            "<html><a href='.'>$linkText</a></html>"
        }
        val link = JLabel(linkHtml)
        link.alignmentX = Component.LEFT_ALIGNMENT
        link.cursor = Cursor(Cursor.HAND_CURSOR)
        link.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                // ShowSettingsUtil.getInstance().editConfigurable(projectSetup.project, MdwConfig())
                ShowSettingsUtil.getInstance().showSettingsDialog(projectSetup.project, MdwConfig::class.java)
            }
        })
        treePanel.add(link, BorderLayout.SOUTH)
    }

    override fun createCenterPanel(): JComponent {
        return centerPanel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return tree
    }

    companion object {
        val LOG = Logger.getInstance(DiscoverAssets::class.java)

    }
}

class RefsNode(private val discoverer: GitDiscoverer, private val refType: RefType) : DefaultMutableTreeNode(refType) {

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

