package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.cli.Fetch
import com.centurylink.mdw.draw.edit.JsonValue
import com.centurylink.mdw.draw.edit.label
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.concurrency.SwingWorker
import com.intellij.util.ui.UIUtil
import org.json.JSONArray
import org.json.JSONObject
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.net.URL
import java.nio.file.Files
import javax.swing.*
import javax.swing.event.DocumentEvent

class SearchDialog(widget: Pagelet.Widget) : Dialog(widget) {

    fun showAndGet(): JsonValue? {
        val dialogWrapper = SearchDialogWrapper(widget, projectSetup)
        return if (dialogWrapper.showAndGet()) {
            dialogWrapper.jsonValue
        } else {
            null
        }
    }
}

class SearchDialogWrapper(private val widget: Pagelet.Widget, projectSetup: ProjectSetup) :
        DialogWrapper(widget, projectSetup) {

    private val searchText = SearchTextField()

    private val listModel = DefaultListModel<JsonValue>()
    private val resultsList = object: JBList<JsonValue>(listModel) {
        override fun getToolTipText(event: MouseEvent): String? {
            val idx = locationToIndex(event.point)
            if (idx > -1) {
                listModel.elementAt(idx)?.let { jsonValue ->
                    return jsonValue.description
                }
            }
            return super.getToolTipText(event)
        }
    }

    private val jsonArray: JSONArray by lazy {
        val searchUrl = widget.attributes["searchUrl"]
        if (searchUrl == null) {
            JSONArray()
        } else if (searchUrl.startsWith("file://")) {
            JSONArray(String(Files.readAllBytes(File(searchUrl.substring(6)).toPath())))

        } else {
            JSONArray(Fetch(URL(searchUrl)).get())
        }
    }

    override fun getPreferredFocusedComponent(): JComponent? {

        object: SwingWorker<Any?>() {
            override fun construct(): Any? {
                return try {
                    jsonArray // initialize
                    null
                } catch (ex: Exception) {
                    LOG.warn(ex)
                    ex
                }
            }
            override fun finished() {
                resultsList.setEmptyText("Nothing to show")
                resultsList.repaint()
                val res = get()
                if (res is Exception) {
                    JOptionPane.showMessageDialog(centerPanel, res.message,
                            "Search Load Error", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)
                } else {
                    searchText.isEnabled = true
                    searchText.grabFocus()
                }
            }
        }.start()

        return super.getPreferredFocusedComponent()
    }

    init {
        init()
        title = widget.label

        searchText.isEnabled = false
        okButton?.isEnabled = false

        val searchProps = widget.attributes["searchProps"]?.split(",")
        searchText.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                listModel.removeAllElements()
                resultsList.repaint()
                val search = e.document.getText(0, e.document.length)
                if (search.length > 1) {
                    jsonArray.let { ja ->
                        val searchResults = mutableListOf<JsonValue>()
                        for (i in 0 until ja.length()) {
                            val json = ja.getJSONObject(i)
                            for (name in JSONObject.getNames(json)) {
                                if (searchProps == null || searchProps.contains(name)) {
                                    val value = json.optString(name)
                                    if (value.toLowerCase().contains(search.toLowerCase())) {
                                        val paths = widget.widgets.map { it.attributes["path"] }
                                        searchResults.add(JsonValue(json, paths[0]!!, paths[1]))
                                        break
                                    }
                                }
                            }
                        }
                        searchResults.sort()
                        searchResults.forEach { listModel.addElement(it) }
                        null
                    }
                }
            }
        })

        centerPanel.add(searchText, BorderLayout.NORTH)

        val listPanel = JPanel(BorderLayout())
        listPanel.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        val borderColor = if (UIUtil.isUnderDarcula()) Color.GRAY else JBColor.border()
        resultsList.border = BorderFactory.createLineBorder(borderColor)
        resultsList.minimumSize = Dimension()
        resultsList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        resultsList.addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                val index = resultsList.locationToIndex(event.point)
                if (index > -1 && !listModel.isEmpty) {
                    jsonValue = listModel.elementAt(index)
                    okButton?.isEnabled = true
                    if (event.clickCount >= 2) {
                        okButton?.doClick()
                    }
                }
                else {
                    jsonValue = null
                    okButton?.isEnabled = false
                }
            }
        })

        listPanel.add(resultsList, BorderLayout.CENTER)
        val scrollPane = JBScrollPane(listPanel)
        scrollPane.minimumSize = Dimension(350, 450)
        centerPanel.add(scrollPane, BorderLayout.CENTER)

        resultsList.setEmptyText("Loading...")
    }

    companion object {
        val LOG = Logger.getInstance(SearchDialog::class.java)
    }
}