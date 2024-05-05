package org.example

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import javax.swing.*


class UI : JFrame(), ActionListener {

    private val folderPathLabel = JLabel("Selected Folder: ")
    private val fileListModel = DefaultListModel<String>()
    private val fileList = JList(fileListModel)
    private val fileScrollPane = JScrollPane(fileList)

    private val searchInputField = JTextArea(5, 20)
    private val searchResultArea = JTextArea(10, 30)
    private val searchFiles: SearchFiles = SearchFiles()
    private var selectedPath: String = ""

    init {
        createUI()
    }

    private fun createUI() {
        title = "TextFinder"
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(800, 600)

        val contentPane = JPanel(BorderLayout())
        add(contentPane)

        val buttonPanel = JPanel()
        contentPane.add(buttonPanel, BorderLayout.NORTH)

        val selectFolderButton = JButton("Select Folder")
        selectFolderButton.addActionListener(this)
        buttonPanel.add(selectFolderButton)

        contentPane.add(folderPathLabel, BorderLayout.SOUTH)

        val filesPanel = JPanel(BorderLayout())
        val filesLabel = JLabel("Files:")
        filesPanel.add(filesLabel, BorderLayout.NORTH)
        filesPanel.add(fileScrollPane, BorderLayout.CENTER)

        contentPane.add(filesPanel, BorderLayout.CENTER)

        val searchPanel = JPanel()
        searchPanel.layout = BoxLayout(searchPanel, BoxLayout.Y_AXIS)
        contentPane.add(searchPanel, BorderLayout.EAST)

        val inputLabel = JLabel("Search String: ")
        searchPanel.add(inputLabel)

        val inputScrollPane = JScrollPane(searchInputField)
        inputScrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS


        searchPanel.add(inputScrollPane)

        val searchButton = JButton("Search")
        searchButton.addActionListener(this)
        searchPanel.add(searchButton)

        val resultPanel = JPanel(BorderLayout())
        val resultLabel = JLabel("Search Result:")
        resultPanel.add(resultLabel, BorderLayout.NORTH)

        searchResultArea.isEditable = false
        val resultScrollPane = JScrollPane(searchResultArea)
        resultScrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        resultPanel.add(resultScrollPane, BorderLayout.CENTER)
        searchPanel.add(resultPanel)
    }

    private fun showMessage(message: String, title: String, messageType: Int) {
        JOptionPane.showMessageDialog(this, message, title, messageType)
    }

    override fun actionPerformed(e: ActionEvent?) {
        when (e?.actionCommand) {
            "Select Folder" -> selectFolder()
            "Search" -> performSearch()
        }
    }

    private fun selectFolder() {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        val response = fileChooser.showDialog(this, "Select")

        if (response == JFileChooser.APPROVE_OPTION) {
            val selectedFolder = fileChooser.selectedFile
            if (!validDirectory(selectedFolder)) {
                return
            }
            this.selectedPath = selectedFolder.absolutePath
            folderPathLabel.text = "Selected Folder: ${this.selectedPath}"
            setFileList(selectedFolder)
        } else if(response!=JFileChooser.CANCEL_OPTION){
            showMessage("Enter a valid path", "Invalid Directory", JOptionPane.WARNING_MESSAGE)

        }
    }

    private fun validDirectory(selectedFolder: File): Boolean {
        if (!selectedFolder.isDirectory) {
            showMessage("Enter a valid path", "Invalid Directory", JOptionPane.WARNING_MESSAGE)
            return false
        }

        return true
    }


    @OptIn(DelicateCoroutinesApi::class)
    private fun performSearch() {
        if (fileListModel.isEmpty) {
            showMessage("No files found", "Empty Directory", JOptionPane.WARNING_MESSAGE)
            return
        }
        val searchText = searchInputField.text.trim()
        if (searchText.isNotEmpty()) {
            GlobalScope.launch {
                searchString(searchText, selectedPath)
            }
        } else {
            showMessage("Enter a search term", "Empty Search", JOptionPane.WARNING_MESSAGE)
        }
    }


    private suspend fun searchString(searchText: String, path: String) {
        try {
            val searchResults = searchFiles.findAllAppearances(searchText, path)

            if (searchResults.isEmpty()) {
                searchResultArea.text = "No occurrences of '$searchText' found."
            } else {
                searchResultArea.text = buildTextFromSearchResult(searchResults, searchText)
            }

        } catch (e: Exception) {
            showMessage(e.message ?: "An error occurred", "Invalid Search", JOptionPane.WARNING_MESSAGE)
        }
    }

    private fun buildTextFromSearchResult(results: List<SearchResult>, searchText: String): String {
        val stringBuilder = StringBuilder()

        results.forEach { result ->
            stringBuilder.append("File: ${result.fileName}  -  Matches:${result.positions.size}\n\n")

            if (result.positions.isEmpty()) {
                stringBuilder.append("No occurrences of '$searchText' found\n\n")
            } else {
                result.positions.forEachIndexed { index, position ->
                    stringBuilder.append("${index + 1}  - Line: ${position.line}, Column: ${position.column}\n")
                }
                stringBuilder.append("\n")
            }
        }

        return stringBuilder.toString()
    }

    private fun setFileList(folder: File) {
        fileListModel.clear()
        try {
            val files = folder.listFiles()
            files?.forEach { file ->
                if (file.isFile && file.canRead())//&& file.extension.equals("txt", ignoreCase = true)}
                {
                    fileListModel.addElement(file.name)
                }
            }
        }
        catch (e:Exception)
        {
            showMessage(e.message ?: "An error occurred", "Invalid Search", JOptionPane.WARNING_MESSAGE)
        }
    }
}

