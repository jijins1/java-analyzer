package com.ruokki.parser

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
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
const val FIELDNAME = "fieldName"
const val FIELDTYPE = "fieldType"

private val logger = KotlinLogging.logger {}

class SourceIndex(val directoryIndex: Directory, val analyzer: Analyzer) {


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

            this.indexJavaClassToDocument(file, document)
            document.add(TextField("body", file.readText(), Field.Store.YES))
            document.add(TextField(classFile, title, Field.Store.YES))
            document.add(SortedDocValuesField(classFile, BytesRef(title)))

            writter.addDocument(document)
            writter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun indexJavaClassToDocument(file: File, document: Document) {
        logger.debug { "Indexing class file ${file.name}" }
        val compilationUnit = StaticJavaParser.parse(file)
        compilationUnit.findAll(ClassOrInterfaceDeclaration::class.java)
            .forEach { classOrInterfaceDeclaration ->
                this.indexJavaClassToDocument(
                    classOrInterfaceDeclaration,
                    document
                )
            }
    }

    private fun indexJavaClassToDocument(clazz: ClassOrInterfaceDeclaration, document: Document) {
        logger.debug { "Indexing class ${clazz.nameAsString}" }
        document.add(TextField(CLASS, clazz.nameAsString, Field.Store.YES))
        clazz.findAll(FieldDeclaration::class.java)
            .forEach { fieldDeclaration: FieldDeclaration -> this.indexJavaFieldToDocument(fieldDeclaration, document) }
    }

    private fun indexJavaFieldToDocument(fieldDeclaration: FieldDeclaration, document: Document) {
        val variable = fieldDeclaration.findFirst(VariableDeclarator::class.java).get().name.asString()
        logger.debug { "Indexing Field ${variable}" }
        document.add(TextField(FIELDNAME, variable, Field.Store.YES))
        document.add(TextField(FIELDTYPE, fieldDeclaration.commonType.asString(), Field.Store.YES))
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