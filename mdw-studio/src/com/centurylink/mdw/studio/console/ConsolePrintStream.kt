package com.centurylink.mdw.studio.console

import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ConsolePrintStream(private val consoleView: ConsoleView, private val isErr: Boolean = false) :
        PrintStream(if (isErr) System.err else System.out) {

    private var outStream = ByteArrayOutputStream()

    private val contentType: ConsoleViewContentType
        get() = if (isErr) { ConsoleViewContentType.ERROR_OUTPUT } else { ConsoleViewContentType.NORMAL_OUTPUT }


    override fun write(b: Int) {
        super.write(b)
        outStream.write(b)
    }

    override fun write(buf: ByteArray, off: Int, len: Int) {
        super.write(buf, off, len)
        outStream.write(buf, off, len)
    }

    override fun flush() {
        super.flush()
        synchronized(this) {
            consoleView.print("$outStream\n", contentType)
            outStream = ByteArrayOutputStream()
        }
    }
}