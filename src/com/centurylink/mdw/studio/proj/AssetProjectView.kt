package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.studio.file.AssetPackage
import com.centurylink.mdw.studio.file.Icons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.SimpleTextAttributes.STYLE_PLAIN
import javax.swing.Icon

class AssetProjectView(val project: Project) : ProjectViewPane(project) {

    override fun isInitiallyVisible(): Boolean {
        val projectSetup = project.getComponent(ProjectSetup::class.java)
        return projectSetup.isMdwProject
    }

    override fun getId(): String {
        return ID
    }

    override fun getTitle(): String {
        return "Assets"
    }

    override fun getIcon(): Icon {
        return Icons.MDW
    }

    override fun getWeight(): Int {
        return 3
    }

    override fun createStructure(): ProjectAbstractTreeStructureBase {
        return AssetViewTreeStructure(project)
    }

    companion object {
        const val ID = "mdwAssets"
    }
}

class AssetViewTreeStructure(project: Project) :
        ProjectTreeStructure(project, AssetProjectView.ID) {

    val projectSetup: ProjectSetup = project.getComponent(ProjectSetup::class.java)

    override fun getChildElements(element: Any?): Array<Any> {
            val children = mutableListOf<Any>()
            for (child in super.getChildElements(element)) {
                when (child) {
                    is PsiDirectoryNode -> {
                        val dir = child.virtualFile
                        if (dir != null) {
                            if (projectSetup.isAssetParent(dir)) {
                                children.add(child)
                            }
                            else if (projectSetup.isAssetSubdir(dir) && !AssetPackage.isMeta(dir)) {
                                val pkg = projectSetup.getPackage(dir)
                                if (pkg == null) {
                                    children.add(child)
                                }
                                else {
                                    val childNode = object : PsiDirectoryNode(child.project, child.value, child.settings, child.filter) {
                                        override fun postprocess(presentation: PresentationData) {
                                            super.postprocess(presentation)
                                            presentation.addText(pkg.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                                            presentation.addText(" v${pkg.verString}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                                        }
                                    }
                                    if (element is AbstractTreeNode<*>) {
                                        childNode.parent = element
                                    }
                                    children.add(childNode)
                                }
                            }
                        }
                    }
                    is PsiFileNode -> {
                        val file = child.virtualFile
                        file?.let {
                            if (projectSetup.isAssetSubdir(file.parent) && !AssetPackage.isMeta(file)) {
                                projectSetup.getAsset(file)?.let {
                                    val childNode = object : PsiFileNode(child.project, child.value, child.settings) {
                                        override fun postprocess(presentation: PresentationData) {
                                            presentation.applyFrom(child.presentation)
                                            val statusColor = child.getFileStatusColor(child.fileStatus)
                                            presentation.addText(presentation.presentableText, SimpleTextAttributes(STYLE_PLAIN, statusColor))
                                            if (it.version > 0) {
                                                presentation.addText(" v${it.verString}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                                            }
                                        }
                                    }
                                    if (element is AbstractTreeNode<*>) {
                                        childNode.parent = element
                                    }
                                    children.add(childNode)
                                }
                            }
                        }
                    }
                }
            }
            return children.toTypedArray()
    }

    override fun createRoot(project: Project, settings: ViewSettings): AbstractTreeNode<*> {
        return object : ProjectViewProjectNode(project, settings) {
            override fun getChildren(): MutableCollection<AbstractTreeNode<Any>> {
                val children = mutableListOf<AbstractTreeNode<Any>>()
                for (child in super.getChildren()) {
                    if (child is PsiDirectoryNode) {
                        val rootDir = (child as PsiDirectoryNode).virtualFile
                        if (rootDir != null && projectSetup.isAssetParent(rootDir)) {
                            children.add(child)
                        }
                    }
                }
                return children
            }
        }
    }

    override fun isToBuildChildrenInBackground(element: Any?): Boolean {
        return false
    }
}