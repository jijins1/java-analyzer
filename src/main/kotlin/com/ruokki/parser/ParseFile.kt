package com.ruokki.parser

import mu.KotlinLogging
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import java.io.File
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

fun main() {
    val indexPath = "indexSource"
    val file = File("C:\\Users\\leoge\\Documents\\git\\wars")
    val path = Paths.get(indexPath)
    val directoryIndex = File(path.toUri())

    logger.info { "Clear" }
    directoryIndex.deleteRecursively();

    logger.info { "Start indexing" }
    val directory: Directory = FSDirectory.open(path)
    val sourceIndex = SourceIndex(directory, MyCustomAnalyzer())
    sourceIndex.addAllFileToIndex(file)

    val queryStringWhiteMage = "Unit"
    testFindWithField(sourceIndex, "class", queryStringWhiteMage)
    testFindWithField(sourceIndex, "body", queryStringWhiteMage)
    testFindWithoutField(sourceIndex, queryStringWhiteMage)

}

private fun testFindWithoutField(sourceIndex: SourceIndex, queryStringWhiteMage: String) {
    logger.info { "Start search without Field" }
    val listDocumentWithoufField = sourceIndex.searchIndex(queryStringWhiteMage)
    logger.info { "Result without field ${listDocumentWithoufField?.size}" }
}

private fun testFindWithField(sourceIndex: SourceIndex, inField: String, queryString: String) {
    logger.info { "Start search with Field class" }
    val listDocument = sourceIndex.searchIndex(inField, queryString)
    logger.info { "Result with field ${listDocument?.size}" }
}

