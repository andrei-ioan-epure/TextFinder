package org.example

import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.FileReader

class SearchFiles{

    suspend fun findAllAppearances(searchQuery: String, folderPath: String): List<SearchResult> {

        validateInput(searchQuery,folderPath)

        val files = getFilesFromFolder(folderPath)
        val searchResults = mutableListOf<SearchResult>()
        val mutex = Mutex()

        coroutineScope {
            val searchJobs = files.map { file ->
                async {
                    try {
                        val result = searchStringInText(file, searchQuery)
                        mutex.withLock {
                            searchResults.add(result)
                        }
                    } catch (e: Exception) {
                        println("Error searching in '${file.name}': ${e.message}")
                    }
                }
            }
            searchJobs.awaitAll()
        }

        return searchResults
    }


    private fun validateInput(searchQuery: String, folderPath: String)
    {
        val folder = File(folderPath)

        if (!folder.isDirectory) {
            throw IllegalArgumentException("Invalid folder path")
        }

        if (searchQuery.isBlank() ) {
            throw  IllegalArgumentException("Search term cannot be empty or blank")
        }

        if (searchQuery.length > 1024) {
            throw  IllegalArgumentException("Maximum length is 1024 characters")
        }

        val files = getFilesFromFolder(folderPath)

        if (files.isEmpty()) {
            throw IllegalArgumentException("No files found")
        }

    }

    private fun getFilesFromFolder(folderPath: String): List<File> {
        val folder = File(folderPath)
        return folder.listFiles { file -> file.isFile && file.canRead() } //&& file.extension.equals("txt", ignoreCase = true)}
            ?.toList() ?: emptyList()
    }




    private fun searchStringInText(file: File, searchString: String): SearchResult {
        val positions = mutableListOf<Position>()
        var lineNumber = 0

        //check each line separately
        val searchStrings = searchString.split("\n")

        BufferedReader(FileReader(file)).use { reader ->
            reader.forEachLine { line ->
                lineNumber++

                // iterate over each individual search string
                searchStrings.forEachIndexed { index, str ->
                    val regex = Regex(Regex.escape(str), RegexOption.IGNORE_CASE)
                    val matches = regex.findAll(line).toList()

                    // check if all parts of the search sequence are found in consecutive lines
                    if (matches.isNotEmpty()) {
                        val startIndex = matches.first().range.first

                        // add position if the entire sequence is consecutive
                        if (index == 0 || positions.lastOrNull()?.line == lineNumber - 1) {
                            positions.add(Position(line=lineNumber, column = startIndex + 1))
                        }
                    }
                }
            }
        }

        return SearchResult(fileName = file.name, positions = positions)
    }
}