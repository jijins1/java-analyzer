package com.ruokki.parser

import com.ruokki.parser.field.FieldIndexer
import mu.KotlinLogging
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.SortedDocValuesField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.*
import org.apache.lucene.queryparser.classic.ParseException
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.store.Directory
import org.apache.lucene.util.BytesRef
import java.io.File
import java.io.IOException
import java.util.*

const val CLASS = "class"
const val METHODNAME = "methodName"
const val FIELDNAME = "fieldName"
const val FIELDTYPE = "fieldType"

private val logger = KotlinLogging.logger {}

class SourceIndex(val directoryIndex: Directory, val analyzer: Analyzer, val fieldIndexers: Array<FieldIndexer>) {


    fun searchIndex(inField: String?, queryString: String?): List<Document>? {
        val query = QueryParser(inField, analyzer).parse(queryString)
        return searchIndex(query);
    }

    fun searchIndex(queryString: String?): List<Document>? {
        val query = TermQuery(Term("body", queryString))
        return searchIndex(query);
    }

    fun searchIndex(query: Query): List<Document>? {
        try {
            logger.info { query.toString() }
            val indexReader: IndexReader = DirectoryReader.open(directoryIndex)
            val searcher = IndexSearcher(indexReader)
            val topDocs = searcher.search(query, 1000)
            val documents: MutableList<Document> =
                ArrayList()
            for (scoreDoc in topDocs.scoreDocs) {
                documents.add(searcher.doc(scoreDoc.doc))
            }
            return documents
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return null
    }


    fun addFileToIndex(file: File) {
        val indexWriterConfig = IndexWriterConfig(analyzer)
        logger.debug { "Add File : $file " }
        try {
            val writter = IndexWriter(directoryIndex, indexWriterConfig)
            val document = Document()
            val title = file.name
            val classFile = "classFile"

            fieldIndexers.forEach { fieldIndexer: FieldIndexer -> fieldIndexer.addIndex(file, document) }
            document.add(TextField("body", file.readText(), Field.Store.YES))
            document.add(TextField(classFile, title, Field.Store.YES))
            document.add(SortedDocValuesField(classFile, BytesRef(title)))

            writter.addDocument(document)
            writter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    fun addAllFileToIndex(file: File) {
        if (file.isDirectory) {
            file.walkTopDown()
                .filter { subFile -> subFile.isFile }
                .filter { subFile -> subFile.extension.equals("java") }
                .forEach { subFile -> this.addFileToIndex(subFile) }
        } else {
            this.addFileToIndex(file)
        }
    }
}