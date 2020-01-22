package com.centurylink.mdw.studio.action

import com.centurylink.mdw.discovery.GitDiscoverer
import com.centurylink.mdw.model.system.MdwVersion
import com.centurylink.mdw.studio.prefs.MdwSettings
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task

class DependenciesLocator(private val projectSetup: ProjectSetup, private val dependencies: Map<String,MdwVersion>) :
        Task.WithResult<Map<String,Pair<GitDiscoverer,String>>,Exception>(projectSetup.project, "Find MDW Dependencies", true) {

    fun doFind(): Map<String,Pair<GitDiscoverer,String>> {
        return ProgressManager.getInstance().run(this)
    }

    /**
     * Returns a map from package name to pair with discoverer and ref.
     */
    override fun compute(indicator: ProgressIndicator): Map<String,Pair<GitDiscoverer,String>> {

        val packages = mutableListOf<String>()
        packages.addAll(dependencies.keys)
        val found = mutableMapOf<String,Pair<GitDiscoverer,String>>()
        val max = MdwSettings.instance.discoveryMaxBranchesTags

        MdwSettings.instance.discoverers.forEach { discoverer ->
            indicator.text = "Loading branches/tags from ${discoverer.repoUrl}"
            val branches = discoverer.getBranches(max)
            val tags = discoverer.getTags(max)
            val finder = DiscoveryFinder(discoverer, branches, tags)

            for (pkg in packages) {
                indicator.text = "Searching $pkg v${dependencies[pkg]} in ${discoverer.repoUrl}"
                finder.findRef(pkg, dependencies[pkg])?.let { ref ->
                    found.put(pkg, Pair(discoverer, ref))
                }
            }
        }

        return found
    }

    companion object {
        val LOG = Logger.getInstance(DependenciesLocator::class.java)
    }
}

class DiscoveryFinder(private val discoverer: GitDiscoverer, private val branches: List<String>, private val tags: List<String>) {

    /**
     * Snapshot versions are only searched in branches (not tags)
     */
    fun findRef(pkg: String, pkgVer: MdwVersion?): String? {
        if (pkgVer == null) { return null }

        // try default conventions first to minimize requests
        if (pkgVer.isSnapshot) {
            // try master branch
            branches.find { it == "master" }?.let {
                if (discoverer.findPackage("master", pkg, pkgVer.toString())) {
                    return "master"
                }
            }
            // search the rest of the branches
            for (branch in branches) {
                if (branch != "master" && discoverer.findPackage(branch, pkg, pkgVer.toString())) {
                    return branch
                }
            }
        } else {
            // try tag matching version
            tags.find { it == pkgVer.toString() }?.let {
                if (discoverer.findPackage(pkgVer.toString(), pkg, pkgVer.toString())) {
                    return pkgVer.toString()
                }
            }
            // then try master
            branches.find { it == "master" }?.let {
                if (discoverer.findPackage("master", pkg, pkgVer.toString())) {
                    return "master"
                }
            }
            // search the rest of the branches
            for (branch in branches) {
                if (branch != "master" && discoverer.findPackage(branch, pkg, pkgVer.toString())) {
                    return branch
                }
            }
            // search the rest of the tags
            for (tag in tags) {
                if (tag != pkgVer.toString() && discoverer.findPackage(tag, pkg, pkgVer.toString())) {
                    return tag
                }
            }
        }
        return null
    }
}

fun GitDiscoverer.findPackage(ref: String, pkg: String, ver: String): Boolean {
    setRef(ref)
    return findPackage(pkg, ver) != null
}


