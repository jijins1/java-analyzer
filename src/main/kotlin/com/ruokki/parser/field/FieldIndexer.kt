package com.ruokki.parser.field

import org.apache.lucene.document.Document
import java.io.File

interface FieldIndexer {
    fun addIndex(file: File, document: Document);
}