package com.centurylink.mdw.draw.model

import com.centurylink.mdw.model.system.MdwVersion
import java.io.File

interface Project {
    val assetRoot: File
    val hubRootUrl: String?
    val mdwVersion: MdwVersion
}