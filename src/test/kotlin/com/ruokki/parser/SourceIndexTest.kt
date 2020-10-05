package com.ruokki.parser

import com.ruokki.parser.field.FieldIndexJavaClass
import junit.framework.TestCase
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import java.io.File
import java.net.URL
import java.nio.file.Paths

class SourceIndexTest : TestCase() {

    lateinit var directoryIndex: File;
    lateinit var sourceIndex: SourceIndex
    override fun setUp() {
        val indexPath = "unitTestIndexSource"
        val resource: URL = javaClass.classLoader.getResource("source")
        val file = File(resource.toURI())
        val path = Paths.get(indexPath)
        directoryIndex = File(path.toUri())


        val directory: Directory = FSDirectory.open(path)
        sourceIndex = SourceIndex(directory, MyCustomAnalyzer(), arrayOf(FieldIndexJavaClass()) )

        sourceIndex.addAllFileToIndex(file)
    }

    fun testBodyFind() {
        val result = sourceIndex.searchIndex("body", "0xFFAA00FF")
        assertThat(result).isNotNull.isNotEmpty
    }
    fun testbodyDontUseJavaTerm() {
        val result = sourceIndex.searchIndex("body", "import")
        assertThat(result).isEmpty()
    }

    fun testFieldDontUseJavaTerm() {
        val result = sourceIndex.searchIndex(FIELDNAME, "barreLife")
        assertThat(result).isNotNull.isNotEmpty
    }
    fun testMethod() {
        val result = sourceIndex.searchIndex(METHODNAME, "initUnit")
        assertThat(result).isNotNull.isNotEmpty
    }
    override fun tearDown() {
        directoryIndex.deleteRecursively()
    }
}