package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.draw.Impl
import org.json.JSONObject
import java.util.*

class Implementors(private val projectSetup : ProjectSetup) : LinkedHashMap<String, Impl>() {

    init {
        for (pkg in projectSetup.packages) {
            for (file in pkg.dir.children) {
                if (file.exists() && !file.isDirectory && "impl" == file.extension) {
                    val implAsset = projectSetup.getAssetPath(file)
                    val impl = Impl(implAsset, JSONObject(String(file.contentsToByteArray())))
                    var iconAsset = impl.iconName
                    if (iconAsset != null && !iconAsset.startsWith("shape:")) {
                        var iconPkg = "com.centurylink.mdw.base"
                        val slash = iconAsset.lastIndexOf('/')
                        if (slash > 0) {
                            iconPkg = iconAsset.substring(0, slash)
                            iconAsset = iconAsset.substring(slash + 1)
                        } else {
                            // find in impl package, if present
                            if (implAsset != null) {
                                val implPkg = implAsset.substring(0, implAsset.indexOf('/'))
                                val pkgIconAsset = projectSetup.getAssetFile(implPkg + '/' + iconAsset)
                                if (pkgIconAsset != null) {
                                    iconPkg = implPkg
                                }
                            }
                        }
                        impl.icon = projectSetup.getIconAsset("$iconPkg/$iconAsset")
                    }
                    put(impl.implementorClassName, impl)
                }
            }
        }
        for (pseudoImpl in Impl.PSEUDO_IMPLS) {
            pseudoImpl.icon = projectSetup.getIconAsset(pseudoImpl.iconName)
            put(pseudoImpl.implementorClassName, pseudoImpl)
        }
    }

    fun toSortedList(): List<Impl> {
       return this.values.sortedBy { it.label }
    }
}

interface ImplementorChangeListener {
    fun onChange(implementors: Implementors)
}