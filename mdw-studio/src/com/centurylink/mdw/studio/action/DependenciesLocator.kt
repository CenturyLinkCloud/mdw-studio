package com.centurylink.mdw.studio.action

import com.centurylink.mdw.discovery.GitDiscoverer
import com.centurylink.mdw.model.PackageDependency
import com.centurylink.mdw.model.project.Data
import com.centurylink.mdw.studio.prefs.MdwSettings
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task

class DependenciesLocator(projectSetup: ProjectSetup, private val dependencies: List<PackageDependency>) :
        Task.WithResult<List<DiscovererPackages>,Exception>(projectSetup.project, "Find MDW Dependencies", true) {

    fun doFind(): List<DiscovererPackages> {
        return ProgressManager.getInstance().run(this)
    }

    /**
     * Returns a list of DiscovererPackages relating discoverer to ref/dependencies.
     */
    override fun compute(indicator: ProgressIndicator): List<DiscovererPackages> {

        val result = mutableListOf<DiscovererPackages>()
        val max = MdwSettings.instance.discoveryMaxBranchesTags
        val found = mutableListOf<PackageDependency>()

        for (discoverer in MdwSettings.instance.discoverers) {
            indicator.text = discoverer.repoUrl.toString()
            val branches = discoverer.getBranches(max)
            val tags = discoverer.getTags(max)
            val finder = DiscoveryFinder(discoverer, branches, tags)
            val refDependencies = mutableMapOf<String,MutableList<PackageDependency>>()
            val foundHere = mutableListOf<PackageDependency>()

            for (dependency in dependencies) {
                if (!found.contains(dependency)) {
                    indicator.text2 = dependency.toString()
                    finder.findRef(dependency)?.let { ref ->
                        found.add(dependency)
                        foundHere.add(dependency)
                        var refPkgDeps: MutableList<PackageDependency>? = refDependencies[ref]
                        if (refPkgDeps == null) {
                            refPkgDeps = mutableListOf()
                            refDependencies[ref] = refPkgDeps
                        }
                        refPkgDeps.add(dependency)
                    }
                }
            }

            if (foundHere.isNotEmpty()) {
                result.add(DiscovererPackages(discoverer, refDependencies))
            }

            // keep searching?
            if (found.containsAll(dependencies))
                break;
        }

        // make sure mdw discoverer comes first so that framework pkgs will be imported by it
        result.sortWith(Comparator { d1, d2 ->
            when (Data.GIT_URL) {
                d1.discoverer.repoUrl.toString() -> {
                    -1
                }
                d2.discoverer.repoUrl.toString() -> {
                    1
                }
                else -> {
                    0
                }
            }
        })
        return result
    }

    companion object {
        val LOG = Logger.getInstance(DependenciesLocator::class.java)
    }
}

class DiscoveryFinder(private val discoverer: GitDiscoverer, private val branches: List<String>, private val tags: List<String>) {

    /**
     * Snapshot versions are only searched in branches (not tags)
     */
    fun findRef(dependency: PackageDependency): String? {
        val pkg = dependency.packageName
        val ver = dependency.version.toString()

        // try default conventions first to minimize requests
        if (dependency.version.isSnapshot) {
            // try master branch
            branches.find { it == "master" }?.let {
                if (discoverer.findPackage("master", pkg, ver)) {
                    return it
                }
            }
            // search the rest of the branches
            for (branch in branches) {
                if (branch != "master" && discoverer.findPackage(branch, pkg, ver)) {
                    return branch
                }
            }
            // search tags
            for (tag in tags) {
                if (discoverer.findPackage(tag, pkg, ver)) {
                    return tag
                }
            }
        } else {
            // try tag matching version
            tags.find { it == ver }?.let {
                if (discoverer.findPackage(ver, pkg, ver)) {
                    return it
                }
            }
            // then try master
            branches.find { it == "master" }?.let {
                if (discoverer.findPackage("master", pkg, ver)) {
                    return it
                }
            }
            // search the rest of the branches
            for (branch in branches) {
                if (branch != "master" && discoverer.findPackage(branch, pkg, ver)) {
                    return branch
                }
            }
            // search the rest of the tags
            for (tag in tags) {
                if (tag != ver && discoverer.findPackage(tag, pkg, ver)) {
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

data class DiscovererPackages(val discoverer: GitDiscoverer, val refDependencies: Map<String,List<PackageDependency>>)
