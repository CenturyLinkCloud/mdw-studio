package com.centurylink.mdw.studio.file

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.usages.Usage
import com.intellij.usages.UsageGroup
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageView
import com.intellij.usages.impl.rules.UsageGroupBase
import com.intellij.usages.rules.UsageGroupingRule
import com.intellij.usages.rules.UsageGroupingRuleProvider
import javax.swing.Icon

class AssetUsageGroupingProvider : UsageGroupingRuleProvider {
    override fun createGroupingActions(view: UsageView): Array<AnAction> {
        return arrayOf()
    }

    override fun getActiveRules(project: Project): Array<UsageGroupingRule> {
        return arrayOf(AssetGroupingRule(project))
    }
}

interface AssetUsage : Usage {
    val asset: Asset
    val targetName: String
}

class AssetGroupingRule(project: Project) : UsageGroupingRule {

    private val topGroups = mutableMapOf<String,UsageGroup>()
    private fun getTopGroup(usage: AssetUsage): UsageGroup {
        var topGroup = topGroups[usage.targetName]
        if (topGroup == null) {
            topGroup = object : UsageGroupBase(0) {
                override fun getText(view: UsageView?): String {
                    return "Usages of ${usage.targetName}"
                }
            }
            topGroups[usage.targetName] = topGroup
        }
        return topGroup
    }

    private val pkgGroups = mutableMapOf<String,UsageGroup>()
    private fun getPkgGroup(usage: AssetUsage): UsageGroup {
        var pkgGroup = pkgGroups[usage.asset.pkg.name]
        if (pkgGroup == null) {
            pkgGroup = object : UsageGroupBase(1) {
                override fun getText(view: UsageView?): String {
                    return usage.asset.pkg.name
                }
                override fun getIcon(isOpen: Boolean): Icon? {
                    return AllIcons.Nodes.Package
                }
            }
            pkgGroups[usage.asset.pkg.name] = pkgGroup
        }
        return pkgGroup
    }

    private val assetGroups = mutableMapOf<String,UsageGroup>()
    private fun getAssetGroup(usage: AssetUsage): UsageGroup {
        var assetGroup = assetGroups[usage.asset.path]
        if (assetGroup == null) {
            assetGroup = object : UsageGroupBase(1) {
                override fun getText(view: UsageView?): String {
                    return usage.asset.name
                }
                override fun getIcon(isOpen: Boolean): Icon? {
                    return FileTypeManager.getInstance().getFileTypeByExtension(usage.asset.ext).icon
                }
            }
            assetGroups[usage.asset.path] = assetGroup
        }
        return assetGroup
    }

    override fun getParentGroupsFor(usage: Usage, targets: Array<out UsageTarget>): MutableList<UsageGroup> {
        if (usage is AssetUsage) {
            val groups = mutableListOf<UsageGroup>()
            groups.add(getTopGroup(usage))
            groups.add(getPkgGroup(usage))
            groups.add(getAssetGroup(usage))
            return groups
        }
        else {
            return mutableListOf()
        }
    }
}