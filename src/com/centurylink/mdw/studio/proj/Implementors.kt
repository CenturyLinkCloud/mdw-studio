package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.model.workflow.ActivityImplementor
import org.json.JSONObject
import java.util.*
import javax.swing.ImageIcon

class Implementor(val assetPath: String?, json: JSONObject) : ActivityImplementor(json) {
    var icon: ImageIcon? = null
    val category: String
      get() = baseClassName

    // generic implementor for unfound impls
    constructor(implClass: String) : this(null, JSONObject("{\"implementorClass\":\"$implClass\"}")) {
        implementorClassName = implClass
        iconName = "shape:activity"
        baseClassName = "com.centurylink.mdw.activity.types.GeneralActivity"
    }

    constructor(category: String, label: String, icon: String, implClass: String) : this(implClass) {
        baseClassName = category
        implementorClassName = implClass
        iconName = icon
    }
}

class Implementors(private val projectSetup : ProjectSetup) : LinkedHashMap<String,Implementor>() {

    init {
        for (pkgDir in projectSetup.getPackageDirs()) {
            for (file in pkgDir.children) {
                if (file.exists() && !file.isDirectory && "impl" == file.extension) {
                    val implAsset = projectSetup.getAssetPath(file)
                    val impl = Implementor(implAsset, JSONObject(String(file.contentsToByteArray())))
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
                        impl.icon = projectSetup.getIcon("$iconPkg/$iconAsset")
                    }
                    put(impl.implementorClassName, impl)
                }
            }
        }
        for (pseudoImpl in PSEUDO_IMPLS) {
            pseudoImpl.icon = projectSetup.getIcon(pseudoImpl.iconName)
            put(pseudoImpl.implementorClassName, pseudoImpl)
        }
    }

    fun toSortedList(): List<Implementor> {
       return this.values.sortedBy { it.label }
    }

    companion object {
        val DUMMY = Implementor("com.centurylink.mdw.workflow.activity.DefaultActivityImpl")
        const val BASE_PKG = "com.centurylink.mdw.base"
        val PSEUDO_IMPLS = listOf(
                Implementor("subflow", "Exception Handler Subflow", "$BASE_PKG/subflow.png", "Exception Handler"),
                Implementor("subflow", "Cancelation Handler Subflow", "$BASE_PKG/subflow.png", "Cancelation Handler"),
                Implementor("subflow", "Delay Handler Subflow", "$BASE_PKG/subflow.png", "Delay Handler"),
                Implementor("note", "Text Note", "$BASE_PKG/note.png", "TextNote")
        )
        const val START_IMPL = "com.centurylink.mdw.workflow.activity.process.ProcessStartActivity"
        const val STOP_IMPL = "com.centurylink.mdw.workflow.activity.process.ProcessFinishActivity"
    }
}